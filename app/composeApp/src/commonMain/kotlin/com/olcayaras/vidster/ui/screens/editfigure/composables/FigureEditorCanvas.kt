package com.olcayaras.vidster.ui.screens.editfigure.composables

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
import com.olcayaras.figures.*
import kotlin.math.atan2
import kotlin.math.sqrt

private const val JOINT_HIT_RADIUS = 48f
private const val SEGMENT_HIT_DISTANCE = 20f

/**
 * Canvas for editing a single figure with selection support.
 * Unlike InfiniteCanvas, this focuses on a single figure and supports joint selection.
 */
@Composable
fun FigureEditorCanvas(
    modifier: Modifier = Modifier,
    figure: Figure,
    selectedJointId: String?,
    canvasState: CanvasState,
    figureModificationCount: Long = 0L,
    onCanvasStateChange: (CanvasState) -> Unit = {},
    onJointSelected: (String) -> Unit = {},
    onJointRotated: (Joint, Float) -> Unit = { _, _ -> },
    onFigureMoved: (Float, Float) -> Unit = { _, _ -> }
) {
    var compiledJointsForDrawing by remember {
        mutableStateOf(figure.compileForEditing())
    }

    LaunchedEffect(figure, figureModificationCount) {
        compiledJointsForDrawing = figure.compileForEditing()
    }

    val currentCanvasState by rememberUpdatedState(canvasState)
    val currentOnCanvasStateChange by rememberUpdatedState(onCanvasStateChange)
    val currentOnJointSelected by rememberUpdatedState(onJointSelected)
    val currentOnJointRotated by rememberUpdatedState(onJointRotated)
    val currentOnFigureMoved by rememberUpdatedState(onFigureMoved)
    val currentFigure by rememberUpdatedState(figure)

    Canvas(
        modifier = modifier
            .background(Color.White)
            // Scroll wheel zoom
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

                    val dragTarget = findDragTarget(
                        position = firstDown.position,
                        canvasState = currentCanvasState,
                        compiledJoints = compiledJointsForDrawing
                    )

                    // If clicking on a joint (not dragging), select it
                    var hasMoved = false

                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.none { it.pressed }) {
                            // Released - if no significant move, treat as click for selection
                            if (!hasMoved && dragTarget is DragTarget.JointRotation) {
                                currentOnJointSelected(dragTarget.compiledJoint.joint.id)
                            } else if (!hasMoved && dragTarget is DragTarget.SegmentClick) {
                                currentOnJointSelected(dragTarget.compiledJoint.joint.id)
                            }
                            break
                        }

                        val pointerCount = event.changes.count { it.pressed }

                        if (pointerCount >= 2) {
                            hasMoved = true
                            handleMultiTouch(event, currentCanvasState, currentOnCanvasStateChange)
                        } else if (pointerCount == 1) {
                            val change = event.changes.first()
                            if (change.positionChanged()) {
                                val delta = change.position - change.previousPosition
                                if (delta.getDistance() > 3f) {
                                    hasMoved = true
                                }
                            }
                            handleSingleTouch(
                                change = change,
                                dragTarget = dragTarget,
                                canvasState = currentCanvasState,
                                figure = currentFigure,
                                onCanvasStateChange = currentOnCanvasStateChange,
                                onJointRotated = currentOnJointRotated,
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
            // Draw figure
            compiledJointsForDrawing.forEach { compiled ->
                val isSelected = compiled.joint.id == selectedJointId
                val color = if (isSelected) Color(0xFF2196F3) else Color.Black
                val thickness = if (isSelected) 6f else 4f
                drawCompiledJoint(compiled, color, thickness)
            }

            // Draw rotation handles (red dots, blue for selected)
            compiledJointsForDrawing
                .filter { it.joint.length > 0f }
                .forEach { compiled ->
                    val isSelected = compiled.joint.id == selectedJointId
                    drawCircle(
                        color = if (isSelected) Color(0xFF2196F3) else Color.Red,
                        radius = if (isSelected) 10f else 8f,
                        center = Offset(compiled.endX, compiled.endY)
                    )
                }
        }
    }
}

