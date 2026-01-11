package com.olcayaras.vidster.ui.screens.editfigure

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.olcayaras.figures.CanvasState
import com.olcayaras.figures.CompiledJoint
import com.olcayaras.figures.SegmentType
import com.olcayaras.vidster.ui.screens.editfigure.composables.FigureEditorCanvas
import com.olcayaras.vidster.ui.screens.editfigure.composables.JointPropertiesPanel
import com.olcayaras.vidster.ui.screens.editfigure.composables.SegmentTypePicker
import com.olcayaras.vidster.ui.screens.editfigure.composables.TemplatesPicker
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Check
import compose.icons.feathericons.Edit2
import compose.icons.feathericons.Plus

private const val MAX_INITIAL_SCALE = 2f

/**
 * Calculates canvas state to center and fit a figure within the available space.
 */
private fun calculateFigureCenteredState(
    canvasSize: IntSize,
    compiledJoints: List<CompiledJoint>,
    padding: Float = 50f
): CanvasState? {
    if (canvasSize == IntSize.Zero || compiledJoints.isEmpty()) return null

    // Calculate bounding box of all joints
    var minX = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE
    var minY = Float.MAX_VALUE
    var maxY = Float.MIN_VALUE

    compiledJoints.forEach { joint ->
        minX = minOf(minX, joint.startX, joint.endX)
        maxX = maxOf(maxX, joint.startX, joint.endX)
        minY = minOf(minY, joint.startY, joint.endY)
        maxY = maxOf(maxY, joint.startY, joint.endY)
    }

    // Add padding
    minX -= padding
    maxX += padding
    minY -= padding
    maxY += padding

    val figureWidth = maxX - minX
    val figureHeight = maxY - minY

    if (figureWidth <= 0 || figureHeight <= 0) return null

    // Calculate scale to fit
    val scaleX = canvasSize.width / figureWidth
    val scaleY = canvasSize.height / figureHeight
    val scale = minOf(scaleX, scaleY, MAX_INITIAL_SCALE)

    // Calculate offset to center
    val figureCenterX = (minX + maxX) / 2
    val figureCenterY = (minY + maxY) / 2
    val canvasCenterX = canvasSize.width / 2f
    val canvasCenterY = canvasSize.height / 2f

    return CanvasState(
        offsetX = canvasCenterX - figureCenterX * scale,
        offsetY = canvasCenterY - figureCenterY * scale,
        scale = scale
    )
}

/**
 * Screen for editing a figure or creating a new one.
 * Layout: Canvas on left, properties panel on right, toolbar at top.
 */
@Composable
fun EditFigureScreen(
    model: EditFigureState,
    take: (EditFigureEvent) -> Unit
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var hasInitializedOffset by remember { mutableStateOf(false) }

    // Center figure when canvas size is available
    LaunchedEffect(canvasSize) {
        if (canvasSize != IntSize.Zero && !hasInitializedOffset) {
            calculateFigureCenteredState(canvasSize, model.compiledJoints)?.let {
                take(EditFigureEvent.UpdateCanvasState(it))
            }
            hasInitializedOffset = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top toolbar
        EditFigureToolbar(
            figureName = model.figure.name,
            onNameChange = { take(EditFigureEvent.UpdateFigureName(it)) },
            onCancel = { take(EditFigureEvent.Cancel) },
            onSave = { take(EditFigureEvent.Save) }
        )

        // Main content: Canvas + Properties Panel
        Row(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            // Left: Canvas area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color.White)
                    .onGloballyPositioned { canvasSize = it.size }
            ) {
                FigureEditorCanvas(
                    modifier = Modifier.fillMaxSize(),
                    figure = model.figure,
                    selectedJointId = model.selectedJointId,
                    canvasState = model.canvasState,
                    figureModificationCount = model.figureModificationCount,
                    addJointMode = model.addJointMode,
                    newJointType = model.newJointType,
                    onCanvasStateChange = { take(EditFigureEvent.UpdateCanvasState(it)) },
                    onJointSelected = { jointId -> take(EditFigureEvent.SelectJoint(jointId)) },
                    onJointRotated = { joint, angle -> take(EditFigureEvent.RotateJoint(joint, angle)) },
                    onJointResized = { joint, length ->
                        take(EditFigureEvent.UpdateJointLengthForJoint(joint.id, length))
                    },
                    onAddJointRequested = { joint, type ->
                        take(EditFigureEvent.AddChildJoint(joint.id, type))
                    },
                    onFigureMoved = { x, y -> take(EditFigureEvent.MoveFigure(x, y)) }
                )

                AddJointOverlay(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    addJointMode = model.addJointMode,
                    newJointType = model.newJointType,
                    onToggleAddJointMode = { take(EditFigureEvent.ToggleAddJointMode) },
                    onNewJointTypeSelected = { take(EditFigureEvent.UpdateNewJointType(it)) }
                )
            }

            // Right: Properties panel
            Surface(
                modifier = Modifier
                    .width(280.dp)
                    .fillMaxHeight(),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Joint properties
                    JointPropertiesPanel(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        selectedJoint = model.selectedJoint,
                        onUpdateId = { take(EditFigureEvent.UpdateJointId(it)) },
                        onUpdateLength = { take(EditFigureEvent.UpdateJointLength(it)) },
                        onUpdateAngle = { take(EditFigureEvent.UpdateJointAngle(it)) },
                        onUpdateType = { take(EditFigureEvent.UpdateJointType(it)) },
                        onDelete = { take(EditFigureEvent.DeleteSelectedJoint) },
                        canDelete = model.canDeleteSelectedJoint
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Templates
                    TemplatesPicker(
                        expanded = model.templatesExpanded,
                        onToggleExpanded = { take(EditFigureEvent.ToggleTemplatesExpanded) },
                        onTemplateSelected = { take(EditFigureEvent.ApplyTemplate(it)) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditFigureToolbar(
    figureName: String,
    onNameChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Cancel button
            TextButton(onClick = onCancel) {
                Icon(
                    FeatherIcons.ArrowLeft,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Cancel")
            }

            // Figure name input
            OutlinedTextField(
                value = figureName,
                onValueChange = onNameChange,
                modifier = Modifier.width(220.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium,
                trailingIcon = {
                    Icon(
                        FeatherIcons.Edit2,
                        contentDescription = "Edit name",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            // Save button
            FilledTonalButton(onClick = onSave) {
                Icon(
                    FeatherIcons.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Save")
            }
        }
    }
}

@Composable
private fun AddJointOverlay(
    modifier: Modifier = Modifier,
    addJointMode: Boolean,
    newJointType: SegmentType,
    onToggleAddJointMode: () -> Unit,
    onNewJointTypeSelected: (SegmentType) -> Unit
) {
    Surface(
        modifier = modifier.widthIn(min = 200.dp, max = 280.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
        tonalElevation = 3.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = addJointMode,
                onClick = onToggleAddJointMode,
                label = { Text(if (addJointMode) "Add Joint: On" else "Add Joint") },
                leadingIcon = {
                    Icon(
                        FeatherIcons.Plus,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )

            SegmentTypePicker(
                label = "New joint type",
                currentType = newJointType,
                onTypeSelected = onNewJointTypeSelected
            )
        }
    }
}
