package com.olcayaras.vidster.export

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import com.olcayaras.figures.SegmentFrame
import com.olcayaras.lib.speech.NoOpTTSProvider
import com.olcayaras.vidster.rendering.exportVideo
import com.olcayaras.vidster.utils.buildAnimation
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import java.nio.file.Paths

internal actual suspend fun exportSegmentFramesVideo(
    destination: PlatformFile,
    frames: List<SegmentFrame>,
    screenSize: IntSize,
    fps: Int,
    backgroundColor: Color,
) {
    exportVideo(
        fps = fps,
        ttsProvider = NoOpTTSProvider(),
        screenSize = screenSize,
        exportTo = Paths.get(destination.path),
        background = backgroundColor,
        videoFactory = { buildAnimation(frames, screenSize, fps) }
    )
}
