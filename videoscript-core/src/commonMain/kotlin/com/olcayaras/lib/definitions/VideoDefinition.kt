@file:Suppress("UNCHECKED_CAST")

package com.olcayaras.lib.definitions

import com.olcayaras.lib.subtitles.Subtitle
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
data class VideoDefinition(val components: List<VideoComponent>) {
    val audioDefinitions = components.filterIsInstance<AudioDefinition>()
    val sequenceDefinitions = components.filterIsInstance<SequenceDefinition>()

    val duration by lazy { sequenceDefinitions.map { it.to }.reduce { a, b -> a.coerceAtLeast(b) } }
}

fun VideoDefinition.currentSubtitle(currentDuration: Duration): Subtitle? {
    val ttsInTime = audioDefinitions.find { currentDuration in it.from..it.to && it is AudioDefinition.TTS }
    if (ttsInTime == null) return null
    ttsInTime as AudioDefinition.TTS

    val speechStart = ttsInTime.from
    val speechWithTimestamps = ttsInTime.speechWithTimestamps
    val now = currentDuration - speechStart

    return speechWithTimestamps.subtitles.find { srt -> now in srt.startTime..srt.endTime }
}

enum class LayerType {
    VIDEO, AUDIO
}

data class Layer(
    val name: String,
    val definitions: List<VideoComponent>,
    val type: LayerType,
)

@Serializable
sealed interface VideoComponent {
    val from: Duration
    val to: Duration
}

