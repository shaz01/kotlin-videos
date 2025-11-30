package com.olcayaras.lib

import com.olcayaras.lib.definitions.AudioDefinition
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.await
import org.w3c.fetch.Response
import org.khronos.webgl.Uint8Array
import kotlin.js.Promise
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Creates an AudioController implementation for JavaScript/Web platform using Web Audio API
 */
actual fun AudioController(definitions: List<AudioDefinition>): AudioController {
    val controller = WebAudioController(definitions)
    // Initialize audio loading asynchronously to avoid blocking
    GlobalScope.launch {
        controller.initialize()
    }
    return controller
}

/**
 * Represents a loaded audio buffer with metadata
 */
private data class AudioSource(
    val buffer: AudioBuffer,        // Web Audio API buffer containing decoded audio data
    val duration: Duration,         // Total duration of the audio clip
    val sampleRate: Int            // Sample rate of the audio (e.g., 44100 Hz)
)

/**
 * Represents an audio source that is currently playing
 */
private data class PlayingSource(
    val source: AudioBufferSourceNode,  // Web Audio API source node for playback
    val gainNode: GainNode,             // Volume control node (currently unused)
    val definition: AudioDefinition,     // Original audio definition for reference
    val startTime: Double,              // When playback started (AudioContext time)
    val startOffset: Double             // Offset within the audio clip where playback started
)

/**
 * Web Audio API implementation of AudioController for browser environments
 */
private class WebAudioController(override val definitions: List<AudioDefinition>) : AudioController {
    // Main Web Audio API context - manages all audio operations
    private val audioContext = AudioContext()
    
    // Current playback position in the timeline
    private var currentDuration: Duration = Duration.ZERO
    
    private var isPlaying = false
    private val audioSources = mutableMapOf<AudioDefinition, AudioSource>()
    
    // Map of currently playing audio sources
    private val playingSources = mutableMapOf<AudioDefinition, PlayingSource>()

    /**
     * Loads all audio definitions asynchronously
     */
    suspend fun initialize() {
        definitions.forEach { definition ->
            try {
                val source = loadAudio(definition)
                if (source != null) {
                    audioSources[definition] = source
                    println("Loaded audio with duration ${source.duration}")
                } else {
                    println("Failed to load audio for definition")
                }
            } catch (e: Exception) {
                println("Exception loading audio: ${e.message}")
            }
        }
        println("Loaded ${audioSources.size} audio sources")
    }

    /**
     * Updates the current playback time and manages which audio sources should be playing
     * This is called continuously during animation/video rendering
     */
    override fun updateTime(duration: Duration) {
        // Allow 100ms tolerance for timing adjustments to avoid constant seeking
        val slop = 100.milliseconds
        val difference = duration - currentDuration
        val adjustPosition = difference.absoluteValue > slop

        if (adjustPosition) {
            println("Adjusting position by $difference")
        }

        // Check each loaded audio source to see if it should be playing at this time
        audioSources.forEach { (definition, audioSource) ->
            val willPlay = duration in definition.from..definition.to  // Should this audio be playing now?
            val isCurrentlyPlaying = playingSources.containsKey(definition)  // Is it currently playing?

            if (!willPlay) {
                if (isCurrentlyPlaying) {
                    stopSource(definition)
                }
            } else {
                // Audio should be playing
                if (adjustPosition && isCurrentlyPlaying) {
                    // If we need to adjust position significantly, stop and restart with correct offset
                    stopSource(definition)
                }

                if (!isCurrentlyPlaying && isPlaying) {
                    // Start playing with the correct offset within the audio clip
                    val offsetInClip = duration - definition.from
                    startSource(definition, audioSource, offsetInClip.inWholeMilliseconds / 1000.0)
                    println("Started source playback")
                }
            }
        }

        currentDuration = duration
    }

    /**
     * Seeks to a specific time position, stopping all audio and restarting at the new position
     */
    override fun seekToDuration(duration: Duration) {
        // Stop all currently playing sources
        playingSources.keys.toList().forEach { definition ->
            stopSource(definition)
        }

        // Start sources that should be playing at the new position
        audioSources.forEach { (definition, audioSource) ->
            if (duration in definition.from..definition.to && isPlaying) {
                val offsetInClip = duration - definition.from
                startSource(definition, audioSource, offsetInClip.inWholeMilliseconds / 1000.0)
            }
        }

        currentDuration = duration
    }

    /**
     * Pauses all audio playback
     */
    override fun pause() {
        isPlaying = false
        // Stop all currently playing sources
        playingSources.keys.toList().forEach { definition ->
            stopSource(definition)
        }
    }

    /**
     * Resumes audio playback for sources that should be playing at current time
     */
    override fun play() {
        isPlaying = true
        // Start sources that should be playing at the current position
        audioSources.forEach { (definition, audioSource) ->
            if (currentDuration in definition.from..definition.to) {
                val offsetInClip = currentDuration - definition.from
                startSource(definition, audioSource, offsetInClip.inWholeMilliseconds / 1000.0)
            }
        }
    }

