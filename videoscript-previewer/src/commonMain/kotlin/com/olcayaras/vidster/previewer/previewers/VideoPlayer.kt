package com.olcayaras.vidster.previewer.previewers

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import com.olcayaras.lib.LocalCurrentFrame
import com.olcayaras.lib.LocalFPS
import com.olcayaras.vidster.previewer.VideoController
import com.olcayaras.vidster.previewer.utils.ScaleBox

@Composable
fun VideoPlayer(
    modifier: Modifier = Modifier.Companion,
    screenSize: IntSize,
    contentScale: Float = 1f,
    background: Color = Color.White,
    controller: VideoController,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalFPS provides controller.fps) {
        LaunchedEffect(controller.isPlaying) {
            controller.updateFrames()
        }

        ScaleBox(
            modifier = modifier
                .background(background),
            screenSize = screenSize,
            contentScale = contentScale,
        ) {
            CompositionLocalProvider(LocalCurrentFrame provides controller.currentFrame.value) {
                content()
            }
        }
    }
}

@Composable
fun VideoPlayer(
    modifier: Modifier = Modifier.Companion,
    screenSize: IntSize,
    contentScale: Float = 1f,
    background: Color = Color.White,
    fps: Int,
    currentFrame: Int,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalFPS provides fps) {
        ScaleBox(
            modifier = modifier
                .background(background),
            screenSize = screenSize,
            contentScale = contentScale,
        ) {
            CompositionLocalProvider(LocalCurrentFrame provides currentFrame) {
                content()
            }
        }
    }
}