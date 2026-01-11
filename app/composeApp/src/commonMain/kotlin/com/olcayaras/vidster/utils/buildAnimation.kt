package com.olcayaras.vidster.utils

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import com.olcayaras.figures.SegmentFrame
import com.olcayaras.figures.SegmentFrameCanvas
import com.olcayaras.lib.asDuration
import com.olcayaras.lib.definitions.VideoDefinition
import com.olcayaras.lib.speech.NoOpTTSProvider
import com.olcayaras.lib.videobuilders.SequenceScope
import com.olcayaras.lib.videobuilders.SequenceEnd
import com.olcayaras.lib.videobuilders.SequenceStart
import com.olcayaras.lib.videobuilders.buildVideo

suspend fun SequenceScope.addSegmentFrameSequences(
    frames: List<SegmentFrame>,
    screenSize: IntSize,
    fps: Int
) {
    val frameDuration = 1.asDuration(fps)
    frames.forEach { frame ->
        Sequence(
            start = SequenceStart.AfterPrevious,
            end = SequenceEnd.FixedDuration(frameDuration),
            enter = EnterTransition.None,
            exit = ExitTransition.None
        ) {
            SegmentFrameCanvas(
                modifier = Modifier.fillMaxSize(),
                frame = frame,
                viewportSize = screenSize
            )
        }
    }
}

suspend fun buildAnimation(
    frames: List<SegmentFrame>,
    screenSize: IntSize,
    fps: Int
): VideoDefinition {
    return buildVideo(fps = fps, ttsProvider = NoOpTTSProvider()) {
        addSegmentFrameSequences(frames, screenSize, fps)
    }
}
