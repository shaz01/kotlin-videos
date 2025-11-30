package com.olcayaras.lib.speech

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.olcayaras.lib.currentDuration
import com.olcayaras.lib.subtitles.Subtitle
import com.olcayaras.lib.subtitles.generateSubtitles
import com.olcayaras.lib.videobuilders.SequenceScope
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Immutable
@Serializable
data class SpeechWithTimestamps(
    val audio: String,
    val chars: List<String>,
    val startSeconds: List<Double>,
    val endSeconds: List<Double>,
) {
    init {
        require(chars.size == startSeconds.size && chars.size == endSeconds.size) {
            "chars, startSeconds, and endSeconds must have the same size"
        }
    }

    /**
     * Returns the time range during which the specified text is spoken.
     *
     * Searches for the given text within the character sequence and calculates
     * the start and end times based on the character timestamps.
     *
     * @param text The text to find the duration for
     * @return A closed range of Duration representing when the text is spoken,
     *         or Duration.ZERO..Duration.ZERO if the text is empty or not found
     */
    fun rangeOf(text: String): ClosedRange<Duration> {
        if (text.isEmpty()) return Duration.ZERO..Duration.ZERO

        val textChars = text.toList().map { it.toString() }
        val startIndex = findSequenceStart(textChars)

        if (startIndex == -1) return Duration.ZERO..Duration.ZERO

        val endIndex = startIndex + textChars.size - 1
        val startTime = startSeconds[startIndex].seconds

        val endTime = if (endIndex < startSeconds.size - 1) {
            endSeconds[endIndex + 1].seconds
        } else {
            endSeconds[endIndex].seconds + 0.1.seconds
        }

        return startTime..endTime
    }

    private fun findSequenceStart(textChars: List<String>): Int {
        // TODO OPTIMIZE!
        for (i in 0..chars.size - textChars.size) {
            if (chars.subList(i, i + textChars.size) == textChars) {
                return i
            }
        }
        return -1
    }

    val length by lazy { endSeconds.lastOrNull()?.seconds ?: Duration.ZERO }

    val subtitles = generateSubtitles()
}


//TODO optimize!
@Composable
fun SequenceScope.getSubtitles(speechStart: Duration, speechWithTimestamps: SpeechWithTimestamps): Subtitle? {
    val now = currentDuration() - speechStart

    return speechWithTimestamps.subtitles.find { srt -> now in srt.startTime..srt.endTime }
}


