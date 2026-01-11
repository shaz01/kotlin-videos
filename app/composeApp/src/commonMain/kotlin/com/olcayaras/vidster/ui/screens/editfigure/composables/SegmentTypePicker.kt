package com.olcayaras.vidster.ui.screens.editfigure.composables

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.olcayaras.figures.SegmentType
import kotlin.math.PI

/**
 * Picker for selecting segment types with parameter controls for Ellipse and Arc.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentTypePicker(
    label: String = "Type",
    currentType: SegmentType,
    onTypeSelected: (SegmentType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = currentType.displayName(),
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                singleLine = true
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                SegmentTypeOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.displayName) },
                        onClick = {
                            onTypeSelected(option.toSegmentType())
                            expanded = false
                        }
                    )
                }
            }
        }

        // Additional parameters for Ellipse and Arc
        when (currentType) {
            is SegmentType.Ellipse -> {
                Spacer(Modifier.height(8.dp))
                Text("Width Ratio", style = MaterialTheme.typography.labelSmall)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Slider(
                        value = currentType.widthRatio,
                        onValueChange = { onTypeSelected(SegmentType.Ellipse(it)) },
                        valueRange = 0.1f..2f,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "%.2f".format(currentType.widthRatio),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(40.dp)
                    )
                }
            }
            is SegmentType.Arc -> {
                Spacer(Modifier.height(8.dp))
                Text("Sweep Angle", style = MaterialTheme.typography.labelSmall)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Slider(
                        value = currentType.sweepAngle,
                        onValueChange = { onTypeSelected(SegmentType.Arc(it)) },
                        valueRange = 0.1f..(2 * PI.toFloat()),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${(currentType.sweepAngle * 180 / PI).toInt()}°",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(40.dp)
                    )
                }
            }
            else -> {}
        }
    }
}

private enum class SegmentTypeOption(val displayName: String) {
    LINE("Line"),
    CIRCLE("Circle"),
    FILLED_CIRCLE("Filled Circle"),
    RECTANGLE("Rectangle"),
    ELLIPSE("Ellipse"),
    ARC("Arc");

    fun toSegmentType(): SegmentType = when (this) {
        LINE -> SegmentType.Line
        CIRCLE -> SegmentType.Circle
        FILLED_CIRCLE -> SegmentType.FilledCircle
        RECTANGLE -> SegmentType.Rectangle
        ELLIPSE -> SegmentType.Ellipse()
        ARC -> SegmentType.Arc()
    }
}

private fun SegmentType.displayName(): String = when (this) {
    SegmentType.Line -> "Line"
    SegmentType.Circle -> "Circle"
    SegmentType.FilledCircle -> "Filled Circle"
    SegmentType.Rectangle -> "Rectangle"
    is SegmentType.Ellipse -> "Ellipse"
    is SegmentType.Arc -> "Arc"
}
