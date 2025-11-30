package com.olcayaras.previewer

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.olcayaras.lib.speech.TTSProvider
import com.olcayaras.lib.speech.asCachedTTSProvider
import com.olcayaras.lib.videobuilders.SequenceScope
import com.olcayaras.lib.videobuilders.buildVideo

suspend fun videoPlayerWindow(
    screenSize: IntSize,
    ttsProvider: TTSProvider,
    contentScale: Float = 1f,
    backgroundColor: Color = Color.White,
    content: suspend SequenceScope.() -> Unit,
    fps: Int = 60,
) {
    val ttsProvider = ttsProvider.asCachedTTSProvider()
    val video = buildVideo(ttsProvider, fps, content)
    application {
        Window(
            onCloseRequest = ::exitApplication,
            state = rememberWindowState()
        ) {
            VideoPlayerUI(screenSize, contentScale, backgroundColor, fps, video)
        }
    }
}
