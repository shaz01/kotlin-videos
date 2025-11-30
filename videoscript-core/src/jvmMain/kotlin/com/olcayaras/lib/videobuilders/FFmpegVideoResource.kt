package com.olcayaras.lib.videobuilders

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.util.fastRoundToInt
import com.olcayaras.lib.LocalCurrentFrame
import com.olcayaras.lib.asDuration
import com.olcayaras.lib.frames
import com.olcayaras.lib.ofFrames
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Frame
import org.bytedeco.javacv.FrameConverter
import org.bytedeco.javacv.FrameGrabber
import org.bytedeco.javacv.Java2DFrameConverter
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ImageInfo
import java.nio.ByteBuffer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.seconds

// there are more to optimize!
// see: https://openrouter.ai/chat?room=orc-1755538484-ZFh7EeFt4njrkwh9r7MM
// and profile to see results
class FFmpegVideoResource private constructor(
    grabber: FFmpegFrameGrabber,
    startTime: Duration,
    endTime: Duration,
    private val fps: Int,
    private val videoFps: Double
) : MediaResource.Video {
    private val grabber = OptimizedFrameGrabber(grabber)
    private val startFrame = startTime.ofFrames(fps)
    private val endFrame = endTime.ofFrames(fps) - 1
    private val duration = (endFrame - startFrame + 1).asDuration(fps)
    private val frameCache = LinkedHashMap<Int, ImageBitmap>(32, 0.75f, true)
    private val maxCacheSize = 60
    override fun getLength(): Duration = duration

    @Suppress("NOTHING_TO_INLINE")
    private inline fun mapFrameToVideoFrame(frame: Int): Int {
        return (frame * (videoFps / fps)).fastRoundToInt()
    }

    @Composable
    override fun currentVideoFrame(): ImageBitmap {
        val currentTime = LocalCurrentFrame.current

        // Calculate video timestamp relative to start time
        val videoTimestampFrame = startFrame + currentTime
        val clampedTimestamp = videoTimestampFrame.coerceIn(startFrame, endFrame)
        val mappedTimestamp = mapFrameToVideoFrame(clampedTimestamp)

        return remember(mappedTimestamp) {
            getFrameAt(mappedTimestamp)
        }
    }

    private fun getFrameAt(timestamp: Int): ImageBitmap {
        // Check cache first
        frameCache[timestamp]?.let { return it }

        // Seek to timestamp and grab frame
        val imageBitmap = grabber.grabFrame(timestamp)

        // Cache the frame (with LRU-style cleanup)
        if (frameCache.size >= maxCacheSize) {
            val oldestKey = frameCache.keys.minOrNull()
            oldestKey?.let { frameCache.remove(it) }
        }
        frameCache[timestamp] = imageBitmap

        return imageBitmap
    }

    fun cleanup() {
        try {
            frameCache.clear()
            grabber.clear()
        } catch (e: Exception) {
            // Log error but don't throw
        }
    }

    companion object {
        fun create(
            path: String,
            fps: Int,
            start: Duration = 0.seconds,
            end: MediaEnd
        ): FFmpegVideoResource {
            // Initialize FFmpeg logging (optional)
            avutil.av_log_set_level(avutil.AV_LOG_ERROR)

            val grabber = FFmpegFrameGrabber(path)
            grabber.pixelFormat = avutil.AV_PIX_FMT_BGRA
            grabber.setOption("fflags", "fastseek")

            // Get video duration
            grabber.start()
            val durationMicros = grabber.lengthInTime
            val fpsOfVideo = grabber.frameRate

            val videoDuration = durationMicros.microseconds

            val endTime = when (end) {
                is MediaEnd.UntilEnd -> videoDuration
                is MediaEnd.FixedDuration -> start + end.duration
                is MediaEnd.FixedPoint -> end.end
            }.coerceAtMost(videoDuration)

            return FFmpegVideoResource(
                fps = fps,
                videoFps = fpsOfVideo,
                grabber = grabber,
                startTime = start,
                endTime = endTime,
            )
        }
    }
}

