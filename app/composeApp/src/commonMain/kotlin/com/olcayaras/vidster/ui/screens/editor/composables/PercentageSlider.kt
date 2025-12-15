package com.olcayaras.vidster.ui.screens.editor.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.RotateCcw
import kotlin.math.roundToInt

/**
 * A reusable percentage slider component with editable percentage text.
 *
 * @param value Current zoom value (e.g., 1.0f = 100%)
 * @param onValueChange Callback when zoom value changes
 * @param modifier Modifier for the entire component
 * @param valueRange Range of allowed zoom values (default 0.5f to 2.0f)
 * @param resetValue Value to reset to when reset button is clicked (default 1.0f)
 * @param label Label text displayed on the left (default "Zoom")
 * @param showResetButton Whether to show the reset button (default true)
 */
@Composable
fun PercentageSlider(
    label: String ,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0.5f..2.0f,
    resetValue: Float = 1.0f,
    showResetButton: Boolean = true
) {
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf("") }

    val minPercent = (valueRange.start * 100).roundToInt()
    val maxPercent = (valueRange.endInclusive * 100).roundToInt()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.weight(1f))

        if (showResetButton) {
            IconButton(
                onClick = { onValueChange(resetValue) },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = FeatherIcons.RotateCcw,
                    contentDescription = "Reset $label",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(4.dp))
        }

        if (isEditing) {
            val focusRequester = remember { FocusRequester() }
            BasicTextField(
                value = editText,
                onValueChange = { new ->
                    val digits = new.filter { it.isDigit() }
                    editText = digits.trimStart('0').ifEmpty { "0" }
                },
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        editText.toIntOrNull()?.let { percent ->
                            val clamped = percent.coerceIn(minPercent, maxPercent) / 100f
                            onValueChange(clamped)
                        }
                        isEditing = false
                    }
                ),
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End
                ),
                modifier = Modifier
                    .width(48.dp)
                    .focusRequester(focusRequester)
            )
            Text("%", style = MaterialTheme.typography.bodySmall)

            LaunchedEffect(Unit) { focusRequester.requestFocus() }
        } else {
            Text(
                "${(value * 100).roundToInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable {
                    editText = (value * 100).roundToInt().toString()
                    isEditing = true
                }
            )
        }
    }

    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        modifier = Modifier.fillMaxWidth()
    )
}
