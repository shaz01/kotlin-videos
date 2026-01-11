package com.olcayaras.vidster.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import com.olcayaras.figures.SegmentFrame
import com.olcayaras.lib.speech.NoOpTTSProvider
import com.olcayaras.vidster.rendering.exportVideo
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.openFileSaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface VideoExportResult {
    data object Cancelled : VideoExportResult
    data class Success(val destination: PlatformFile) : VideoExportResult
    data class Failed(val error: Throwable) : VideoExportResult
}

suspend fun exportVideoWithDialog(
    frames: List<SegmentFrame>,
    screenSize: IntSize,
    fps: Int,
    backgroundColor: Color,
    suggestedName: String = "vidster-export",
    extension: String = "mp4",
): VideoExportResult {
    val destination = FileKit.openFileSaver(
        suggestedName = suggestedName,
        extension = extension,
    ) ?: return VideoExportResult.Cancelled

    return try {
        withContext(Dispatchers.Default) {
            exportVideo(
                fps = fps,
                ttsProvider = NoOpTTSProvider(),
                screenSize = screenSize,
                exportTo = destination,
                background = backgroundColor,
                videoFactory = { buildAnimation(frames, screenSize, fps) }
            )
        }
        VideoExportResult.Success(destination)
    } catch (t: Throwable) {
        VideoExportResult.Failed(t)
    }
}
