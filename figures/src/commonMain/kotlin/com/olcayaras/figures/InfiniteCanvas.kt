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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.unit.toSize
import kotlin.math.atan2
import kotlin.math.sqrt

private const val JOINT_HIT_RADIUS = 48f
private const val SEGMENT_HIT_DISTANCE = 20f
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
            drawFilledCircleShape(
                color = color,
                radius = compiled.radius,
                centerX = compiled.centerX,
                centerY = compiled.centerY
            )
        }
        SegmentType.Rectangle -> {
            drawRectangleShape(
                color = color,
                length = compiled.joint.length,
                angle = worldAngle,
                startX = compiled.startX,
                startY = compiled.startY,
                endX = compiled.endX,
                endY = compiled.endY
            )
        }
        is SegmentType.Ellipse -> {
            drawEllipseShape(
                color = color,
                thickness = thickness,
                length = compiled.joint.length,
                widthRatio = compiled.joint.type.widthRatio,
                angle = worldAngle,
                centerX = compiled.centerX,
                centerY = compiled.centerY
            )
        }
        is SegmentType.Arc -> {
            drawArcShape(
                color = color,
                thickness = thickness,
                length = compiled.joint.length,
                sweepAngle = compiled.joint.type.sweepAngle,
                angle = worldAngle,
                centerX = compiled.centerX,
                centerY = compiled.centerY
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

/** Transform point to local coordinate system of a shape. */
private data class LocalPoint(val x: Float, val y: Float)

private fun transformToLocalCoordinates(
    px: Float, py: Float,
    compiled: CompiledJoint
): LocalPoint {
    val worldAngle = compiled.parentWorldAngle + compiled.joint.angle
    val dx = px - compiled.centerX
    val dy = py - compiled.centerY
    val cos = kotlin.math.cos(-worldAngle)
    val sin = kotlin.math.sin(-worldAngle)
    val localX = dx * cos - dy * sin
    val localY = dx * sin + dy * cos
    return LocalPoint(localX, localY)
}

/** Distance from point to a rectangle (returns 0 if inside). */
private fun pointToRectDistance(px: Float, py: Float, compiled: CompiledJoint): Float {
    val width = compiled.joint.length
    val height = compiled.joint.length * 0.5f

    // Transform point to rectangle's local coordinate system
    val (localX, localY) = transformToLocalCoordinates(px, py, compiled)

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
    val height = compiled.joint.length
    val width = compiled.joint.length * type.widthRatio

    // Transform point to ellipse's local coordinate system
    val (localX, localY) = transformToLocalCoordinates(px, py, compiled)

    // Normalized distance (1.0 = on ellipse edge)
    val a = width / 2
    val b = height / 2
    val normalizedDist = sqrt((localX * localX) / (a * a) + (localY * localY) / (b * b))

    // Approximate distance to edge
    val avgRadius = (a + b) / 2
    return kotlin.math.abs(normalizedDist - 1f) * avgRadius
}
