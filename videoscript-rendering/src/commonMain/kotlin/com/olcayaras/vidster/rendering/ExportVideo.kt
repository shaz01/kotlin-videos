package com.olcayaras.vidster.rendering

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import com.olcayaras.lib.LocalCurrentFrame
import com.olcayaras.lib.LocalFPS
import com.olcayaras.lib.LocalIsRendering
import com.olcayaras.lib.definitions.SequencesAsVideo
import com.olcayaras.lib.speech.TTSProvider
import com.olcayaras.lib.speech.asCachedTTSProvider
import com.olcayaras.lib.videobuilders.SequenceScope
import com.olcayaras.lib.videobuilders.buildVideo
import io.github.vinceglb.filekit.PlatformFile

suspend fun exportVideo(
    fps: Int = 60,
    ttsProvider: TTSProvider,
    screenSize: IntSize,
    exportTo: PlatformFile,
    background: Color = Color.White,
    density: Float = 1f,
    withAlpha: Boolean = background.alpha < 1f,
    videoFactory: suspend SequenceScope.() -> Unit
) {
    val ttsProvider = ttsProvider.asCachedTTSProvider()
    val video = buildVideo(ttsProvider, fps, videoFactory)
    val renderer = createFrameRenderer(screenSize.width, screenSize.height, Density(density))
    val exporter = createVideoRenderer(renderer)

    val totalDuration = video.duration
    var frame by mutableIntStateOf(0)

    // Handles the sequences part
    renderer.setContent {
        CompositionLocalProvider(
            LocalCurrentFrame provides frame,
            LocalFPS provides fps,
            LocalIsRendering provides true,
        ) {
            Box(Modifier.fillMaxSize().background(background)) {
                SequencesAsVideo(video.sequenceDefinitions)
            }
        }
    }

    exporter.exportVideo(
        outputPath = exportTo,
        fps = fps,
        totalDuration = totalDuration,
        onFrameRendered = { frameIndex, totalFrames ->
            frame = frameIndex + 1
            println("Rendering frame $frameIndex of $totalFrames")
        },
        audio = video.audioDefinitions,
        withAlpha = withAlpha,
    )

    renderer.cleanup()
}
