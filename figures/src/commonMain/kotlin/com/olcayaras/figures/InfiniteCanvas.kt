package com.olcayaras.figures

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import kotlin.math.atan2

private const val VIEWPORT_EDGE_HIT_DISTANCE = 20f

/**
 * An infinite canvas for editing stick figures with pan/zoom support.
 * Shows a draggable viewport rectangle representing the camera view area.
 */
@Composable
fun InfiniteCanvas(
    modifier: Modifier = Modifier,
    figures: List<Figure>,
    canvasState: CanvasState,
    viewport: Viewport = Viewport(),
    screenSize: IntSize = IntSize(1920, 1080),
    figureModificationCount: Long = 0L,
    rotationAllowed: Boolean = true,
    onCanvasStateChange: (CanvasState) -> Unit = {},
    onViewportChanged: (Viewport) -> Unit = {},
    onJointAngleChanged: (figure: Figure, joint: Joint, newAngle: Float) -> Unit = { _, _, _ -> },
    onFigureMoved: (figure: Figure, newX: Float, newY: Float) -> Unit = { _, _, _ -> }
) {
    // Store compiled joints in state - this triggers redraw when updated
    var compiledJointsForDrawing by remember {
        mutableStateOf(figures.flatMap { it.compileForEditing() })
    }

    // Update compiled joints when figures are modified
    LaunchedEffect(figures, figureModificationCount) {
        compiledJointsForDrawing = figures.flatMap { it.compileForEditing() }
    }

    // Use rememberUpdatedState to always read latest values inside gesture handler
    val currentCanvasState by rememberUpdatedState(canvasState)
    val currentViewport by rememberUpdatedState(viewport)
    val currentOnCanvasStateChange by rememberUpdatedState(onCanvasStateChange)
    val currentOnViewportChanged by rememberUpdatedState(onViewportChanged)
    val currentOnJointAngleChanged by rememberUpdatedState(onJointAngleChanged)
    val currentOnFigureMoved by rememberUpdatedState(onFigureMoved)

    val viewportRect by rememberUpdatedState(Rect(offset = viewport.topLeft, size = screenSize.toSize()))

    Canvas(
        modifier = modifier
            .background(Color.White)
            .scrollWheelZoom(
                getCanvasState = { currentCanvasState },
                onCanvasStateChange = currentOnCanvasStateChange
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    val firstDown = awaitFirstDown(requireUnconsumed = false)

                    // Hit test to determine drag target
                    var dragTarget = findDragTarget(
                        position = firstDown.position,
                        canvasState = currentCanvasState,
                        compiledJoints = compiledJointsForDrawing,
                        rotationAllowed = rotationAllowed,
                        viewportRect = viewportRect
                    )

                    // Process pointer events until all fingers are lifted
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.none { it.pressed }) break

                        val pointerCount = event.changes.count { it.pressed }

                        if (pointerCount >= 2) {
                            dragTarget = null
                            handleMultiTouch(event, currentCanvasState, currentOnCanvasStateChange)
                        } else if (pointerCount == 1) {
                            handleSingleTouch(
                                change = event.changes.first(),
                                dragTarget = dragTarget,
                                canvasState = currentCanvasState,
                                viewport = currentViewport,
                                onCanvasStateChange = currentOnCanvasStateChange,
                                onViewportChanged = currentOnViewportChanged,
                                onJointAngleChanged = currentOnJointAngleChanged,
                                onFigureMoved = currentOnFigureMoved
                            )
                        }
                    }
                }
            }
    ) {
        withTransform({
            translate(canvasState.offsetX, canvasState.offsetY)
            scale(canvasState.scale, canvasState.scale, Offset.Zero)
        }) {
            // Draw viewport rectangle (camera frame)
            drawViewportRect(viewportRect)

            // Draw figures
            compiledJointsForDrawing.forEach { compiled ->
                drawCompiledJoint(compiled, Color.Black, 4f)
            }

            // Draw rotation handles (red dots)
            if (rotationAllowed) {
                compiledJointsForDrawing
                    .filter { it.joint.length > 0f }
                    .forEach { compiled ->
                        drawCircle(
                            color = Color.Red,
                            radius = 8f,
                            center = Offset(compiled.endX, compiled.endY)
                        )
                    }
            }
        }
    }
}

/** Draws the viewport rectangle showing the camera frame area. */
private fun DrawScope.drawViewportRect(rect: Rect) {
    val strokeWidth = 2f
    val dashPattern = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)

    // Draw dashed rectangle border
    drawRect(
        color = Color.Blue.copy(alpha = 0.6f),
        topLeft = rect.topLeft,
        size = rect.size,
        style = Stroke(width = strokeWidth, pathEffect = dashPattern)
    )

    // Draw corner handles for visual feedback
    val handleSize = 12f
    val handleColor = Color.Blue.copy(alpha = 0.8f)
    drawCircle(color = handleColor, radius = handleSize, center = Offset(rect.left, rect.top))
    drawCircle(color = handleColor, radius = handleSize, center = Offset(rect.right, rect.top))
    drawCircle(color = handleColor, radius = handleSize, center = Offset(rect.left, rect.bottom))
    drawCircle(color = handleColor, radius = handleSize, center = Offset(rect.right, rect.bottom))
}

