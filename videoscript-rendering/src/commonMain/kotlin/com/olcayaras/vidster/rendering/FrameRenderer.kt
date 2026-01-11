package com.olcayaras.vidster.rendering

import androidx.compose.runtime.*
import org.jetbrains.skia.*
import androidx.compose.ui.unit.Density

interface FrameRenderer {
    val width: Int
    val height: Int
    val density: Density
    fun setContent(content: @Composable () -> Unit)
    fun renderFrame(timeNanos: Long): Image

    fun cleanup()
}

expect fun createFrameRenderer(
    width: Int,
    height: Int,
    density: Density,
): FrameRenderer