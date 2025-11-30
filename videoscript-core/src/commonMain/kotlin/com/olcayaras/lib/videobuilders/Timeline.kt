package com.olcayaras.lib.videobuilders

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import com.olcayaras.lib.definitions.SequenceAnimationScope
import com.olcayaras.lib.speech.SpeechWithTimestamps
import kotlinx.serialization.Serializable
import kotlin.time.Duration

sealed interface VidComp

infix fun <T : VidComp> T.withTime(timing: Timing): TimedVidComp<T> {
    return TimedVidComp(timing, this)
}

@Serializable
data class Timing(val from: Duration, val end: SequenceEnd, val isUntilEnd: Boolean = end is SequenceEnd.UntilEnd) {
    fun absoluteEnd(): Duration? = when (end) {
        is SequenceEnd.FixedDuration -> from + end.duration
        is SequenceEnd.FixedPoint -> end.end
        is SequenceEnd.BeforeNext -> null
        is SequenceEnd.UntilEnd -> null
    }
}

@Serializable
data class AbsoluteTiming(val from: Duration, val end: Duration)

data class SequenceDef(
    val enter: EnterTransition,
    val exit: ExitTransition,
    val content: @Composable SequenceAnimationScope.() -> Unit,
    val tag: Int? = null,
) : VidComp

sealed interface AudioDef : VidComp {
    data class TTS(
        val audioBase64: String,
        val text: String,
        val speechWithTimestamps: SpeechWithTimestamps
    ) : AudioDef

    data class Resource(
        val file: String,
        val mediaFrom: Duration,
        val mediaTo: Duration,
    ): AudioDef
}

@Serializable
data class TimedVidComp<out T : VidComp>(val timing: Timing, val comp: T)


class TimelineBuilder {
    private val items: MutableList<TimedVidComp<*>> = mutableListOf()
    var currentPosition: Duration = Duration.ZERO
        private set
    private var built = false

    private fun setCursorToAtLeast(duration: Duration) {
        currentPosition = maxOf(duration, currentPosition)
    }

    private fun getStartAbsolute(start: SequenceStart): Duration {
        return when (start) {
            SequenceStart.AfterPrevious -> {
                if (items.isEmpty()) currentPosition // which should be zero
                else items.maxOf { (timing, _) ->
                    if (timing.end is SequenceEnd.BeforeNext) {
                        val start = timing.from
                        start + timing.end.minimumLength
                    } else {
                        currentPosition
                    }
                }
            }

            is SequenceStart.FixedPoint -> start.start
        }
    }

    fun timingOf(def: VidComp): Timing? = items.find { it.comp == def }?.timing

    fun add(
        start: SequenceStart,
        end: SequenceEnd,
        components: List<VidComp>,
        resolveDisabled: Boolean = false // for e.g. adding audio
    ) {
        if (built) throw IllegalStateException()
        if (components.isEmpty()) return

        val startAbsolute = getStartAbsolute(start)
        if (!resolveDisabled) resolveEndBeforeNext(startAbsolute)

        // Move the cursor:
        when (end) {
            is SequenceEnd.FixedDuration -> setCursorToAtLeast(startAbsolute + end.duration)
            is SequenceEnd.FixedPoint -> setCursorToAtLeast(end.end)
            is SequenceEnd.BeforeNext -> setCursorToAtLeast(startAbsolute + end.minimumLength)
            is SequenceEnd.UntilEnd -> {}
        }

        val timing = Timing(from = startAbsolute, end = end)

        components.forEach { component ->
            items.add(component withTime timing)
        }
    }

    fun add(
        start: SequenceStart,
        end: SequenceEnd,
        timeline: List<TimedVidComp<*>>,
    ): AbsoluteTiming {
        if (built) throw IllegalStateException()
        val startAbsolute = getStartAbsolute(start)
        resolveEndBeforeNext(startAbsolute)

        val absoluteEnd = when (end) {
            is SequenceEnd.FixedDuration -> startAbsolute + end.duration
            is SequenceEnd.FixedPoint -> startAbsolute + end.end

            // null means no clamping.
            is SequenceEnd.BeforeNext -> null
            is SequenceEnd.UntilEnd -> null
        }

        var maxEnd = absoluteEnd ?: Duration.ZERO
        timeline.forEach { (oldTiming, comp) ->
            val newStart = oldTiming.from + startAbsolute
            val newEnd = run {
                val isUntilEnd = oldTiming.isUntilEnd
                if (isUntilEnd && absoluteEnd != null) return@run absoluteEnd
                // a timelineBuilder's output can't have non-absolute endings
                val nonClamped = startAbsolute + (oldTiming.absoluteEnd() ?: throw IllegalArgumentException())
                val clamped = if (absoluteEnd != null) nonClamped.coerceAtMost(absoluteEnd) else nonClamped
                clamped
            }

            maxEnd = maxEnd.coerceAtLeast(newEnd)
            val newTiming = Timing(newStart, SequenceEnd.FixedPoint(newEnd))
            items.add(comp withTime newTiming)
        }

        setCursorToAtLeast(maxEnd)
        return AbsoluteTiming(startAbsolute, maxEnd)
    }


    private fun resolveEndBeforeNext(startAbsolute: Duration) {
        items.forEachIndexed { index, item ->
            if (item.timing.end is SequenceEnd.BeforeNext && item.timing.from <= startAbsolute) {
                val newItem = item.copy(timing = item.timing.copy(end = SequenceEnd.FixedPoint(startAbsolute)))
                items[index] = newItem
            }
        }
    }

    fun build(): List<TimedVidComp<*>> {
        if (built) return items

        val endAbsolute = items.maxOfOrNull { (timing, _) -> timing.absoluteEnd() ?: Duration.ZERO } ?: Duration.ZERO
        items.forEachIndexed { index, item ->
            if (item.timing.end is SequenceEnd.BeforeNext || item.timing.end is SequenceEnd.UntilEnd) {
                val newTiming = item.timing.copy(end = SequenceEnd.FixedPoint(endAbsolute), isUntilEnd = true)
                val newItem = item.copy(timing = newTiming)
                items[index] = newItem
            }
        }
        built = true
        return items
    }
}
