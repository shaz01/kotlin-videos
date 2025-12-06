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