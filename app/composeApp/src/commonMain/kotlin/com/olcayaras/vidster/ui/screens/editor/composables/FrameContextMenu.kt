package com.olcayaras.vidster.ui.screens.editor.composables

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowDown
import compose.icons.feathericons.ArrowUp
import compose.icons.feathericons.CheckSquare
import compose.icons.feathericons.Copy
import compose.icons.feathericons.Trash2

@Composable
fun FrameContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    canDelete: Boolean,
    showDesktopTip: Boolean = false,
    onDuplicate: () -> Unit,
    onInsertBefore: () -> Unit,
    onInsertAfter: () -> Unit,
    onDelete: () -> Unit,
    onSelect: () -> Unit,
    offset: DpOffset = DpOffset.Zero
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        offset = offset
    ) {
        DropdownMenuItem(
            text = { Text("Duplicate") },
            onClick = {
                onDuplicate()
                onDismiss()
            },
            leadingIcon = {
                Icon(
                    imageVector = FeatherIcons.Copy,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )
        DropdownMenuItem(
            text = { Text("Insert Before") },
            onClick = {
                onInsertBefore()
                onDismiss()
            },
            leadingIcon = {
                Icon(
                    imageVector = FeatherIcons.ArrowUp,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )
        DropdownMenuItem(
            text = { Text("Insert After") },
            onClick = {
                onInsertAfter()
                onDismiss()
            },
            leadingIcon = {
                Icon(
                    imageVector = FeatherIcons.ArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        DropdownMenuItem(
            text = { Text("Select") },
            onClick = {
                onSelect()
                onDismiss()
            },
            leadingIcon = {
                Icon(
                    imageVector = FeatherIcons.CheckSquare,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )

        if (canDelete) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                onClick = {
                    onDelete()
                    onDismiss()
                },
                leadingIcon = {
                    Icon(
                        imageVector = FeatherIcons.Trash2,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            )
        }

        if (showDesktopTip) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = "Tip: Right-click for quick access",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}
