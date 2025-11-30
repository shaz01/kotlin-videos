package com.olcayaras.lib

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import com.olcayaras.previewer.utils.ScaleBox

@Composable
fun SingleFrame(
    modifier: Modifier = Modifier.Companion,
    screenSize: IntSize,
    contentScale: Float = 1f,
    frame: Int,
    background: Color = Color.White,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalCurrentFrame provides frame) {
        ScaleBox(
            modifier = modifier.background(background),
            screenSize = screenSize,
            contentScale = contentScale,
            content = content
        )
    }
}