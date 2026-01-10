package com.olcayaras.vidster.ui.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

actual fun Modifier.onRightClick(onRightClick: (position: Offset) -> Unit): Modifier {
    // Right-click doesn't exist on Android, return unmodified
    return this
}
