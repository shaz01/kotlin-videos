package com.olcayaras.vidster.ui.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

/**
 * Adds right-click detection to a composable.
 * On desktop: triggers onRightClick with the click position
 * On mobile: no-op (right-click doesn't exist)
 */
expect fun Modifier.onRightClick(onRightClick: (position: Offset) -> Unit): Modifier
