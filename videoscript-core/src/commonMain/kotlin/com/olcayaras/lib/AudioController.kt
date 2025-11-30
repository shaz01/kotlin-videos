package com.olcayaras.lib

import com.olcayaras.lib.definitions.AudioDefinition
import kotlin.time.Duration

interface AudioController {
    val definitions: List<AudioDefinition>

    /**
     * Updates the time. Pauses or plays the audio definitions based on it.
     */
    fun updateTime(duration: Duration)

    fun pause()
    fun play()
    fun seekToDuration(duration: Duration)
}

expect fun AudioController(definitions: List<AudioDefinition>): AudioController

object NoOpAudioController : AudioController {
    override val definitions: List<AudioDefinition> = emptyList()
    override fun updateTime(duration: Duration) = Unit
    override fun pause() = Unit
    override fun play() = Unit
    override fun seekToDuration(duration: Duration) = Unit
}
