package com.olcayaras.vidster.ui.screens.editfigure.composables

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.olcayaras.figures.Joint
import com.olcayaras.figures.SegmentType
import compose.icons.FeatherIcons
import compose.icons.feathericons.Plus
import compose.icons.feathericons.Trash2
import kotlin.math.PI

/**
 * Panel showing properties of the selected joint with editable fields.
 */
@Composable
fun JointPropertiesPanel(
    selectedJoint: Joint?,
    onUpdateId: (String) -> Unit,
    onUpdateLength: (Float) -> Unit,
    onUpdateAngle: (Float) -> Unit,
    onUpdateType: (SegmentType) -> Unit,
    onAddChild: () -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Joint Properties", style = MaterialTheme.typography.titleMedium)

        if (selectedJoint == null) {
            Text(
                "Select a joint to edit its properties",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            // Use key only for joint ID - don't include modification count as it breaks slider drag
            key(selectedJoint.id) {
                JointPropertyFields(
                    selectedJoint = selectedJoint,
                    onUpdateId = onUpdateId,
                    onUpdateLength = onUpdateLength,
                    onUpdateAngle = onUpdateAngle,
                    onUpdateType = onUpdateType,
                    onAddChild = onAddChild,
                    onDelete = onDelete,
                    canDelete = canDelete
                )
            }
        }
    }
}

@Composable
private fun JointPropertyFields(
    selectedJoint: Joint,
    onUpdateId: (String) -> Unit,
    onUpdateLength: (Float) -> Unit,
    onUpdateAngle: (Float) -> Unit,
    onUpdateType: (SegmentType) -> Unit,
    onAddChild: () -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean
) {
    // Local state for sliders to ensure smooth dragging
    var localLength by remember(selectedJoint.id) { mutableStateOf(selectedJoint.length) }
    var localAngle by remember(selectedJoint.id) { mutableStateOf(selectedJoint.angle) }

    // ID field
    OutlinedTextField(
        value = selectedJoint.id,
        onValueChange = onUpdateId,
        label = { Text("ID") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    // Length slider - use local state for smooth updates
    Column {
        Text("Length: ${localLength.toInt()}", style = MaterialTheme.typography.labelMedium)
        Slider(
            value = localLength,
            onValueChange = { newLength ->
                localLength = newLength
                onUpdateLength(newLength)
            },
            valueRange = 0f..200f,
            modifier = Modifier.fillMaxWidth()
        )
    }

    // Angle slider (in degrees for display) - use local state for smooth updates
    Column {
        val angleDegrees = (localAngle * 180 / PI).toInt()
        Text("Angle: $angleDegrees°", style = MaterialTheme.typography.labelMedium)
        Slider(
            value = localAngle,
            onValueChange = { newAngle ->
                localAngle = newAngle
                onUpdateAngle(newAngle)
            },
            valueRange = (-PI.toFloat())..(PI.toFloat()),
            modifier = Modifier.fillMaxWidth()
        )
    }

    // Segment type picker
    SegmentTypePicker(
        currentType = selectedJoint.type,
        onTypeSelected = onUpdateType
    )

    Spacer(Modifier.height(8.dp))

    // Action buttons
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilledTonalButton(
            onClick = onAddChild,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                FeatherIcons.Plus,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("Add Child")
        }

        OutlinedButton(
            onClick = onDelete,
            enabled = canDelete,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                FeatherIcons.Trash2,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("Delete")
        }
    }
}