class OptimizedFrameGrabber(
    private val grabber: FFmpegFrameGrabber,
) {
    private val converter = ComposeFrameConverter()
    private var currentGrabberPosition: Int = -1

    fun grabFrame(frameNumber: Int): ImageBitmap {
        // Small forward jump - decode forward
        if (frameNumber > currentGrabberPosition) {
            // Decode forward without seeking
            repeat(frameNumber - currentGrabberPosition - 1) {
                grabber.grabFrame(false, true, false, false)
            }
        } else {
            // Backward jump - seek
            grabber.frameNumber = frameNumber
        }

        val frame = grabber.grabImage()
            ?: throw IllegalStateException("Failed to grab frame $frameNumber")

        currentGrabberPosition = frameNumber

        return converter.convert(frame)
    }

    fun clear() {
        grabber.stop()
        grabber.release()
    }
}

/**
 * Direct Frame to Compose ImageBitmap converter
 * Avoids BufferedImage intermediate step for better performance
 */
class ComposeFrameConverter : FrameConverter<ImageBitmap>() {
    private var lastWidth = 0
    private var lastHeight = 0
    private var lastChannels = 0
    private var reusableByteArray: ByteArray? = null

    override fun convert(imageBitmap: ImageBitmap): Frame {
        throw UnsupportedOperationException("ImageBitmap to Frame conversion not implemented")
    }

    override fun convert(frame: Frame): ImageBitmap {
        if (frame.image == null || frame.image[0] == null) {
            throw IllegalArgumentException("Frame has no image data")
        }

        val width = frame.imageWidth
        val height = frame.imageHeight
        val channels = frame.imageChannels
        val stride = frame.imageStride

        // create bytearray or use cache if w & h are same
        if (reusableByteArray == null || lastWidth != width || lastHeight != height || lastChannels != channels) {
            lastWidth = width
            lastHeight = height
            lastChannels = channels
            reusableByteArray = ByteArray(width * height * 4)
        }
        val dstArray = reusableByteArray!!

        // Get source buffer from frame
        val sourceBuffer = frame.image[0] as ByteBuffer
        sourceBuffer.rewind()

        // Convert pixels based on format
        when (channels) {
            3 -> convertBGRtoBGRA(sourceBuffer, dstArray, width, height, stride)
            4 -> sourceBuffer.get(dstArray)
            1 -> convertGrayToBGRA(sourceBuffer, dstArray, width, height, stride)
            else -> throw UnsupportedOperationException("Unsupported channel count: $channels")
        }

        // we create new bitmap every time because they're not reusable.
        val bitmap = Bitmap().apply { allocPixels(ImageInfo.makeS32(width, height, ColorAlphaType.UNPREMUL)) }
        bitmap.installPixels(dstArray)
        return bitmap.asComposeImageBitmap()
    }

    private fun convertBGRtoBGRA(
        src: ByteBuffer,
        dest: ByteArray,
        width: Int,
        height: Int,
        stride: Int
    ) {
        for (y in 0 until height) {
            val srcRowStart = y * stride
            val destRowStart = y * width * 4

            for (x in 0 until width) {
                val srcIdx = srcRowStart + (x * 3)
                val destIdx = destRowStart + (x * 4)

                val b = src.get(srcIdx + 0)
                val g = src.get(srcIdx + 1)
                val r = src.get(srcIdx + 2)

                // Write as BGRA
                dest[destIdx + 0] = b    // B
                dest[destIdx + 1] = g    // G
                dest[destIdx + 2] = r    // R
                dest[destIdx + 3] = 0xFF.toByte() // A
            }
        }
    }

    private fun convertGrayToBGRA(
        src: ByteBuffer,
        dest: ByteArray,
        width: Int,
        height: Int,
        stride: Int
    ) {
        for (y in 0 until height) {
            val srcRowStart = y * stride
            val destRowStart = y * width * 4

            for (x in 0 until width) {
                val gray = src.get(srcRowStart + x)
                val destIdx = destRowStart + (x * 4)

                dest[destIdx + 0] = gray       // B
                dest[destIdx + 1] = gray       // G
                dest[destIdx + 2] = gray       // R
                dest[destIdx + 3] = 0xFF.toByte()  // A
            }
        }
    }

    override fun close() {
        super.close()
        reusableByteArray = null
    }
}