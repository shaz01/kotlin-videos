package com.olcayaras.lib.videobuilders

import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
sealed interface SequenceStart {
    @Serializable
    data object AfterPrevious : SequenceStart

    @Serializable
    data class FixedPoint(val start: Duration) : SequenceStart
}

@Serializable
sealed interface SequenceEnd {
    @Serializable
    data class UntilEnd(val extra: Duration = Duration.Companion.ZERO) : SequenceEnd

    @Serializable
    data class BeforeNext(val minimumLength: Duration = Duration.Companion.ZERO) : SequenceEnd

    @Serializable
    data class FixedPoint(val end: Duration) : SequenceEnd

    @Serializable
    data class FixedDuration(val duration: Duration) : SequenceEnd
}