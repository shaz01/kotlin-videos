@file:OptIn(ExperimentalEncodingApi::class)

package com.olcayaras.lib

import com.olcayaras.audio.AudioContext
import com.olcayaras.lib.definitions.AudioDefinition
import org.lwjgl.openal.AL10.*
import org.lwjgl.openal.AL11.AL_SAMPLE_OFFSET
import org.lwjgl.stb.STBVorbis.stb_vorbis_decode_filename
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.bytedeco.javacv.FFmpegFrameGrabber
import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

actual fun AudioController(definitions: List<AudioDefinition>): AudioController {
    return LWJGLOpenALAudioController(definitions)
}

internal class AudioSource(
    private val buffer: Int,
    private val source: Int,
    val duration: Duration,
    val sampleRate: Int
) {
    fun play() {
        alSourcePlay(source)
    }
    
    fun pause() {
        alSourcePause(source)
    }
    
    fun stop() {
        alSourceStop(source)
    }
    
    fun isPlaying(): Boolean {
        return alGetSourcei(source, AL_SOURCE_STATE) == AL_PLAYING
    }
    
    fun setSampleOffset(offset: Int) {
        alSourcei(source, AL_SAMPLE_OFFSET, offset)
    }
    
    fun cleanup() {
        alDeleteSources(source)
        alDeleteBuffers(buffer)
    }
}

private class LWJGLOpenALAudioController(override val definitions: List<AudioDefinition>) : AudioController {
    private val audioContext = AudioContext()
    private var currentDuration: Duration = Duration.ZERO
    private var isPlaying = false

    private val audioSources: Map<AudioDefinition, AudioSource>

    init {
        // Load all audio definitions
        audioSources = definitions
            .mapNotNull { definition -> loadAudio(definition)?.let { definition to it } }
            .toMap()
    }

    override fun updateTime(duration: Duration) {
        val slop = 100.milliseconds
        val difference = duration - currentDuration
        val adjustPosition = difference.absoluteValue > slop

        if (adjustPosition) println("Adjusting position by $difference")

        audioSources.forEach { (definition, audioSource) ->
            val willPlay = duration in definition.from..definition.to
            val isCurrentlyPlaying = audioSource.isPlaying()

            if (!willPlay) {
                if (isCurrentlyPlaying) audioSource.stop()
            } else {
                if (adjustPosition) {
                    // Calculate position within the audio clip
                    val offsetInClip = duration - definition.from
                    val sampleOffset =
                        (offsetInClip.inWholeNanoseconds * audioSource.sampleRate / 1_000_000_000L).toInt()

                    // Stop current playback and seek
                    audioSource.stop()
                    audioSource.setSampleOffset(sampleOffset)
                }

                if (!isCurrentlyPlaying && isPlaying) {
                    audioSource.play()
                }
            }
        }

        currentDuration = duration
    }

    override fun seekToDuration(duration: Duration) {
        audioSources.forEach { (definition, audioSource) ->
            if (duration in definition.from..definition.to) {
                val offsetInClip = duration - definition.from
                val sampleOffset = (offsetInClip.inWholeNanoseconds * audioSource.sampleRate / 1_000_000_000L).toInt()

                val wasPlaying = audioSource.isPlaying()
                audioSource.stop()
                audioSource.setSampleOffset(sampleOffset)

                if (wasPlaying && isPlaying) {
                    audioSource.play()
                }
            }
        }
        currentDuration = duration
    }

    override fun pause() {
        isPlaying = false
        audioSources.values.forEach { audioSource ->
            if (audioSource.isPlaying()) {
                audioSource.pause()
            }
        }
    }

    override fun play() {
        isPlaying = true
        audioSources.forEach { (definition, audioSource) ->
            if (currentDuration in definition.from..definition.to) {
                val offsetInClip = currentDuration - definition.from
                val sampleOffset = (offsetInClip.inWholeNanoseconds * audioSource.sampleRate / 1_000_000_000L).toInt()

                audioSource.setSampleOffset(sampleOffset)
                audioSource.play()
            }
        }
    }

    fun cleanup() {
        audioSources.values.forEach { audioSource ->
            audioSource.cleanup()
        }
        
        audioContext.cleanup()
    }

