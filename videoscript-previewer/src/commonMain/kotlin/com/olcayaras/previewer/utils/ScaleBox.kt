package com.olcayaras.previewer.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize

@Composable
fun ScaleBox(
    modifier: Modifier,
    screenSize: IntSize,
    contentScale: Float,
    content: @Composable () -> Unit
) {
    var composeSize by remember { mutableStateOf(IntSize.Zero) }
    val aspectRatio = screenSize.width.toFloat() / screenSize.height.toFloat()
    val scale by derivedStateOf {
        if (composeSize == IntSize.Zero || screenSize.width == 0 || screenSize.height == 0) {
            1f
        } else if (screenSize.width > screenSize.height) {
            composeSize.width.toFloat() / screenSize.width
        } else {
            composeSize.height.toFloat() / screenSize.height
        }
    }
    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { composeSize = it }
            .aspectRatio(aspectRatio)
    ) {
        CompositionLocalProvider(LocalDensity provides Density(scale * contentScale, fontScale = 1f)) {
            content()
        }
    }
}