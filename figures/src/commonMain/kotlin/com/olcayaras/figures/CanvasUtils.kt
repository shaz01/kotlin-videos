package com.olcayaras.figures

import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs
import kotlin.math.sqrt

// =============================================================================
// Constants
// =============================================================================

/**
 * The radius (in pixels) used to detect user interaction with a joint control point.
 * If the user's pointer is within this distance from a joint tip, it is considered a hit.
 */
const val JOINT_HIT_RADIUS = 48f

/**
 * The maximum distance (in pixels) from a line segment at which a user interaction
 * is considered to hit the segment. Used for selecting or interacting with segments.
 */
const val SEGMENT_HIT_DISTANCE = 20f

// =============================================================================
// Hit Testing Utilities
// =============================================================================

/** Euclidean distance between two points. */
fun distanceTo(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val dx = x2 - x1
    val dy = y2 - y1
    return sqrt(dx * dx + dy * dy)
}

/** Shortest distance from point (px, py) to line segment (x1,y1)-(x2,y2). */
fun pointToSegmentDistance(
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
fun pointToCircleDistance(
    px: Float, py: Float,
    centerX: Float, centerY: Float,
    radius: Float
): Float {
    val distToCenter = distanceTo(px, py, centerX, centerY)
    return abs(distToCenter - radius)
}

/** Transform point to local coordinate system of a shape. */
data class LocalPoint(val x: Float, val y: Float)

fun transformToLocalCoordinates(
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
fun pointToRectDistance(px: Float, py: Float, compiled: CompiledJoint): Float {
    val width = compiled.joint.length
    val height = compiled.joint.length * 0.5f

    val (localX, localY) = transformToLocalCoordinates(px, py, compiled)

    val halfW = width / 2
    val halfH = height / 2
    if (abs(localX) <= halfW && abs(localY) <= halfH) {
        return 0f
    }

    val clampedX = localX.coerceIn(-halfW, halfW)
    val clampedY = localY.coerceIn(-halfH, halfH)
    return distanceTo(localX, localY, clampedX, clampedY)
}

/** Distance from point to ellipse edge (approximate). */
fun pointToEllipseDistance(px: Float, py: Float, compiled: CompiledJoint): Float {
    val type = compiled.joint.type as SegmentType.Ellipse
    val height = compiled.joint.length
    val width = compiled.joint.length * type.widthRatio

    val (localX, localY) = transformToLocalCoordinates(px, py, compiled)

    val a = width / 2
    val b = height / 2
    val normalizedDist = sqrt((localX * localX) / (a * a) + (localY * localY) / (b * b))

    val avgRadius = (a + b) / 2
    return abs(normalizedDist - 1f) * avgRadius
}

/** Finds the closest joint tip (red dot) within hit radius, or null if none. */
fun findHitJoint(
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
fun findHitSegment(
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
                    pointToCircleDistance(canvasX, canvasY, compiled.centerX, compiled.centerY, compiled.radius)
                }
                SegmentType.FilledCircle -> {
                    val distToCenter = distanceTo(canvasX, canvasY, compiled.centerX, compiled.centerY)
                    if (distToCenter <= compiled.radius) 0f else distToCenter - compiled.radius
                }
                SegmentType.Rectangle -> {
                    pointToRectDistance(canvasX, canvasY, compiled)
                }
                is SegmentType.Ellipse -> {
                    pointToEllipseDistance(canvasX, canvasY, compiled)
                }
            }
        }
        .minByOrNull { it.value }
        ?.takeIf { it.value <= hitDistance }
        ?.key
}

// =============================================================================
// Gesture Handling
// =============================================================================

/** Modifier for scroll wheel zoom (desktop). */
fun Modifier.scrollWheelZoom(
    getCanvasState: () -> CanvasState,
    onCanvasStateChange: (CanvasState) -> Unit
): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            if (event.type == PointerEventType.Scroll) {
                val scrollDelta = event.changes.first().scrollDelta
                val zoomFactor = if (scrollDelta.y > 0) 0.9f else 1.1f
                val position = event.changes.first().position
                onCanvasStateChange(
                    getCanvasState().zoom(zoomFactor, position.x, position.y)
                )
                event.changes.forEach { it.consume() }
            }
        }
    }
}

/** Handles two-finger gestures: pinch to zoom, drag to pan. */
fun handleMultiTouch(
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

// =============================================================================
// Drawing
// =============================================================================

/** Draws a joint as a line segment or shape with a joint dot at the start. */
fun DrawScope.drawCompiledJoint(
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
