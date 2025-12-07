package com.olcayaras.figures

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.cos
import kotlin.math.sin

private fun DrawScope.withScreenSize(
    screenSize: IntSize?,
    block: DrawScope.() -> Unit
) {
    if (screenSize == null || screenSize.width <= 0 || screenSize.height <= 0) {
        block()
        return
    }

    val contentScale = if (screenSize.width > screenSize.height) {
        size.width / screenSize.width
    } else {
        size.height / screenSize.height
    }

    withTransform({
        scale(contentScale, contentScale)
    }) {
        block()
    }
}

private fun DrawScope.withViewport(
    viewport: Viewport,
    pivotX: Float,
    pivotY: Float,
    block: DrawScope.() -> Unit
) {
    withTransform({
        // Apply transforms around canvas center
        translate(pivotX, pivotY)
        rotate(degrees = viewport.rotation * (180f / Math.PI.toFloat()))
        scale(viewport.scale, viewport.scale)
        translate(-pivotX + viewport.offsetX, -pivotY + viewport.offsetY)
    }) {
        block()
    }
}

private fun DrawScope.drawSegment(
    segment: Segment,
    color: Color,
    thickness: Float
) {
    if (segment.length <= 0f) return

    val endX = segment.startX + segment.length * cos(segment.angle)
    val endY = segment.startY + segment.length * sin(segment.angle)
    val start = Offset(segment.startX, segment.startY)
    val end = Offset(endX, endY)

    when (segment.type) {
        SegmentType.Line -> {
            drawLine(
                color = color,
                start = start,
                end = end,
                strokeWidth = thickness,
                cap = StrokeCap.Round
            )
        }
        SegmentType.Circle -> {
            drawCircle(
                color = color,
                radius = segment.radius,
                center = Offset(segment.centerX, segment.centerY),
                style = Stroke(width = thickness)
            )
        }
    }

    // Joint circle at start
    drawCircle(
        color = color,
        radius = thickness,
        center = start
    )
}

/**
 * The canvas that renders a [SegmentFrame].
 * Not interactive. Not usable for editor.
 *
 * @param screenSize If provided, the canvas will scale content as if it's rendering at this logical size.
 *                   Similar to ScaleBox but works with Canvas drawing. Automatically applies aspect ratio.
 */
@Composable
fun SegmentFrameCanvas(
    modifier: Modifier = Modifier,
    frame: SegmentFrame,
    screenSize: IntSize? = null
) {
    val finalModifier = if (screenSize != null) {
        modifier.aspectRatio(screenSize.width.toFloat() / screenSize.height.toFloat())
    } else {
        modifier
    }

    Canvas(modifier = finalModifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2

        withScreenSize(screenSize) {
            withViewport(frame.viewport, pivotX = centerX, pivotY = centerY) {
                frame.segments.forEach { segment ->
                    drawSegment(segment, Color.Black, 4f)
                }
            }
        }
    }
}

@Preview
@Composable
fun SegmentFrameCanvasPreview() {
    SegmentFrameCanvas(
        modifier = Modifier.size(150.dp, 100.dp).background(Color.White),
        frame = getMockSegmentFrame()
    )
}
