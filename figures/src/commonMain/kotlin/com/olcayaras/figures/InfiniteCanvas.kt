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
import androidx.compose.ui.graphics.Path
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

    val worldAngle = compiled.parentWorldAngle + compiled.joint.angle

    when (compiled.joint.type) {
        SegmentType.Line -> {
            drawLine(color = color, start = start, end = end, strokeWidth = thickness, cap = StrokeCap.Round)
        }
        SegmentType.Circle -> {
            drawCircle(
                color = color,
                radius = compiled.radius,
                center = Offset(compiled.centerX, compiled.centerY),
                style = Stroke(width = thickness)
            )
        }
        SegmentType.FilledCircle -> {
            drawCircle(
                color = color,
                radius = compiled.radius,
                center = Offset(compiled.centerX, compiled.centerY)
            )
        }
        SegmentType.Rectangle -> {
            // Calculate perpendicular direction for rectangle height
            val halfHeight = compiled.joint.length * 0.25f
            val perpAngle = worldAngle + (Math.PI / 2).toFloat()
            val perpX = halfHeight * kotlin.math.cos(perpAngle)
            val perpY = halfHeight * kotlin.math.sin(perpAngle)

            // Four corners of rectangle
            val path = Path().apply {
                moveTo(compiled.startX - perpX, compiled.startY - perpY)
                lineTo(compiled.startX + perpX, compiled.startY + perpY)
                lineTo(compiled.endX + perpX, compiled.endY + perpY)
                lineTo(compiled.endX - perpX, compiled.endY - perpY)
                close()
            }
            drawPath(path = path, color = color)
        }
        is SegmentType.Ellipse -> {
            // Draw ellipse using path with calculated points
            val majorRadius = compiled.joint.length / 2
            val minorRadius = majorRadius * compiled.joint.type.widthRatio

            // Use path to draw rotated ellipse (major axis along segment direction)
            val path = Path()
            val steps = 32
            for (i in 0..steps) {
                val t = (i.toFloat() / steps) * 2 * Math.PI.toFloat()
                // Ellipse point in local coords (major axis along X, minor along Y)
                val localX = majorRadius * kotlin.math.cos(t)
                val localY = minorRadius * kotlin.math.sin(t)
                // Rotate to world coords (X axis aligns with worldAngle)
                val cos = kotlin.math.cos(worldAngle)
                val sin = kotlin.math.sin(worldAngle)
                val worldX = compiled.centerX + localX * cos - localY * sin
                val worldY = compiled.centerY + localX * sin + localY * cos
                if (i == 0) path.moveTo(worldX, worldY) else path.lineTo(worldX, worldY)
            }
            path.close()
            drawPath(path = path, color = color, style = Stroke(width = thickness))
        }
        is SegmentType.Arc -> {
            // Draw arc from start point, curving around center
            val radius = compiled.joint.length / 2
            val sweepAngle = compiled.joint.type.sweepAngle
            // Arc starts from the direction pointing to start (opposite of worldAngle)
            val arcStartAngle = worldAngle + Math.PI.toFloat()

            val path = Path()
            val steps = 24
            for (i in 0..steps) {
                val t = arcStartAngle + (i.toFloat() / steps) * sweepAngle
                val x = compiled.centerX + radius * kotlin.math.cos(t)
                val y = compiled.centerY + radius * kotlin.math.sin(t)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path = path, color = color, style = Stroke(width = thickness))
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

/** Finds the closest segment within hit distance, or null if none. */
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
                SegmentType.Circle, is SegmentType.Arc -> {
                    // Arc uses same hit test as circle (distance to the arc's circle edge)
                    pointToCircleDistance(canvasX, canvasY, compiled.centerX, compiled.centerY, compiled.radius)
                }
                SegmentType.FilledCircle -> {
                    // For filled circle, hit if inside the circle
                    val distToCenter = distanceTo(canvasX, canvasY, compiled.centerX, compiled.centerY)
                    if (distToCenter <= compiled.radius) 0f else distToCenter - compiled.radius
                }
                SegmentType.Rectangle -> {
                    // Simplified: use distance to center, scaled by aspect ratio
                    pointToRectDistance(canvasX, canvasY, compiled)
                }
                is SegmentType.Ellipse -> {
                    // Simplified: use distance to the ellipse edge (approximate)
                    pointToEllipseDistance(canvasX, canvasY, compiled)
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

/** Distance from point to a rectangle (returns 0 if inside). */
private fun pointToRectDistance(px: Float, py: Float, compiled: CompiledJoint): Float {
    val worldAngle = compiled.parentWorldAngle + compiled.joint.angle
    val width = compiled.joint.length
    val height = compiled.joint.length * 0.5f

    // Transform point to rectangle's local coordinate system
    val dx = px - compiled.centerX
    val dy = py - compiled.centerY
    val cos = kotlin.math.cos(-worldAngle)
    val sin = kotlin.math.sin(-worldAngle)
    val localX = dx * cos - dy * sin
    val localY = dx * sin + dy * cos

    // Check if inside rectangle
    val halfW = width / 2
    val halfH = height / 2
    if (kotlin.math.abs(localX) <= halfW && kotlin.math.abs(localY) <= halfH) {
        return 0f
    }

    // Distance to nearest edge
    val clampedX = localX.coerceIn(-halfW, halfW)
    val clampedY = localY.coerceIn(-halfH, halfH)
    return distanceTo(localX, localY, clampedX, clampedY)
}

/** Distance from point to ellipse edge (approximate). */
private fun pointToEllipseDistance(px: Float, py: Float, compiled: CompiledJoint): Float {
    val type = compiled.joint.type as SegmentType.Ellipse
    val worldAngle = compiled.parentWorldAngle + compiled.joint.angle
    val height = compiled.joint.length
    val width = compiled.joint.length * type.widthRatio

    // Transform point to ellipse's local coordinate system
    val dx = px - compiled.centerX
    val dy = py - compiled.centerY
    val cos = kotlin.math.cos(-worldAngle)
    val sin = kotlin.math.sin(-worldAngle)
    val localX = dx * cos - dy * sin
    val localY = dx * sin + dy * cos

    // Normalized distance (1.0 = on ellipse edge)
    val a = width / 2
    val b = height / 2
    val normalizedDist = sqrt((localX * localX) / (a * a) + (localY * localY) / (b * b))

    // Approximate distance to edge
    val avgRadius = (a + b) / 2
    return kotlin.math.abs(normalizedDist - 1f) * avgRadius
}