// =============================================================================
// Drag Target
// =============================================================================

/** What the user is dragging. */
private sealed class DragTarget {
    data class JointRotation(val compiledJoint: CompiledJoint) : DragTarget()
    data class FigureMove(val figure: Figure) : DragTarget()
    data object ViewportMove : DragTarget()
}

/** Hit-tests the touch position to determine what to drag. Joint tips have priority, then viewport edges, then segments. */
private fun findDragTarget(
    position: Offset,
    canvasState: CanvasState,
    compiledJoints: List<CompiledJoint>,
    rotationAllowed: Boolean,
    viewportRect: Rect
): DragTarget? {
    val (canvasX, canvasY) = canvasState.screenToCanvas(position.x, position.y)

    // Joint tips (red dots) have priority
    if (rotationAllowed) {
        findHitJoint(compiledJoints, canvasX, canvasY, canvasState.scale)?.let {
            return DragTarget.JointRotation(it)
        }
    }

    // Then check viewport edges (only edges, not inside)
    if (hitTestViewportEdge(canvasX, canvasY, viewportRect, canvasState.scale)) {
        return DragTarget.ViewportMove
    }

    // Then check segments
    findHitSegment(compiledJoints, canvasX, canvasY, canvasState.scale)?.let {
        return DragTarget.FigureMove(it.figure)
    }

    return null
}

/** Checks if the point is near the edge of the viewport rectangle but not deep inside. */
private fun hitTestViewportEdge(
    canvasX: Float,
    canvasY: Float,
    viewportRect: Rect,
    scale: Float
): Boolean {
    val threshold = VIEWPORT_EDGE_HIT_DISTANCE / scale

    // Check if point is inside the expanded rect (rect + threshold on all sides)
    val expandedRect = Rect(
        left = viewportRect.left - threshold,
        top = viewportRect.top - threshold,
        right = viewportRect.right + threshold,
        bottom = viewportRect.bottom + threshold
    )

    if (!expandedRect.contains(Offset(canvasX, canvasY))) {
        return false // Point is outside even the expanded rect
    }

    // Check if point is inside the shrunk rect (rect - threshold on all sides)
    val shrunkRect = Rect(
        left = viewportRect.left + threshold,
        top = viewportRect.top + threshold,
        right = viewportRect.right - threshold,
        bottom = viewportRect.bottom - threshold
    )

    // If inside expanded but outside shrunk, it's on the edge
    return !shrunkRect.contains(Offset(canvasX, canvasY))
}

/** Handles single-finger drag: rotates joints, moves figures, moves viewport, or pans canvas. */
private fun handleSingleTouch(
    change: PointerInputChange,
    dragTarget: DragTarget?,
    canvasState: CanvasState,
    viewport: Viewport,
    onCanvasStateChange: (CanvasState) -> Unit,
    onViewportChanged: (Viewport) -> Unit,
    onJointAngleChanged: (Figure, Joint, Float) -> Unit,
    onFigureMoved: (Figure, Float, Float) -> Unit
) {
    if (!change.positionChanged()) return

    val (canvasX, canvasY) = canvasState.screenToCanvas(change.position.x, change.position.y)
    val (prevCanvasX, prevCanvasY) = canvasState.screenToCanvas(change.previousPosition.x, change.previousPosition.y)

    when (dragTarget) {
        is DragTarget.JointRotation -> {
            val compiled = dragTarget.compiledJoint
            val newWorldAngle = atan2(canvasY - compiled.startY, canvasX - compiled.startX)
            val newRelativeAngle = newWorldAngle - compiled.parentWorldAngle
            onJointAngleChanged(compiled.figure, compiled.joint, newRelativeAngle)
            change.consume()
        }

        is DragTarget.FigureMove -> {
            val figure = dragTarget.figure
            onFigureMoved(figure, figure.x + (canvasX - prevCanvasX), figure.y + (canvasY - prevCanvasY))
            change.consume()
        }

        is DragTarget.ViewportMove -> {
            // Dragging viewport moves the camera - offset is inverted
            val deltaX = canvasX - prevCanvasX
            val deltaY = canvasY - prevCanvasY
            val newViewport = viewport.copy(
                leftX = viewport.leftX + deltaX,
                topY = viewport.topY + deltaY
            )
            onViewportChanged(newViewport)
            change.consume()
        }

        null -> {
            // No target - pan the canvas (single-finger pan for desktop & mobile)
            val delta = change.position - change.previousPosition
            onCanvasStateChange(canvasState.pan(delta.x, delta.y))
            change.consume()
        }
    }
}

