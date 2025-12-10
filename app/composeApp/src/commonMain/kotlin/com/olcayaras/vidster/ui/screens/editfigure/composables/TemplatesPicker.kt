package com.olcayaras.vidster.ui.screens.editfigure.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.olcayaras.vidster.ui.screens.editfigure.FigureTemplate
import compose.icons.FeatherIcons
import compose.icons.feathericons.ChevronDown
import compose.icons.feathericons.ChevronRight

/**
 * Expandable picker for figure templates.
 */
@Composable
fun TemplatesPicker(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onTemplateSelected: (FigureTemplate) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpanded() }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Templates", style = MaterialTheme.typography.titleMedium)
            Icon(
                if (expanded) FeatherIcons.ChevronDown else FeatherIcons.ChevronRight,
                contentDescription = if (expanded) "Collapse" else "Expand",
                modifier = Modifier.size(20.dp)
            )
        }

        // Template list
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FigureTemplate.entries.forEach { template ->
                    OutlinedCard(
                        onClick = { onTemplateSelected(template) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = template.displayName,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
