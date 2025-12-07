package com.olcayaras.figures

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

/**
 * Draws a filled circle shape.
 *
 * @param color The color to draw with
 * @param radius The radius of the circle
 * @param centerX The X coordinate of the circle's center
 * @param centerY The Y coordinate of the circle's center
 */
internal fun DrawScope.drawFilledCircleShape(
    color: Color,
    radius: Float,
    centerX: Float,
    centerY: Float
) {
    drawCircle(
        color = color,
        radius = radius,
        center = Offset(centerX, centerY)
    )
}

/**
 * Draws a rectangle shape aligned along a segment.
 *
 * @param color The color to draw with
 * @param length The length of the segment (rectangle's major axis)
 * @param angle The angle of the segment in radians
 * @param startX The X coordinate of the segment's start
 * @param startY The Y coordinate of the segment's start
 * @param endX The X coordinate of the segment's end
 * @param endY The Y coordinate of the segment's end
 */
internal fun DrawScope.drawRectangleShape(
    color: Color,
    length: Float,
    angle: Float,
    startX: Float,
    startY: Float,
    endX: Float,
    endY: Float
) {
    // Calculate perpendicular direction for rectangle height
    val halfHeight = length * 0.25f
    val perpAngle = angle + (Math.PI / 2).toFloat()
    val perpX = halfHeight * cos(perpAngle)
    val perpY = halfHeight * sin(perpAngle)

    // Four corners of rectangle
    val path = Path().apply {
        moveTo(startX - perpX, startY - perpY)
        lineTo(startX + perpX, startY + perpY)
        lineTo(endX + perpX, endY + perpY)
        lineTo(endX - perpX, endY - perpY)
        close()
    }
    drawPath(path = path, color = color)
}

/**
 * Draws an ellipse shape aligned along a segment.
 *
 * @param color The color to draw with
 * @param thickness The stroke thickness
 * @param length The length of the segment (ellipse's major axis diameter)
 * @param widthRatio The ratio of minor axis to major axis
 * @param angle The angle of the segment in radians
 * @param centerX The X coordinate of the ellipse's center
 * @param centerY The Y coordinate of the ellipse's center
 */
internal fun DrawScope.drawEllipseShape(
    color: Color,
    thickness: Float,
    length: Float,
    widthRatio: Float,
    angle: Float,
    centerX: Float,
    centerY: Float
) {
    val majorRadius = length / 2
    val minorRadius = majorRadius * widthRatio

    val path = Path()
    val steps = 32
    for (i in 0..steps) {
        val t = (i.toFloat() / steps) * 2 * Math.PI.toFloat()
        val localX = majorRadius * cos(t)
        val localY = minorRadius * sin(t)
        val worldX = centerX + localX * cos(angle) - localY * sin(angle)
        val worldY = centerY + localX * sin(angle) + localY * cos(angle)
        if (i == 0) path.moveTo(worldX, worldY) else path.lineTo(worldX, worldY)
    }
    path.close()
    drawPath(path = path, color = color, style = Stroke(width = thickness))
}

/**
 * Draws an arc shape.
 *
 * @param color The color to draw with
 * @param thickness The stroke thickness
 * @param length The length of the segment (arc's diameter)
 * @param sweepAngle The sweep angle of the arc in radians
 * @param angle The angle of the segment in radians
 * @param centerX The X coordinate of the arc's center
 * @param centerY The Y coordinate of the arc's center
 */
internal fun DrawScope.drawArcShape(
    color: Color,
    thickness: Float,
    length: Float,
    sweepAngle: Float,
    angle: Float,
    centerX: Float,
    centerY: Float
) {
    val radius = length / 2
    val arcStartAngle = angle + Math.PI.toFloat()

    val path = Path()
    val steps = 24
    for (i in 0..steps) {
        val t = arcStartAngle + (i.toFloat() / steps) * sweepAngle
        val x = centerX + radius * cos(t)
        val y = centerY + radius * sin(t)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path = path, color = color, style = Stroke(width = thickness))
}
