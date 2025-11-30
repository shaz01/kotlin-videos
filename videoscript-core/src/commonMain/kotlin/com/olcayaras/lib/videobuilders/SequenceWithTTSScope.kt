@file:Suppress("FunctionName")

package com.olcayaras.lib.videobuilders

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import com.olcayaras.lib.currentDuration
import com.olcayaras.lib.definitions.SequenceAnimationScope
import com.olcayaras.lib.speech.SpeechWithTimestamps
import com.olcayaras.lib.speech.getSubtitles
import com.olcayaras.lib.subtitles.Subtitle
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO

interface SequenceWithTTSScope : SequenceScope {
    fun rangeOf(text: String): ClosedRange<Duration>

    @Composable
    fun getSubtitle(): Subtitle?

    suspend fun Sequence(
        triggerText: String,
        end: SequenceEnd? = SequenceEnd.BeforeNext(),
        enter: EnterTransition = fadeIn(),
        exit: ExitTransition = fadeOut(),
        tag: Int? = null,
        content: @Composable SequenceAnimationScope.() -> Unit,
    )
}

class SequenceWithTTSScopeNewImpl(
    val speech: SpeechWithTimestamps,
    val delegate: SequenceScope,
) : SequenceWithTTSScope, SequenceScope by delegate {
    override fun rangeOf(text: String): ClosedRange<Duration> {
        val range = speech.rangeOf(text)
        return range.start ..range.endInclusive
    }

    @Composable
    override fun getSubtitle(): Subtitle? {
        return getSubtitles(ZERO, speech)
    }

    override suspend fun Sequence(
        triggerText: String,
        end: SequenceEnd?,
        enter: EnterTransition,
        exit: ExitTransition,
        tag: Int?,
        content: @Composable (SequenceAnimationScope.() -> Unit),
    ) {
        val range = speech.rangeOf(triggerText)
        if (range.isEmpty()) {
            println("Trigger text not found in speech: $triggerText")
            return
        }
        delegate.Sequence(
            start = SequenceStart.FixedPoint(speech.rangeOf(triggerText).start),
            end = end ?: SequenceEnd.FixedPoint(range.endInclusive),
            enter = enter,
            exit = exit,
            content = content,
            tag = tag,
        )
    }
}


@Composable
fun SequenceWithTTSScope.isActive(text: String): Boolean {
    val range = rangeOf(text)
    return currentDuration() in range
}
