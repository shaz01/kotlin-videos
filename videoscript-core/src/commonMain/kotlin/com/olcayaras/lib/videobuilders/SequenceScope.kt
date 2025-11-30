@file:Suppress("FunctionName")

package com.olcayaras.lib.videobuilders

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import com.olcayaras.lib.definitions.SequenceAnimationScope
import com.olcayaras.lib.speech.TTSProvider
import kotlin.time.Duration

interface SequenceScope {
    val absoluteStart: Duration

    suspend fun Sequence(
        start: SequenceStart = SequenceStart.AfterPrevious,
        end: SequenceEnd = SequenceEnd.BeforeNext(),
        enter: EnterTransition = fadeIn(),
        exit: ExitTransition = fadeOut(),
        tag: Int? = null,
        content: @Composable SequenceAnimationScope.() -> Unit,
    )

    // starts a new sequence scope
    suspend fun Subsequence(
        start: SequenceStart = SequenceStart.AfterPrevious,
        end: SequenceEnd = SequenceEnd.BeforeNext(),
        content: suspend SequenceScope.() -> Unit
    )

    suspend fun TTS(
        text: String,
        start: SequenceStart = SequenceStart.AfterPrevious,
        end: SequenceEnd? = null,
        content: suspend SequenceWithTTSScope.() -> Unit
    )

    suspend fun WithVideo(
        videoPath: String,
        mediaStart: Duration = Duration.ZERO,
        mediaEnd: MediaEnd = MediaEnd.UntilEnd,
        start: (Duration) -> SequenceStart = { SequenceStart.AfterPrevious },
        end: (Duration) -> SequenceEnd = { length -> SequenceEnd.FixedDuration(length) },
        content: suspend SequenceScope.(MediaResource.Video) -> Unit
    )
}

suspend fun SequenceScope.VideoSequence(
    videoPath: String,
    mediaStart: Duration = Duration.ZERO,
    mediaEnd: MediaEnd = MediaEnd.UntilEnd,
    start: (Duration) -> SequenceStart = { SequenceStart.AfterPrevious },
    end: (Duration) -> SequenceEnd = { length -> SequenceEnd.FixedDuration(length) },
    content: @Composable (SequenceAnimationScope.(MediaResource.Video) -> Unit)
) {
    WithVideo(
        videoPath = videoPath,
        mediaStart = mediaStart,
        mediaEnd = mediaEnd,
        start = start,
        end = end,
        content = { video ->
            Sequence(end = SequenceEnd.UntilEnd()){
                content(video)
            }
        }
    )
}

class SequenceScopeNewImpl(
    val fps: Int,
    val tts: TTSProvider,
    val mediaResourceScope: MediaResourceScope = createMediaResourceScope(fps),
    override val absoluteStart: Duration = Duration.ZERO
) : SequenceScope, MediaResourceScope by mediaResourceScope {
    val timeline = TimelineBuilder()

    override suspend fun Sequence(
        start: SequenceStart,
        end: SequenceEnd,
        enter: EnterTransition,
        exit: ExitTransition,
        tag: Int?,
        content: @Composable (SequenceAnimationScope.() -> Unit),
    ) {
        timeline.add(
            start = start,
            end = end,
            components = listOf(
                SequenceDef(enter, exit, content, tag)
            )
        )
    }

    private suspend fun Subsequence(
        start: SequenceStart,
        end: SequenceEnd,
        spanningComponents: List<VidComp>,
        resolvingDisabled: Boolean = false,
        content: suspend SequenceScope.() -> Unit
    ) {
        val newScope = SequenceScopeNewImpl(fps, tts, mediaResourceScope)
        newScope.content()
        val subTimeline = newScope.build()
        val (absStart, absEnd) = timeline.add(start, end, subTimeline)
        timeline.add(
            SequenceStart.FixedPoint(absStart),
            SequenceEnd.FixedPoint(absEnd),
            components = spanningComponents,
            resolveDisabled = resolvingDisabled
        )
    }


    override suspend fun Subsequence(
        start: SequenceStart,
        end: SequenceEnd,
        content: suspend SequenceScope.() -> Unit
    ) {
        Subsequence(start, end, emptyList(), false, content)
    }

    override suspend fun TTS(
        text: String,
        start: SequenceStart,
        end: SequenceEnd?,
        content: suspend SequenceWithTTSScope.() -> Unit
    ) {
        val speech = tts(text)
        val audioDef = AudioDef.TTS(
            audioBase64 = speech.audio,
            text = text,
            speechWithTimestamps = speech
        )
        val subsequenceEnd = end ?: SequenceEnd.FixedDuration(speech.length)

        timeline.add(
            start = start,
            end = subsequenceEnd,
            components = listOf(audioDef),
            resolveDisabled = true
        )

        val timing = timeline.timingOf(audioDef)!!
        Subsequence(
            start = SequenceStart.FixedPoint(timing.from),
            end = subsequenceEnd,
            content = {
                val withTTS = SequenceWithTTSScopeNewImpl(speech, this)
                withTTS.content()
            }
        )
    }

    override suspend fun WithVideo(
        videoPath: String,
        mediaStart: Duration,
        mediaEnd: MediaEnd,
        start: (videoLength: Duration) -> SequenceStart,
        end: (videoLength: Duration) -> SequenceEnd,
        content: suspend SequenceScope.(MediaResource.Video) -> Unit
    ) {
        val media = video(videoPath, mediaStart, mediaEnd)
        val mediaLength = media.getLength()
        val start = start(mediaLength)
        val end = end(mediaLength)

        Subsequence(
            start = start,
            end = end,
            content = { content(media) },
            spanningComponents = listOf(
                AudioDef.Resource(
                    file = videoPath,
                    mediaFrom = mediaStart,
                    mediaTo = mediaEnd.getAbsolute(mediaStart, media)
                )
            )
        )
    }

    fun build() = timeline.build()
}