    private fun loadAudio(definition: AudioDefinition): AudioSource? {
        return try {
            val result = when (definition) {
                is AudioDefinition.Resource -> {
                    println("Loading audio from file: ${definition.file}")
                    LWJGLOpenAlAudioLoader.loadFromFile(definition.file)
                }

                is AudioDefinition.TTS -> {
                    println("Loading audio from TTS base64 (${definition.audioBase64.length} chars)")
                    val data = Base64.decode(definition.audioBase64)
                    LWJGLOpenAlAudioLoader.loadMP3FromData(data)
                }
            }

            if (result == null) {
                println("FAILED: loadAudio returned null")
            } else {
                println("SUCCESS: Loaded audio for with duration ${result.duration}")
            }

            result
        } catch (e: Exception) {
            println("EXCEPTION: Failed to load audio for: ${e.message}")
            e.printStackTrace()
            null
        }
    }
}

private object LWJGLOpenAlAudioLoader {
    fun loadFromFile(filePath: String): AudioSource? {
        val file = File(filePath)
        if (!file.exists()) {
            println("Audio file not found: $filePath")
            return null
        }

        // Try loading as OGG first (LWJGL has built-in support)
        if (filePath.endsWith(".ogg", ignoreCase = true)) {
            return loadOggFile(filePath)
        }

        // Try loading video files using JavaCV
        if (filePath.endsWith(".mp4", ignoreCase = true) || 
            filePath.endsWith(".mov", ignoreCase = true) ||
            filePath.endsWith(".avi", ignoreCase = true) ||
            filePath.endsWith(".webm", ignoreCase = true)) {
            return loadVideoFile(filePath)
        }

        // Fallback to WAV using Java Sound API
        return loadWavFile(filePath)
    }

