package com.olcayaras.vidster.ui.screens.editor.composables

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.olcayaras.vidster.ui.theme.AppTheme
import compose.icons.FeatherIcons
import compose.icons.feathericons.Play
import compose.icons.feathericons.Plus
import compose.icons.feathericons.Type
import org.jetbrains.compose.ui.tooling.preview.Preview

private val toolbarRadius = RoundedCornerShape(24.dp)

@Composable
fun EditorToolbar(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(8.dp),
    content: @Composable () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(contentPadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
        }
    }
}

@Preview
@Composable
private fun EditorToolbarPreview() {
    AppTheme {
        Surface {
            EditorToolbar {
                IconButton(onClick = {}) {
                    Icon(FeatherIcons.Type, contentDescription = null)
                }
                IconButton(onClick = {}) {
                    Icon(FeatherIcons.Plus, contentDescription = null)
                }
                FilledTonalIconButton(onClick = {}) {
                    Icon(FeatherIcons.Play, contentDescription = null)
                }
            }
        }
    }
}