package com.olcayaras.vidster.ui.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

actual fun Modifier.onRightClick(onRightClick: (position: Offset) -> Unit): Modifier {
    // JS web can potentially support right-click but keeping it simple for now
    return this
}
