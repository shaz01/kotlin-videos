package com.olcayaras.vidster.export

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import com.olcayaras.figures.SegmentFrame
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
            exportSegmentFramesVideo(
                destination = destination,
                frames = frames,
                screenSize = screenSize,
                fps = fps,
                backgroundColor = backgroundColor,
            )
        }
        VideoExportResult.Success(destination)
    } catch (t: Throwable) {
        VideoExportResult.Failed(t)
    }
}

internal expect suspend fun exportSegmentFramesVideo(
    destination: PlatformFile,
    frames: List<SegmentFrame>,
    screenSize: IntSize,
    fps: Int,
    backgroundColor: Color,
)
