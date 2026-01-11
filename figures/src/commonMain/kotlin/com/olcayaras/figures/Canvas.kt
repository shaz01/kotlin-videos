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
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntSize
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Does what ScaleBox does to a canvas.
 * Draws a viewport into any sized canvas, scaling appropriately,
 * provided that their aspect ratios match.
 */
private fun DrawScope.transformViewport(
    viewport: Viewport,
    viewportSize: IntSize,
    block: DrawScope.() -> Unit
) {
    if (viewportSize.width == 0 || viewportSize.height == 0) return

    scale(viewport.scale) {
        scale(
            scaleX = (size.width / viewportSize.width),
            scaleY = (size.height / viewportSize.height),
            pivot = Offset.Zero
        ) {
            translate(-viewport.leftX, -viewport.topY) {
                block()
            }
        }
    }
}

private fun DrawScope.drawSegment(
    segment: Segment,
    color: Color,
    thickness: Float
) {
    if (segment.length <= 0f) return

    val start = Offset(segment.startX, segment.startY)
    val end = Offset(segment.endX, segment.endY)

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

        SegmentType.FilledCircle -> {
            drawFilledCircleShape(
                color = color,
                radius = segment.radius,
                centerX = segment.centerX,
                centerY = segment.centerY
            )
        }

        SegmentType.Rectangle -> {
            drawRectangleShape(
                color = color,
                length = segment.length,
                angle = segment.angle,
                startX = segment.startX,
                startY = segment.startY,
                endX = segment.endX,
                endY = segment.endY
            )
        }

        is SegmentType.Ellipse -> {
            drawEllipseShape(
                color = color,
                thickness = thickness,
                length = segment.length,
                widthRatio = segment.type.widthRatio,
                angle = segment.angle,
                centerX = segment.centerX,
                centerY = segment.centerY
            )
        }

        is SegmentType.Arc -> {
            drawArcShape(
                color = color,
                thickness = thickness,
                length = segment.length,
                sweepAngle = segment.type.sweepAngle,
                angle = segment.angle,
                centerX = segment.centerX,
                centerY = segment.centerY
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
 * @param viewportSize If provided, the canvas will scale content as if it's rendering at this logical size.
 *                   Similar to ScaleBox but works with Canvas drawing. Automatically applies aspect ratio.
 */
@Composable
fun SegmentFrameCanvas(
    modifier: Modifier = Modifier,
    frame: SegmentFrame,
    viewportSize: IntSize? = null
) {
    val finalModifier = if (viewportSize != null) {
        modifier.aspectRatio(viewportSize.width.toFloat() / viewportSize.height.toFloat())
    } else {
        modifier
    }

    Canvas(modifier = finalModifier) {
        val size = viewportSize ?: size.toIntSize()
        transformViewport(frame.viewport, size) {
            frame.segments.forEach { segment ->
                drawSegment(segment, Color.Black, FigureConstants.DEFAULT_STROKE_WIDTH)
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
