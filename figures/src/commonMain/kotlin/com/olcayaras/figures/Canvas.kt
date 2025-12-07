package com.olcayaras.figures

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
            drawCircle(
                color = color,
                radius = segment.radius,
                center = Offset(segment.centerX, segment.centerY)
            )
        }
        SegmentType.Rectangle -> {
            // Calculate perpendicular direction for rectangle height
            val halfHeight = segment.length * 0.25f  // Half of 0.5 aspect ratio
            val perpAngle = segment.angle + (Math.PI / 2).toFloat()
            val perpX = halfHeight * cos(perpAngle)
            val perpY = halfHeight * sin(perpAngle)

            // Four corners of rectangle
            val path = Path().apply {
                moveTo(segment.startX - perpX, segment.startY - perpY)
                lineTo(segment.startX + perpX, segment.startY + perpY)
                lineTo(segment.endX + perpX, segment.endY + perpY)
                lineTo(segment.endX - perpX, segment.endY - perpY)
                close()
            }
            drawPath(path = path, color = color)
        }
        is SegmentType.Ellipse -> {
            // Draw ellipse using path with calculated points (major axis along segment)
            val majorRadius = segment.length / 2
            val minorRadius = majorRadius * segment.type.widthRatio

            val path = Path()
            val steps = 32
            for (i in 0..steps) {
                val t = (i.toFloat() / steps) * 2 * Math.PI.toFloat()
                val localX = majorRadius * cos(t)
                val localY = minorRadius * sin(t)
                val worldX = segment.centerX + localX * cos(segment.angle) - localY * sin(segment.angle)
                val worldY = segment.centerY + localX * sin(segment.angle) + localY * cos(segment.angle)
                if (i == 0) path.moveTo(worldX, worldY) else path.lineTo(worldX, worldY)
            }
            path.close()
            drawPath(path = path, color = color, style = Stroke(width = thickness))
        }
        is SegmentType.Arc -> {
            // Draw arc from start point, curving around center
            val radius = segment.length / 2
            val sweepAngle = segment.type.sweepAngle
            val arcStartAngle = segment.angle + Math.PI.toFloat()

            val path = Path()
            val steps = 24
            for (i in 0..steps) {
                val t = arcStartAngle + (i.toFloat() / steps) * sweepAngle
                val x = segment.centerX + radius * cos(t)
                val y = segment.centerY + radius * sin(t)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path = path, color = color, style = Stroke(width = thickness))
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
