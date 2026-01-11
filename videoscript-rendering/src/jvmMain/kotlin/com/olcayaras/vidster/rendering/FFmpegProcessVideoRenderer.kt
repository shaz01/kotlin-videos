package com.olcayaras.vidster.rendering

import com.olcayaras.lib.definitions.AudioDefinition
import com.olcayaras.lib.ofFrames
import org.jetbrains.skia.Bitmap
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.time.Duration

class FFmpegProcessVideoRenderer(val frameRenderer: FrameRenderer): VideoRenderer {
    private val tempAudioFiles = mutableListOf<Path>()
    
    override fun exportVideo(
        outputPath: Path,
        totalDuration: Duration,
        fps: Int,
        withAlpha: Boolean,
        audio: List<AudioDefinition>,
        onFrameRendered: (frameIndex: Int, totalFrames: Int) -> Unit
    ) {
        val totalFrames = totalDuration.ofFrames(fps)
        val frameIntervalNanos = 1_000_000_000L / fps

        // Create temporary audio files from base64 data
        val audioFiles = createTempAudioFiles(audio)
        
        val ffmpegProcess = startFFmpegProcessRaw(
            outputPath,
            frameRenderer.width,
            frameRenderer.height,
            fps,
            audioFiles,
            withAlpha
        )

        val bitmap = Bitmap().also {
            it.allocN32Pixels(frameRenderer.width, frameRenderer.height)
        }

        val rgbArray = if (!withAlpha) ByteArray(bitmap.width * bitmap.height * 3) else null
        try {
            // Render all frames to temporary files
            ffmpegProcess.outputStream.use { ffmpegInput ->
                for (frameIndex in 0 until totalFrames) {
                    val timeNanos = frameIndex * frameIntervalNanos
                    val frameImage = frameRenderer.renderFrame(timeNanos)

                    // sets rgb array to the frameImage's content
                    frameImage.readPixels(bitmap)
                    val src = bitmap.readPixels()!! // BGRA from Skia
                    if (withAlpha) {
                        // We feed BGRA directly to FFmpeg when alpha is requested
                        ffmpegInput.write(src)
                    } else {
                        // Convert BGRA -> RGB and feed to FFmpeg
                        convertBGRAtoRGB(src, rgbArray!!)
                        ffmpegInput.write(rgbArray)
                    }

                    onFrameRendered(frameIndex, totalFrames)
                }
            }
            val exitCode = ffmpegProcess.waitFor()
            if (exitCode != 0) {
                throw RuntimeException("FFmpeg failed with exit code: $exitCode")
            }
        } catch (e: Exception) {
            ffmpegProcess.destroyForcibly()
            throw e
        } finally {
            // Clean up temporary audio files
            cleanupTempAudioFiles()
        }
    }

    private fun startFFmpegProcessRaw(
        outputPath: Path,
        width: Int,
        height: Int,
        fps: Int,
        audioFiles: List<AudioFileInfo>,
        withAlpha: Boolean
    ): Process {
        val command = buildFFmpegCommand(outputPath, width, height, fps, audioFiles, withAlpha)

        println("Executing command: ${command.joinToString(" ")}")
        val processBuilder = ProcessBuilder(command)
        
        // Capture stderr to help debug issues
        processBuilder.redirectError(ProcessBuilder.Redirect.PIPE)

        val process = processBuilder.start()
        
        // Start a thread to read and print stderr
        Thread {
            process.errorStream.bufferedReader().use { reader ->
                reader.lineSequence().forEach { line ->
                    println("FFmpeg stderr: $line")
                }
            }
        }.start()

        return process
    }

    private fun convertBGRAtoRGB(bgra: ByteArray, rgb: ByteArray) {
        for (i in bgra.indices step 4) {
            val rgbIndex = (i / 4) * 3
            rgb[rgbIndex] = bgra[i + 2]     // B -> R
            rgb[rgbIndex + 1] = bgra[i + 1] // G -> G
            rgb[rgbIndex + 2] = bgra[i]     // R -> B
            // Skip rgba[i + 3] which is the alpha channel
        }
    }
    
    private fun createTempAudioFiles(audio: List<AudioDefinition>): List<AudioFileInfo> {
        val audioFiles = mutableListOf<AudioFileInfo>()
        
        audio.forEach { audioDefinition ->
            when (audioDefinition) {
                is AudioDefinition.TTS -> {
                    // Create temporary file for MP3 data
                    val tempFile = Files.createTempFile("audio_", ".mp3")
                    tempAudioFiles.add(tempFile)
                    
                    // Decode base64 audio data and write to temp file
                    val audioBytes = Base64.getDecoder().decode(audioDefinition.audioBase64)
                    Files.write(tempFile, audioBytes)
                    
                    audioFiles.add(
                        AudioFileInfo(
                            file = tempFile,
                            startTime = audioDefinition.from,
                            endTime = audioDefinition.to
                        )
                    )
                }
                is AudioDefinition.Resource -> {
//                     Use the resource file directly - no need to create temp file
                    val resourcePath = Path.of(audioDefinition.file)
                    if (!Files.exists(resourcePath)) {
                        println("Warning: Audio resource file not found: ${audioDefinition.file}")
                        return@forEach
                    }

                    audioFiles.add(
                        AudioFileInfo(
                            file = resourcePath,
                            startTime = audioDefinition.from,
                            endTime = audioDefinition.to
                        )
                    )
                }
            }
        }
        
        return audioFiles
    }
    
