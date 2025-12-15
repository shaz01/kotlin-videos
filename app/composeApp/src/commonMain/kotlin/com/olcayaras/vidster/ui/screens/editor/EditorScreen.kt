package com.olcayaras.vidster.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.olcayaras.figures.*
import com.olcayaras.vidster.ui.screens.editor.composables.EditorSheetContainer
import com.olcayaras.vidster.ui.screens.editor.composables.EditorTimelineColumn
import com.olcayaras.vidster.ui.screens.editor.composables.EditorToolbar
import com.olcayaras.vidster.ui.screens.editor.composables.OnionSkinModePicker
import compose.icons.FeatherIcons
import compose.icons.feathericons.Crosshair
import compose.icons.feathericons.Edit2
import compose.icons.feathericons.Play
import compose.icons.feathericons.Plus
import compose.icons.feathericons.RotateCcw
import compose.icons.feathericons.RotateCw
import compose.icons.feathericons.User
import io.github.aakira.napier.Napier
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.roundToInt

/**
 * Calculates the centered offset for the canvas based on viewport and available space.
 * @return CanvasState containing the calculated offset and scale, or null if the canvas size is zero
 */
private fun calculateCenteredOffset(
    canvasSize: IntSize,
    density: Density,
    left: Int = 0,
    top: Int = 0,
    right: Int = canvasSize.width,
    bottom: Int = canvasSize.height,
    viewport: Viewport,
    viewportSize: IntSize,
): CanvasState? {
    if (canvasSize == IntSize.Zero) return null

    val eightDpInPx = with(density) { 8.dp.toPx() }

    val safeLeft = left + eightDpInPx
    val safeTop = top + eightDpInPx
    val safeRight = right - eightDpInPx
    val safeBottom = bottom - eightDpInPx

    val safeWidth = safeRight - safeLeft
    val safeHeight = safeBottom - safeTop
    val viewportWidth = viewportSize.width
    val viewportHeight = viewportSize.height
    if (safeWidth <= 0 || safeHeight <= 0) return null
    if (viewportWidth <= 0 || viewportHeight <= 0) return null

    val scale = minOf(safeWidth / viewportWidth, safeHeight / viewportHeight)

    val equalizingPaddingX = (safeWidth - viewportWidth * scale) / 2
    val equalizingPaddingY = (safeHeight - viewportHeight * scale) / 2

    return CanvasState(
        offsetX = safeLeft + equalizingPaddingX - viewport.leftX * scale,
        offsetY = safeTop + equalizingPaddingY - viewport.topY * scale,
        scale = scale
    )
}

/**
 * Main editor screen composable that displays the animation editor interface.
 * Contains an infinite canvas for figure manipulation, timeline, toolbar, and properties panel.
 *
 * @param model The current state of the editor
 * @param take Callback to handle editor events
 */
