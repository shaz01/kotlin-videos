package com.olcayaras.vidster.ui.screens.editor.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.olcayaras.vidster.ui.screens.editor.OnionSkinMode
import compose.icons.FeatherIcons
import compose.icons.feathericons.ChevronDown
import compose.icons.feathericons.ChevronsLeft
import compose.icons.feathericons.ChevronsRight
import compose.icons.feathericons.Layers
import compose.icons.feathericons.X

@Composable
fun OnionSkinModePicker(
    selectedMode: OnionSkinMode,
    onModeSelected: (OnionSkinMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val tint = if (selectedMode != OnionSkinMode.Disabled)
        MaterialTheme.colorScheme.primary
    else
        LocalContentColor.current

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = FeatherIcons.Layers,
                    contentDescription = "Onion Skin: ${selectedMode.label}",
                    tint = tint
                )
                Icon(
                    imageVector = FeatherIcons.ChevronDown,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = tint
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            OnionSkinMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.label) },
                    onClick = {
                        onModeSelected(mode)
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = mode.icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }
    }
}

private val OnionSkinMode.icon: ImageVector
    get() = when (this) {
        OnionSkinMode.Disabled -> FeatherIcons.X
        OnionSkinMode.Previous -> FeatherIcons.ChevronsLeft
        OnionSkinMode.Future -> FeatherIcons.ChevronsRight
        OnionSkinMode.Both -> FeatherIcons.Layers
    }

private val OnionSkinMode.label: String
    get() = when (this) {
        OnionSkinMode.Disabled -> "Disabled"
        OnionSkinMode.Previous -> "Previous"
        OnionSkinMode.Future -> "Future"
        OnionSkinMode.Both -> "Both"
    }
