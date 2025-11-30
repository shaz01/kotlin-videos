@file:OptIn(InternalComposeUiApi::class)

package com.olcayaras.vidster.rendering

import androidx.compose.runtime.*
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
import org.jetbrains.skia.*
import androidx.compose.ui.unit.Density


interface FrameRenderer {
    val width: Int
    val height: Int
    val density: Density
    fun setContent(content: @Composable () -> Unit)
    fun renderFrame(timeNanos: Long): Image
}

class FrameRendererSimple(
    override val width: Int,
    override val height: Int,
    override val density: Density = Density(1f)
) : FrameRenderer {
    val scene = ImageComposeScene(
        width = width,
        height = height,
        density = density
    )

    override fun setContent(content: @Composable () -> Unit) {
        scene.setContent(content)
    }

    override fun renderFrame(timeNanos: Long): Image {
        return scene.render(timeNanos)
    }

    fun cleanup() {
        scene.close()
    }
}