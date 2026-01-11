package com.olcayaras.vidster.export

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import com.olcayaras.figures.SegmentFrame
import io.github.vinceglb.filekit.PlatformFile

internal actual suspend fun exportSegmentFramesVideo(
    destination: PlatformFile,
    frames: List<SegmentFrame>,
    screenSize: IntSize,
    fps: Int,
    backgroundColor: Color,
) {
    throw UnsupportedOperationException("Video export is not available on Android yet.")
}
