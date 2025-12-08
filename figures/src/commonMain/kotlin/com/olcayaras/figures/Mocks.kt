package com.olcayaras.figures

import kotlin.math.PI


fun getMockFigure(name: String = "humanoid", x: Float = 300f, y: Float = 200f): Figure {
    return Figure(
        name = name,
        x = x,
        y = y,
        root = Joint("hip", length = 0f, angle = 0f).apply {
            children += Joint("torso", length = 50f, angle = (-PI / 2).toFloat()).apply {
                children += Joint("head", length = 30f, angle = 0f, type = SegmentType.Circle)
                children += Joint("leftArm", length = 40f, angle = (PI / 3).toFloat())
                children += Joint("rightArm", length = 40f, angle = (-4 * PI / 3).toFloat())
            }
            children += Joint("leftLeg", length = 45f, angle = (PI / 2 + 0.2f).toFloat())
            children += Joint("rightLeg", length = 45f, angle = (PI / 2 - 0.2f).toFloat())
        }
    )
}

/**
 * A figure demonstrating all segment types.
 */
fun getMockShapesDemo(x: Float = 300f, y: Float = 300f): Figure {
    return Figure(
        name = "shapes-demo",
        x = x,
        y = y,
        root = Joint("origin", length = 0f, angle = 0f).apply {
            // Circle (stroke)
            children += Joint("line-circle", length = 100f, angle = (30 * PI / 180).toFloat()).apply {
                children += Joint("circle", length = 40f, angle = 0.toFloat(), type = SegmentType.Circle)
            }

            // Filled circle
            children += Joint("line-filled-circle", length = 100f, angle = ((30 * PI / 180f).toFloat())).apply {
                children += Joint(
                    "filledCircle",
                    length = 30f,
                    angle = (2 * PI / 3).toFloat(),
                    type = SegmentType.FilledCircle
                )
            }

            // Rectangle
            children += Joint("line-rect", length = 100f, angle = (30 * PI / 180).toFloat()).apply {
                children += Joint("rect", length = 60f, angle = PI.toFloat(), type = SegmentType.Rectangle)
            }

            // Ellipse
            children += Joint("line-ellipse", length = 100f, angle = (30 * PI / 180).toFloat()).apply {
                children += Joint(
                    "ellipse",
                    length = 50f,
                    angle = (-2 * PI / 3).toFloat(),
                    type = SegmentType.Ellipse(widthRatio = 0.4f)
                )
            }
            // Arc (half circle)
            children += Joint("line-arc", length = 100f, angle = (30 * PI / 180).toFloat()).apply {
                children += Joint(
                    "arc",
                    length = 40f,
                    angle = (-PI / 3).toFloat(),
                    type = SegmentType.Arc(sweepAngle = PI.toFloat())
                )
            }
        }
    )
}

fun getMockSegmentFrame(name: String = "humanoid"): SegmentFrame {
    return SegmentFrame(
        viewport = Viewport(
            leftX = 0f,
            topY = 0f,
            rotation = 0f,
            scale = 1f
        ),
        segments = (0..1920).step(50).flatMap { x ->
            getMockFigure(name, x = x.toFloat(), y = x * 0.3f).compile()
        }
    )
}