private sealed class DragTarget {
    data class JointRotation(val compiledJoint: CompiledJoint) : DragTarget()
    data class SegmentClick(val compiledJoint: CompiledJoint) : DragTarget()
    data class FigureMove(val figure: Figure) : DragTarget()
}

private fun findDragTarget(
    position: Offset,
    canvasState: CanvasState,
    compiledJoints: List<CompiledJoint>
): DragTarget? {
    val (canvasX, canvasY) = canvasState.screenToCanvas(position.x, position.y)

    // Joint tips (red dots) have priority - for rotation
    findHitJoint(compiledJoints, canvasX, canvasY, canvasState.scale)?.let {
        return DragTarget.JointRotation(it)
    }

    // Then check segments - for selection and figure movement
    findHitSegment(compiledJoints, canvasX, canvasY, canvasState.scale)?.let {
        return DragTarget.SegmentClick(it)
    }

    return null
}

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

private fun handleSingleTouch(
    change: PointerInputChange,
    dragTarget: DragTarget?,
    canvasState: CanvasState,
    figure: Figure,
    onCanvasStateChange: (CanvasState) -> Unit,
    onJointRotated: (Joint, Float) -> Unit,
    onFigureMoved: (Float, Float) -> Unit
) {
    if (!change.positionChanged()) return

    val (canvasX, canvasY) = canvasState.screenToCanvas(change.position.x, change.position.y)
    val (prevCanvasX, prevCanvasY) = canvasState.screenToCanvas(change.previousPosition.x, change.previousPosition.y)

    when (dragTarget) {
        is DragTarget.JointRotation -> {
            val compiled = dragTarget.compiledJoint
            val newWorldAngle = atan2(canvasY - compiled.startY, canvasX - compiled.startX)
            val newRelativeAngle = newWorldAngle - compiled.parentWorldAngle
            onJointRotated(compiled.joint, newRelativeAngle)
            change.consume()
        }

        is DragTarget.SegmentClick, is DragTarget.FigureMove -> {
            onFigureMoved(figure.x + (canvasX - prevCanvasX), figure.y + (canvasY - prevCanvasY))
            change.consume()
        }

        null -> {
            val delta = change.position - change.previousPosition
            onCanvasStateChange(canvasState.pan(delta.x, delta.y))
            change.consume()
        }
    }
}

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
            val ellipseType = compiled.joint.type as SegmentType.Ellipse
            drawEllipseShape(
                color = color,
                thickness = thickness,
                length = compiled.joint.length,
                widthRatio = ellipseType.widthRatio,
                angle = worldAngle,
                centerX = compiled.centerX,
                centerY = compiled.centerY
            )
        }
        is SegmentType.Arc -> {
            val arcType = compiled.joint.type as SegmentType.Arc
            drawArcShape(
                color = color,
                thickness = thickness,
                length = compiled.joint.length,
                sweepAngle = arcType.sweepAngle,
                angle = worldAngle,
                centerX = compiled.centerX,
                centerY = compiled.centerY
            )
        }
    }

    // Joint dot at start
    drawCircle(color = color, radius = thickness, center = start)
}

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
                SegmentType.Circle, SegmentType.FilledCircle, is SegmentType.Arc, is SegmentType.Ellipse -> {
                    val distToCenter = distanceTo(canvasX, canvasY, compiled.centerX, compiled.centerY)
                    if (distToCenter <= compiled.radius) 0f else distToCenter - compiled.radius
                }
                SegmentType.Rectangle -> {
                    val distToCenter = distanceTo(canvasX, canvasY, compiled.centerX, compiled.centerY)
                    if (distToCenter <= compiled.joint.length / 2) 0f else distToCenter - compiled.joint.length / 2
                }
            }
        }
        .minByOrNull { it.value }
        ?.takeIf { it.value <= hitDistance }
        ?.key
}

private fun distanceTo(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val dx = x2 - x1
    val dy = y2 - y1
    return sqrt(dx * dx + dy * dy)
}

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
