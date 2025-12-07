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

fun getMockSegmentFrame(name: String = "humanoid"): SegmentFrame {
    return SegmentFrame(
        viewport = Viewport(
            offsetX = 0f,
            offsetY = 0f,
            rotation = 0f,
            scale = 1f
        ),
        segments = (0..1920).step(50).flatMap { x ->
            getMockFigure(name, x = x.toFloat(), y = x * 0.3f).compile()
        }
    )
}