@Composable
fun EditorScreen(
    model: EditorState,
    take: (EditorEvent) -> Unit
) {
    val density = LocalDensity.current
    // Measurement state for overlay sizes
    var timelineWidth by remember { mutableIntStateOf(0) }
    var timelineTargetWidth by remember { mutableStateOf(160.dp) }
    var toolbarHeight by remember { mutableIntStateOf(0) }

    var propertiesPanelLeft by remember { mutableIntStateOf(0) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    fun resetViewportToCenter() {
        calculateCenteredOffset(
            canvasSize = canvasSize,
            density = density,
            left = timelineWidth,
            top = toolbarHeight,
            right = propertiesPanelLeft,
            viewport = model.selectedFrame.viewport,
            viewportSize = model.viewportSize
        )?.let {
            take(EditorEvent.UpdateCanvasState(it))
        } ?: Napier.e { "Failed to calculate centered offset" }
    }

    // Set initial offset when measurements are available
    var hasInitializedOffset by remember { mutableStateOf(false) }
    LaunchedEffect(timelineWidth, toolbarHeight, propertiesPanelLeft, canvasSize) {
        if (canvasSize != IntSize.Zero && !hasInitializedOffset) {
            hasInitializedOffset = true
            resetViewportToCenter()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .onGloballyPositioned { canvasSize = it.size }
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    val isCtrlOrCmd = keyEvent.isCtrlPressed || keyEvent.isMetaPressed
                    when {
                        isCtrlOrCmd && keyEvent.isShiftPressed && keyEvent.key == Key.Z -> {
                            take(EditorEvent.Redo)
                            true
                        }
                        isCtrlOrCmd && keyEvent.key == Key.Z -> {
                            take(EditorEvent.Undo)
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .focusable()
    ) {
        // Convert onion skin frames to layers with colors
        val onionSkinLayers = remember(model.onionSkinFrames) {
            model.onionSkinFrames.map { frame ->
                OnionSkinLayer(
                    compiledJoints = frame.compiledJoints,
                    alpha = frame.alpha,
                    color = if (frame.isPrevious) Color.Red else Color.Blue
                )
            }
        }

        // Canvas layer (background) - Interactive infinite canvas
        InfiniteCanvas(
            modifier = Modifier.fillMaxSize(),
            figures = model.selectedFigures,
            canvasState = model.canvasState,
            viewport = model.selectedFrame.viewport,
            viewportSize = model.viewportSize,
            figureModificationCount = model.figureModificationCount,
            rotationAllowed = true,
            onionSkinLayers = onionSkinLayers,
            onCanvasStateChange = { take(EditorEvent.UpdateCanvasState(it)) },
            onViewportDragStart = { take(EditorEvent.BeginViewportDrag) },
            onViewportChanged = { take(EditorEvent.UpdateViewport(it)) },
            onJointDragStart = { figure, joint ->
                take(EditorEvent.BeginJointDrag(figure, joint))
            },
            onJointAngleChanged = { figure, joint, angle ->
                take(EditorEvent.UpdateJointAngle(figure, joint, angle))
            },
            onFigureDragStart = { figure ->
                take(EditorEvent.BeginFigureMove(figure))
            },
            onFigureMoved = { figure, newX, newY ->
                take(EditorEvent.MoveFigure(figure, newX, newY))
            }
        )

        // UI overlay layer
        Row(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(100f)
        ) {
            // Left: Timeline with resize handle
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .onGloballyPositioned {
                        timelineWidth = it.boundsInParent().right.roundToInt()
                    }
            ) {
                EditorTimelineColumn(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(timelineTargetWidth),
                    frames = model.segmentFrames,
                    onClick = { segmentFrame ->
                        val index = model.segmentFrames.indexOf(segmentFrame)
                        if (index >= 0) {
                            take(EditorEvent.SelectFrame(index))
                        }
                    },
                    selectedFrame = model.selectedSegmentFrame,
                    viewportSize = model.viewportSize
                )

                // Resize handle
                val resizeInteractionSource = remember { MutableInteractionSource() }
                val isHovered by resizeInteractionSource.collectIsHoveredAsState()
                val isDragging = remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .align(Alignment.CenterEnd)
                        .width(8.dp)
                        .hoverable(resizeInteractionSource)
                        .pointerHoverIcon(PointerIcon.Hand)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { isDragging.value = true },
                                onDragEnd = { isDragging.value = false },
                                onDragCancel = { isDragging.value = false }
                            ) { change, dragAmount ->
                                change.consume()
                                val newWidth = timelineTargetWidth + with(density) { dragAmount.x.toDp() }
                                timelineTargetWidth = newWidth.coerceIn(100.dp, 400.dp)
                            }
                        }
                        .background(
                            if (isHovered || isDragging.value)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            else
                                Color.Transparent
                        )
                )
            }

            // Center: Main content area with toolbar
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Toolbar at top
                EditorToolbar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned {
                            toolbarHeight = it.boundsInParent().bottom.roundToInt()
                        }
                ) {
                    FilledTonalIconButton(onClick = { take(EditorEvent.PlayAnimation) }) {
                        Icon(FeatherIcons.Play, contentDescription = "Play Animation")
                    }
                    IconButton(onClick = { take(EditorEvent.AddFrame) }) {
                        Icon(FeatherIcons.Plus, contentDescription = "Add Frame")
                    }
                    IconButton(onClick = ::resetViewportToCenter) {
                        Icon(FeatherIcons.Crosshair, contentDescription = "Reset View")
                    }
                    // Onion skin mode picker
                    OnionSkinModePicker(
                        selectedMode = model.onionSkinMode,
                        onModeSelected = { take(EditorEvent.SetOnionSkinMode(it)) }
                    )
                    // Undo/Redo buttons
                    IconButton(
                        onClick = { take(EditorEvent.Undo) },
                        enabled = model.canUndo
                    ) {
                        Icon(FeatherIcons.RotateCcw, contentDescription = "Undo")
                    }
                    IconButton(
                        onClick = { take(EditorEvent.Redo) },
                        enabled = model.canRedo
                    ) {
                        Icon(FeatherIcons.RotateCw, contentDescription = "Redo")
                    }
                }
            }

            // Right: Properties panel
            EditorSheetContainer(
                modifier = Modifier
                    .width(200.dp)
                    .fillMaxHeight()
                    .onGloballyPositioned {
                        propertiesPanelLeft = it.boundsInParent().left.roundToInt()
                    }
            ) {
                Text("Figures", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                // List figures in current frame
                model.selectedFigures.forEachIndexed { index, figure ->
                    OutlinedCard(
                        onClick = { take(EditorEvent.EditFigure(index)) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                FeatherIcons.User,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                figure.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                FeatherIcons.Edit2,
                                contentDescription = "Edit",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Add new figure button
                OutlinedButton(
                    onClick = { take(EditorEvent.AddNewFigure) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(FeatherIcons.Plus, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Figure")
                }
            }
        }
    }
}

@Preview(widthDp = 900, heightDp = 360, showBackground = true)
@Composable
fun EditorScreenPreview() {
    val frames = listOf(
        FigureFrame(
            figures = listOf(getMockFigure(x = 400f, y = 200f)),
            viewport = Viewport()
        )
    )
    EditorScreen(
        model = EditorState(
            frames = frames,
            selectedFrameIndex = 0,
            canvasState = CanvasState(),
            viewportSize = IntSize(1920, 1080)
        ),
        take = {}
    )
}