    /**
     * Starts playing an audio source with a specific offset
     */
    private fun startSource(definition: AudioDefinition, audioSource: AudioSource, offset: Double) {
        try {
            // Create a new source node for this playback instance
            val source = audioContext.createBufferSource()
            val gainNode = audioContext.createGain()  // For future volume control
            
            // Set up the audio graph: source -> gainNode -> speakers
            source.buffer = audioSource.buffer
            source.connect(gainNode)
            gainNode.connect(audioContext.destination)
            
            // Start playback with the specified offset (in seconds)
            val startTime = audioContext.currentTime
            source.start(startTime, offset)
            
            // Track this playing source
            val playingSource = PlayingSource(source, gainNode, definition, startTime, offset)
            playingSources[definition] = playingSource
            
            // Clean up when audio ends naturally
            source.onended = {
                playingSources.remove(definition)
            }
        } catch (e: Exception) {
            println("Error starting audio source: ${e.message}")
        }
    }

    /**
     * Stops a currently playing audio source
     */
    private fun stopSource(definition: AudioDefinition) {
        playingSources[definition]?.let { playingSource ->
            try {
                // Stop the Web Audio source node
                playingSource.source.stop()
            } catch (e: Exception) {
                println("Error stopping audio source: ${e.message}")
            }
            // Remove from tracking
            playingSources.remove(definition)
        }
    }

    /**
     * Loads audio data based on the definition type (file or TTS)
     */
    private suspend fun loadAudio(definition: AudioDefinition): AudioSource? {
        return try {
            when (definition) {
                is AudioDefinition.Resource -> {
                    // Load audio from a file URL
                    println("Loading audio from file: ${definition.file}")
                    loadFromFile(definition.file)
                }
                is AudioDefinition.TTS -> {
                    // Load audio from base64-encoded TTS data
                    println("Loading audio from TTS base64 (${definition.audioBase64.length} chars)")
                    loadFromBase64(definition.audioBase64)
                }
            }
        } catch (e: Exception) {
            println("Failed to load audio: ${e.message}")
            null
        }
    }

    /**
     * Loads audio from a file URL using fetch API
     */
    private suspend fun loadFromFile(filePath: String): AudioSource? {
        return try {
            // Fetch the audio file
            val response = fetch(filePath).await()
            if (!response.ok) {
                println("Failed to fetch audio file: ${response.status}")
                return null
            }
            
            // Get the raw audio data and decode it
            val arrayBuffer = response.arrayBuffer().await()
            val audioBuffer = audioContext.decodeAudioData(arrayBuffer).await()
            
            // Extract metadata
            val duration = audioBuffer.duration.seconds
            val sampleRate = audioBuffer.sampleRate.toInt()
            
            AudioSource(audioBuffer, duration, sampleRate)
        } catch (e: Exception) {
            println("Error loading audio from file: ${e.message}")
            null
        }
    }

    /**
     * Loads audio from base64-encoded data (typically from TTS services)
     */
    private suspend fun loadFromBase64(base64: String): AudioSource? {
        return try {
            // Decode base64 to binary string
            val binaryString = atob(base64)
            
            // Convert binary string to Uint8Array
            val bytes = Uint8Array(binaryString.length)
            for (i in 0 until binaryString.length) {
                bytes.asDynamic()[i] = binaryString[i].code.toByte()
            }
            
            // Get the ArrayBuffer and decode the audio
            val arrayBuffer = bytes.buffer
            val audioBuffer = audioContext.decodeAudioData(arrayBuffer).await()
            
            // Extract metadata
            val duration = audioBuffer.duration.seconds
            val sampleRate = audioBuffer.sampleRate.toInt()
            
            AudioSource(audioBuffer, duration, sampleRate)
        } catch (e: Exception) {
            println("Error loading audio from base64: ${e.message}")
            null
        }
    }
}

// External declarations for browser APIs

/**
 * Fetch API for loading audio files from URLs
 */
external fun fetch(url: String): Promise<Response>

/**
 * Base64 decoding function available in browsers
 */
external fun atob(encoded: String): String

/**
 * Web Audio API context - the main entry point for audio operations
 */
external class AudioContext {
    val currentTime: Double                // Current audio context time in seconds
    val destination: AudioDestinationNode  // The speakers/output
    fun createBufferSource(): AudioBufferSourceNode  // Creates a source for playing audio buffers
    fun createGain(): GainNode            // Creates a volume control node
    fun decodeAudioData(audioData: org.khronos.webgl.ArrayBuffer): Promise<AudioBuffer>  // Decodes audio data
}

/**
 * Contains decoded audio data ready for playback
 */
external interface AudioBuffer {
    val duration: Double    // Length of audio in seconds
    val sampleRate: Double  // Sample rate (e.g., 44100 Hz)
}

/**
 * Base class for all audio nodes in the Web Audio API graph
 */
external interface AudioNode {
    fun connect(destination: AudioNode)  // Connect this node to another node
}

/**
 * Represents the final output (speakers/headphones)
 */
external interface AudioDestinationNode : AudioNode

/**
 * A source node that plays audio from an AudioBuffer
 */
external interface AudioBufferSourceNode : AudioNode {
    var buffer: AudioBuffer?              // The audio data to play
    var onended: (() -> Unit)?           // Callback when playback ends
    fun start(startTime: Double = definedExternally, offset: Double = definedExternally)  // Start playback
    fun stop()                           // Stop playback
}

/**
 * A node for controlling volume/gain
 */
external interface GainNode : AudioNode {
    val gain: AudioParam  // The gain/volume parameter
}

/**
 * Represents an audio parameter that can be automated
 */
external interface AudioParam {
    var value: Double  // Current value of the parameter
}
