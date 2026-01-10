package com.olcayaras.vidster.ui.util

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput

@OptIn(ExperimentalComposeUiApi::class)
actual fun Modifier.onRightClick(onRightClick: (position: Offset) -> Unit): Modifier {
    return this.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Press &&
                    event.button == PointerButton.Secondary
                ) {
                    val position = event.changes.firstOrNull()?.position
                    if (position != null) {
                        onRightClick(position)
                    }
                }
            }
        }
    }
}