    fun loadVideoFile(filePath: String): AudioSource? {
        return try {
            val grabber = FFmpegFrameGrabber(filePath)
            grabber.start()
            
            // Check if file has audio
            if (grabber.audioChannels <= 0) {
                println("No audio tracks found in video file: $filePath")
                grabber.stop()
                return null
            }
            
            val sampleRate = grabber.sampleRate
            val channels = grabber.audioChannels
            val duration = (grabber.lengthInTime / 1_000_000.0).seconds  // Convert microseconds to seconds
            
            val audioData = mutableListOf<Short>()
            
            // Extract audio samples
            var frame = grabber.grab()
            while (frame != null) {
                if (frame.samples != null && frame.samples.isNotEmpty()) {
                    // JavaCV Frame samples are stored as Buffer arrays
                    val sampleBuffer = frame.samples[0]
                    if (sampleBuffer is java.nio.ShortBuffer) {
                        val samplesArray = ShortArray(sampleBuffer.remaining())
                        sampleBuffer.get(samplesArray)
                        audioData.addAll(samplesArray.toList())
                        sampleBuffer.rewind()  // Reset buffer position for next potential use
                    }
                }
                frame = grabber.grab()
            }
            
            grabber.stop()
            
            // Convert to OpenAL format
            val audioBuffer = MemoryUtil.memAlloc(audioData.size * 2)  // 2 bytes per short
            audioData.forEach { sample -> audioBuffer.putShort(sample) }
            audioBuffer.flip()
            
            val alFormat = when (channels) {
                1 -> AL_FORMAT_MONO16
                2 -> AL_FORMAT_STEREO16
                else -> {
                    MemoryUtil.memFree(audioBuffer)
                    println("Unsupported audio channel count: $channels")
                    return null
                }
            }
            
            val buffer = alGenBuffers()
            alBufferData(buffer, alFormat, audioBuffer, sampleRate)
            MemoryUtil.memFree(audioBuffer)
            
            val source = alGenSources()
            alSourcei(source, AL_BUFFER, buffer)
            
            AudioSource(buffer, source, duration, sampleRate)
        } catch (e: Exception) {
            println("Error loading video file audio: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    fun loadOggFile(filePath: String): AudioSource? {
        MemoryStack.stackPush().use { stack ->
            val channelsBuffer = stack.mallocInt(1)
            val sampleRateBuffer = stack.mallocInt(1)

            val audioBuffer = stb_vorbis_decode_filename(filePath, channelsBuffer, sampleRateBuffer)
                ?: return null

            val channels = channelsBuffer.get(0)
            val sampleRate = sampleRateBuffer.get(0)
            val samples = audioBuffer.remaining() / channels
            val duration = (samples.toDouble() / sampleRate).seconds

            val format = if (channels == 1) AL_FORMAT_MONO16 else AL_FORMAT_STEREO16

            val buffer = alGenBuffers()
            alBufferData(buffer, format, audioBuffer, sampleRate)

            val source = alGenSources()
            alSourcei(source, AL_BUFFER, buffer)

            return AudioSource(buffer, source, duration, sampleRate)
        }
    }

    fun loadWavFile(filePath: String): AudioSource? {
        val audioFile = File(filePath)
        val audioStream = AudioSystem.getAudioInputStream(audioFile)
        return loadFromAudioStream(audioStream)
    }

    fun loadMP3FromData(data: ByteArray): AudioSource? {
        val audio = AudioSystem.getAudioInputStream(data.inputStream())
        return loadFromAudioStream(
            convertToFullySpecifiedPCM(audio)
        )
    }

    fun loadFromAudioStream(audioStream: AudioInputStream): AudioSource? {
        val format = audioStream.format
        val sampleRate = format.sampleRate.toInt()
        val channels = format.channels
        val sampleSizeInBits = format.sampleSizeInBits

        // Convert to PCM if needed
        val pcmStream = if (format.encoding != AudioFormat.Encoding.PCM_SIGNED) {
            val pcmFormat = AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                format.sampleRate,
                16,
                channels,
                channels * 2,
                format.sampleRate,
                false
            )
            AudioSystem.getAudioInputStream(pcmFormat, audioStream)
        } else audioStream

        // Read all audio data
        val audioData = pcmStream.readAllBytes()
        val audioBuffer = MemoryUtil.memAlloc(audioData.size)
        audioBuffer.put(audioData).flip()

        val alFormat = when (channels) {
            1 if sampleSizeInBits == 8 -> AL_FORMAT_MONO8
            1 if sampleSizeInBits == 16 -> AL_FORMAT_MONO16
            2 if sampleSizeInBits == 8 -> AL_FORMAT_STEREO8
            2 if sampleSizeInBits == 16 -> AL_FORMAT_STEREO16
            else -> {
                MemoryUtil.memFree(audioBuffer)
                return null
            }
        }

        val buffer = alGenBuffers()
        alBufferData(buffer, alFormat, audioBuffer, sampleRate)
        MemoryUtil.memFree(audioBuffer)

        val source = alGenSources()
        alSourcei(source, AL_BUFFER, buffer)

        val samples = audioData.size / (channels * (sampleSizeInBits / 8))
        val duration = (samples.toDouble() / sampleRate).seconds

        audioStream.close()

        return AudioSource(buffer, source, duration, sampleRate)
    }

    fun convertToFullySpecifiedPCM(audioStream: AudioInputStream): AudioInputStream {
        val sourceFormat = audioStream.format

        // Create a fully specified PCM format
        val targetFormat = AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            sourceFormat.sampleRate.takeIf { it != AudioSystem.NOT_SPECIFIED.toFloat() } ?: 44100f,
            16, // 16-bit
            sourceFormat.channels.takeIf { it != AudioSystem.NOT_SPECIFIED } ?: 2,
            4, // 2 channels * 2 bytes per sample = 4 bytes per frame
            sourceFormat.sampleRate.takeIf { it != AudioSystem.NOT_SPECIFIED.toFloat() } ?: 44100f,
            false // little endian
        )

        // If source is already the target format, return as-is
        if (AudioSystem.isConversionSupported(targetFormat, sourceFormat)) {
            return AudioSystem.getAudioInputStream(targetFormat, audioStream)
        }

        // If direct conversion isn't supported, try intermediate conversion
        return try {
            AudioSystem.getAudioInputStream(targetFormat, audioStream)
        } catch (_: Exception) {
            // Fallback: try with different parameters
            val fallbackFormat = AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                44100f,
                16,
                2,
                4,
                44100f,
                false
            )
            AudioSystem.getAudioInputStream(fallbackFormat, audioStream)
        }
    }
}