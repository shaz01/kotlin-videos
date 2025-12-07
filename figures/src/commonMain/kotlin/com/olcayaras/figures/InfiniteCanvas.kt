package com.olcayaras.figures

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import kotlin.math.atan2
import kotlin.math.sqrt

private const val JOINT_HIT_RADIUS = 48f
private const val SEGMENT_HIT_DISTANCE = 20f

/**
 * An infinite canvas for editing stick figures with pan/zoom support.
 */
@Composable
fun InfiniteCanvas(
    modifier: Modifier = Modifier,
    figures: List<Figure>,
    canvasState: CanvasState,
    figureModificationCount: Long = 0L,
    rotationAllowed: Boolean = true,
    onCanvasStateChange: (CanvasState) -> Unit = {},
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
    val currentOnCanvasStateChange by rememberUpdatedState(onCanvasStateChange)
    val currentOnJointAngleChanged by rememberUpdatedState(onJointAngleChanged)
    val currentOnFigureMoved by rememberUpdatedState(onFigureMoved)

    Canvas(
        modifier = modifier
            .background(Color.White)
            // Scroll wheel zoom (desktop)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Scroll) {
                            val scrollDelta = event.changes.first().scrollDelta
                            val zoomFactor = if (scrollDelta.y > 0) 0.9f else 1.1f
                            val position = event.changes.first().position
                            currentOnCanvasStateChange(
                                currentCanvasState.zoom(zoomFactor, position.x, position.y)
                            )
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
            }
            // Touch/click gestures
            .pointerInput(Unit) {
                awaitEachGesture {
                    val firstDown = awaitFirstDown(requireUnconsumed = false)

                    // Hit test to determine drag target
                    var dragTarget = findDragTarget(
                        position = firstDown.position,
                        canvasState = currentCanvasState,
                        compiledJoints = compiledJointsForDrawing,
                        rotationAllowed = rotationAllowed
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
                                onCanvasStateChange = currentOnCanvasStateChange,
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

// =============================================================================
// Drag Target
// =============================================================================

/** What the user is dragging. */
private sealed class DragTarget {
    data class JointRotation(val compiledJoint: CompiledJoint) : DragTarget()
    data class FigureMove(val figure: Figure) : DragTarget()
}

/** Hit-tests the touch position to determine what to drag. Joint tips have priority over segments. */
private fun findDragTarget(
    position: Offset,
    canvasState: CanvasState,
    compiledJoints: List<CompiledJoint>,
    rotationAllowed: Boolean
): DragTarget? {
    val (canvasX, canvasY) = canvasState.screenToCanvas(position.x, position.y)

    // Joint tips (red dots) have priority
    if (rotationAllowed) {
        findHitJoint(compiledJoints, canvasX, canvasY, canvasState.scale)?.let {
            return DragTarget.JointRotation(it)
        }
    }

    // Then check segments
    findHitSegment(compiledJoints, canvasX, canvasY, canvasState.scale)?.let {
        return DragTarget.FigureMove(it.figure)
    }

    return null
}

// =============================================================================
// Gesture Handlers
// =============================================================================

/** Handles two-finger gestures: pinch to zoom, drag to pan. */
private fun handleMultiTouch(
    event: PointerEvent,
    canvasState: CanvasState,
    onCanvasStateChange: (CanvasState) -> Unit
) {
    val zoom = event.calculateZoom()
    val pan = event.calculatePan()
    val centroid = event.calculateCentroid()

    if (zoom != 1f || pan != Offset.Zero) {
        var newState = canvasState.pan(pan.x, pan.y)
        if (zoom != 1f) {
            newState = newState.zoom(zoom, centroid.x, centroid.y)
        }
        onCanvasStateChange(newState)
    }

    event.changes.forEach { it.consume() }
}

/** Handles single-finger drag: rotates joints, moves figures, or pans canvas. */
private fun handleSingleTouch(
    change: PointerInputChange,
    dragTarget: DragTarget?,
    canvasState: CanvasState,
    onCanvasStateChange: (CanvasState) -> Unit,
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

        null -> {
            // No target - pan the canvas (single-finger pan for desktop & mobile)
            val delta = change.position - change.previousPosition
            onCanvasStateChange(canvasState.pan(delta.x, delta.y))
            change.consume()
        }
    }
}

// =============================================================================
// Drawing
// =============================================================================

/** Draws a joint as a line segment or circle with a joint dot at the start. */
private fun DrawScope.drawCompiledJoint(
    compiled: CompiledJoint,
    color: Color,
    thickness: Float
) {
    if (compiled.joint.length <= 0f) return

    val start = Offset(compiled.startX, compiled.startY)
    val end = Offset(compiled.endX, compiled.endY)

    when (compiled.joint.type) {
        SegmentType.Line -> {
            drawLine(color = color, start = start, end = end, strokeWidth = thickness, cap = StrokeCap.Round)
        }
        SegmentType.Circle -> {
            val centerX = (compiled.startX + compiled.endX) / 2
            val centerY = (compiled.startY + compiled.endY) / 2
            val radius = compiled.joint.length / 2
            drawCircle(
                color = color,
                radius = radius,
                center = Offset(centerX, centerY),
                style = Stroke(width = thickness)
            )
        }
    }

    // Joint dot at start
    drawCircle(color = color, radius = thickness, center = start)
}

// =============================================================================
// Hit Testing
// =============================================================================

/** Finds the closest joint tip (red dot) within hit radius, or null if none. */
private fun findHitJoint(
    compiledJoints: List<CompiledJoint>,
    canvasX: Float,
    canvasY: Float,
    scale: Float
): CompiledJoint? {
    val hitRadius = JOINT_HIT_RADIUS / scale

    return compiledJoints
        .filter { it.joint.length > 0f }
        .minByOrNull { distanceTo(canvasX, canvasY, it.endX, it.endY) }
        ?.takeIf { distanceTo(canvasX, canvasY, it.endX, it.endY) <= hitRadius }
}

/** Finds the closest segment (line or circle) within hit distance, or null if none. */
private fun findHitSegment(
    compiledJoints: List<CompiledJoint>,
    canvasX: Float,
    canvasY: Float,
    scale: Float
): CompiledJoint? {
    val hitDistance = SEGMENT_HIT_DISTANCE / scale

    return compiledJoints
        .asSequence()
        .filter { it.joint.length > 0f }
        .associateWith { compiled ->
            when (compiled.joint.type) {
                SegmentType.Line -> {
                    pointToSegmentDistance(canvasX, canvasY, compiled.startX, compiled.startY, compiled.endX, compiled.endY)
                }
                SegmentType.Circle -> {
                    val centerX = (compiled.startX + compiled.endX) / 2
                    val centerY = (compiled.startY + compiled.endY) / 2
                    val radius = compiled.joint.length / 2
                    pointToCircleDistance(canvasX, canvasY, centerX, centerY, radius)
                }
            }
        }
        .minByOrNull { it.value }
        ?.takeIf { it.value <= hitDistance }
        ?.key
}

/** Euclidean distance between two points. */
private fun distanceTo(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val dx = x2 - x1
    val dy = y2 - y1
    return sqrt(dx * dx + dy * dy)
}

/** Shortest distance from point (px, py) to line segment (x1,y1)-(x2,y2). */
private fun pointToSegmentDistance(
    px: Float, py: Float,
    x1: Float, y1: Float,
    x2: Float, y2: Float
): Float {
    val dx = x2 - x1
    val dy = y2 - y1
    val lengthSquared = dx * dx + dy * dy

    if (lengthSquared == 0f) return distanceTo(px, py, x1, y1)

    val t = (((px - x1) * dx + (py - y1) * dy) / lengthSquared).coerceIn(0f, 1f)
    return distanceTo(px, py, x1 + t * dx, y1 + t * dy)
}

/** Distance from point (px, py) to the edge of a circle. */
private fun pointToCircleDistance(
    px: Float, py: Float,
    centerX: Float, centerY: Float,
    radius: Float
): Float {
    val distToCenter = distanceTo(px, py, centerX, centerY)
    return kotlin.math.abs(distToCenter - radius)
}