    private fun cleanupTempAudioFiles() {
        tempAudioFiles.forEach { file ->
            try {
                Files.deleteIfExists(file)
            } catch (e: Exception) {
                // Log error but don't fail the export
                println("Warning: Could not delete temporary audio file: $file")
            }
        }
        tempAudioFiles.clear()
    }
    private fun buildFFmpegCommand(
        outputPath: Path,
        width: Int,
        height: Int,
        fps: Int,
        audioFiles: List<AudioFileInfo>,
        withAlpha: Boolean
    ): List<String> {
        val command = mutableListOf<String>()

        // Basic FFmpeg setup
        command.addAll(listOf(
            "ffmpeg",                    // FFmpeg executable
            "-y",                        // Overwrite output file if it exists
            "-f", "rawvideo",           // Input format: raw uncompressed video frames
            "-vcodec", "rawvideo",      // Video codec for input: raw (uncompressed)
            // Pixel format for input piping. Use BGRA when alpha is requested, RGB otherwise
            "-pix_fmt", if (withAlpha) "bgra" else "rgb24",
            "-s", "${width}x${height}", // Video dimensions (e.g. "1920x1080")
            "-framerate", fps.toString(), // Input framerate (e.g. "30")
            "-i", "-",                  // Input source: stdin (pipe from your app)
        ))

        // Add audio input files
        audioFiles.forEach { audioFile ->
            command.addAll(listOf("-i", audioFile.file.toAbsolutePath().toString()))
        }

        // Add filter complex for audio processing if we have audio files
        if (audioFiles.isNotEmpty()) {
            val filterComplex = buildAudioFilterComplex(audioFiles, fps)
            command.addAll(listOf("-filter_complex", filterComplex))
            command.addAll(listOf("-map", "0:v", "-map", "[audio]")) // Map video from input 0, audio from filter
        } else {
            command.addAll(listOf("-map", "0:v")) // Map only video if no audio
        }

        // Output settings
        if (withAlpha) {
            val ext = outputPath.fileName.toString().substringAfterLast('.', "").lowercase()
            when (ext) {
                "webm" -> {
                    // VP9 with alpha in WebM
                    command.addAll(listOf(
                        "-c:v", "libvpx-vp9",
                        "-pix_fmt", "yuva420p",
                        "-crf", "30",
                        "-b:v", "0",
                        "-deadline", "good",
                        "-row-mt", "1",
                    ))
                    if (audioFiles.isNotEmpty()) {
                        command.addAll(listOf("-c:a", "libopus"))
                    }
                }
                "mov", "qt" -> {
                    // ProRes 4444 with alpha in MOV container
                    command.addAll(listOf(
                        "-c:v", "prores_ks",
                        "-profile:v", "4444",
                        "-pix_fmt", "yuva444p10le",
                    ))
                    if (audioFiles.isNotEmpty()) {
                        // Uncompressed PCM for compatibility; adjust if needed
                        command.addAll(listOf("-c:a", "pcm_s16le"))
                    }
                }
                else -> {
                    throw IllegalArgumentException(
                        "Transparent export requires .webm (VP9 alpha) or .mov (ProRes 4444). Got: .$ext"
                    )
                }
            }
        } else {
            command.addAll(listOf(
                "-c:v", "libx264",          // Output video codec: H.264 compression
                "-pix_fmt", "yuv420p",      // Output pixel format: YUV 4:2:0 (standard for MP4)
                "-crf", "18",               // Constant Rate Factor: quality (0=lossless, 51=worst, 18=high quality)
                "-preset", "fast",          // Encoding speed preset (ultrafast/fast/medium/slow/veryslow)
            ))
            // Add audio codec if we have audio
            if (audioFiles.isNotEmpty()) {
                command.addAll(listOf("-c:a", "aac")) // Output audio codec: AAC
            }
        }

        // Output file
        command.add(outputPath.toAbsolutePath().toString())

        return command
    }

    private fun buildAudioFilterComplex(audioFiles: List<AudioFileInfo>, fps: Int): String {
        val filters = mutableListOf<String>()

        audioFiles.forEachIndexed { index, audioFile ->
            val inputIndex = index + 1 // Input 0 is video, audio inputs start at 1
            val startSeconds = audioFile.startTime.inWholeMilliseconds / 1000.0
            val duration = (audioFile.endTime - audioFile.startTime).inWholeMilliseconds / 1000.0

            // Create a filter to trim and position this audio segment
            val filterLabel = if (audioFiles.size == 1) "audio" else "a${index}"
            
            // For resource files, we take the full audio and position it at the correct time
            // atrim duration ensures we don't exceed the intended segment length
            filters.add("[$inputIndex:a]atrim=duration=$duration,adelay=${startSeconds.toInt() * 1000}|${startSeconds.toInt() * 1000}[$filterLabel]")
        }

        // If we have multiple audio files, mix them together
        return if (audioFiles.size > 1) {
            val audioInputs = audioFiles.indices.map { "[a$it]" }
            "${filters.joinToString(";")};${audioInputs.joinToString("")}amix=inputs=${audioInputs.size}[audio]"
        } else {
            // Single audio file - just return the filter
            filters.joinToString(";")
        }
    }
    private data class AudioFileInfo(
        val file: Path,
        val startTime: Duration,
        val endTime: Duration
    )
}
