package com.olcayaras.vidster.ui.screens.editor.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.olcayaras.vidster.ui.theme.AppTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

private val sheetRadius = RectangleShape

@Composable
fun EditorSheetContainer(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
        modifier = modifier.clip(sheetRadius)
    ) {
        Column(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

@Preview
@Composable
private fun EditorSheetContainerPreview() {
    AppTheme {
        Surface {
            EditorSheetContainer {
                Text("Properties")
                Text("Width: 1920")
                Text("Height: 1080")
            }
        }
    }
}