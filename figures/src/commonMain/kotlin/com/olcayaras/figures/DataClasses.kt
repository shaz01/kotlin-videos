package com.olcayaras.figures

import kotlin.math.cos
import kotlin.math.sin

data class Joint(
    val id: String,
    val length: Float, // distance from parent
    var angle: Float, // relative to parent (radians)
    val children: MutableList<Joint> = mutableListOf()
)

data class Figure(
    val name: String,
    val root: Joint,
    var x: Float, // origin position
    var y: Float
)

data class FigureFrame(
    val figures: List<Figure>,
    val viewport: Viewport,
    val viewportTransition: ViewportTransition = ViewportTransition.None
) {
    fun compile() = SegmentFrame(
        segments = figures.flatMap { compileJoints(root = it.root, startX = it.x, startY = it.y) },
        viewport = viewport,
        viewportTransition = viewportTransition
    )
}


// --------------------------------------------------------------------------------------------

// Think like compiled joint/figure
data class Segment(
    val length: Float,
    val angle: Float, // radians
    val startX: Float,
    val startY: Float,
)

// Think like compiled FigureFrame
data class SegmentFrame(
    val segments: List<Segment>,
    val viewport: Viewport,
    val viewportTransition: ViewportTransition = ViewportTransition.None
)

fun compileJoints(root: Joint, startX: Float, startY: Float): List<Segment> {
    val dx = root.length * cos(root.angle)
    val dy = root.length * sin(root.angle)


    val result = root.children.flatMap { child ->
        compileJoints(root = child, startX + dx, startY + dy)
    }
    return result + Segment(root.length, root.angle, startX, startY)
}

fun Joint.compile() = compileJoints(root = this, startX = 0f, startY = 0f)


fun compileFigure(figure: Figure): List<Segment> =
    compileJoints(root = figure.root, startX = figure.x, startY = figure.y)

fun Figure.compile() = compileFigure(this)

// --------------------------------------------------------------------------------------------
// Interactive editing support - CompiledJoint preserves hierarchy and world positions

/**
 * A compiled joint that preserves the reference to the original [Joint] for editing,
 * along with computed world positions for hit testing and rendering.
 *
 * @param joint Reference to the original joint (for editing angle)
 * @param figure Reference to the figure this joint belongs to
 * @param startX World X position where this joint connects to parent
 * @param startY World Y position where this joint connects to parent
 * @param endX World X position of joint tip (where red dot renders)
 * @param endY World Y position of joint tip
 * @param parentWorldAngle Accumulated angle from root to parent (for angle calculation)
 */
data class CompiledJoint(
    val joint: Joint,
    val figure: Figure,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val parentWorldAngle: Float
)

/**
 * Compiles a figure into a list of [CompiledJoint]s with world positions.
 * Used for interactive editing - hit testing and joint manipulation.
 */
fun compileJointsForEditing(
    figure: Figure,
    joint: Joint,
    startX: Float,
    startY: Float,
    parentWorldAngle: Float = 0f
): List<CompiledJoint> {
    val worldAngle = parentWorldAngle + joint.angle
    val endX = startX + joint.length * cos(worldAngle)
    val endY = startY + joint.length * sin(worldAngle)

    val compiled = CompiledJoint(
        joint = joint,
        figure = figure,
        startX = startX,
        startY = startY,
        endX = endX,
        endY = endY,
        parentWorldAngle = parentWorldAngle
    )

    val childJoints = joint.children.flatMap { child ->
        compileJointsForEditing(figure, child, endX, endY, worldAngle)
    }

    return listOf(compiled) + childJoints
}

fun Figure.compileForEditing(): List<CompiledJoint> =
    compileJointsForEditing(this, root, x, y)

// --------------------------------------------------------------------------------------------

data class Viewport(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f // radians
) {
    fun lerp(other: Viewport, t: Float): Viewport = Viewport(
        offsetX = offsetX + (other.offsetX - offsetX) * t,
        offsetY = offsetY + (other.offsetY - offsetY) * t,
        scale = scale + (other.scale - scale) * t,
        rotation = rotation + (other.rotation - rotation) * t
    )
}

sealed interface ViewportTransition {
    object None : ViewportTransition
    object Lerp : ViewportTransition
}