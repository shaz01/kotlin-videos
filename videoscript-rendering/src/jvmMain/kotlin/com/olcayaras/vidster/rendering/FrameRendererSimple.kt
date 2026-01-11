@file:OptIn(InternalComposeUiApi::class)

package com.olcayaras.vidster.rendering

import androidx.compose.runtime.*
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.InternalComposeUiApi
import org.jetbrains.skia.*
import androidx.compose.ui.unit.Density

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

    override fun cleanup() {
        scene.close()
    }
}