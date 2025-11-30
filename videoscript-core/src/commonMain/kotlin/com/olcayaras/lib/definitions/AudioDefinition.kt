package com.olcayaras.lib.definitions

import com.olcayaras.lib.speech.SpeechWithTimestamps
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
sealed interface AudioDefinition: VideoComponent {
    @Serializable
    data class TTS(
        override val from: Duration,
        override val to: Duration,
        val audioBase64: String,
        val text: String,
        val speechWithTimestamps: SpeechWithTimestamps,
    ): AudioDefinition

    @Serializable
    data class Resource(
        override val from: Duration,
        override val to: Duration,
        val file: String,
        val mediaFrom: Duration,
        val mediaTo: Duration,
    ): AudioDefinition
}