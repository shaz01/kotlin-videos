package com.olcayaras.figures

import kotlin.math.PI

/**
 * Interpolates between two angles using the shortest path.
 * Handles wraparound at 2π to avoid spinning the long way around.
 *
 * @param from Starting angle in radians
 * @param to Target angle in radians
 * @param t Interpolation factor (0.0 = from, 1.0 = to)
 * @return Interpolated angle in radians
 */
fun lerpAngle(from: Float, to: Float, t: Float): Float {
    val pi = PI.toFloat()
    val twoPi = 2 * pi

    // Normalize difference to [-π, π] for shortest path
    var diff = (to - from) % twoPi
    if (diff > pi) diff -= twoPi
    if (diff < -pi) diff += twoPi

    return from + diff * t
}

/**
 * Creates an interpolated joint tree between this joint and [other].
 * Joints are matched by ID. Children that don't match are kept from this joint.
 *
 * @param other Target joint to interpolate towards
 * @param t Interpolation factor (0.0 = this, 1.0 = other)
 * @return New Joint with interpolated angle and children
 */
fun Joint.lerp(other: Joint, t: Float): Joint {
    // Build a map of other's children by ID for efficient lookup
    val otherChildrenById = other.children.associateBy { it.id }

    return Joint(
        id = id,
        length = length, // Length doesn't change during interpolation
        angle = lerpAngle(angle, other.angle, t),
        type = type, // Type doesn't change during interpolation
        children = children.mapTo(mutableListOf()) { child ->
            val matchingChild = otherChildrenById[child.id]
            if (matchingChild != null) {
                child.lerp(matchingChild, t)
            } else {
                // No matching child in target, keep original
                child.deepCopy()
            }
        }
    )
}

/**
 * Creates an interpolated figure between this and [other].
 * Interpolates position (x, y) and all joint angles.
 *
 * @param other Target figure to interpolate towards (must have same name)
 * @param t Interpolation factor (0.0 = this, 1.0 = other)
 * @return New Figure with interpolated position and joints
 */
fun Figure.lerp(other: Figure, t: Float): Figure {
    require(name == other.name) { "Figure names must match for interpolation: '$name' vs '${other.name}'" }

    return Figure(
        name = name,
        root = root.lerp(other.root, t),
        x = x + (other.x - x) * t,
        y = y + (other.y - y) * t
    )
}

/**
 * Creates an interpolated frame between this and [other].
 * Figures are matched by name. Unmatched figures appear/disappear at midpoint (t=0.5).
 * Viewport is interpolated if viewportTransition is Lerp.
 *
 * @param other Target frame to interpolate towards
 * @param t Interpolation factor (0.0 = this, 1.0 = other)
 * @return New FigureFrame with interpolated figures and viewport
 */
fun FigureFrame.lerp(other: FigureFrame, t: Float): FigureFrame {
    val thisFiguresByName = figures.associateBy { it.name }
    val otherFiguresByName = other.figures.associateBy { it.name }

    val interpolatedFigures = mutableListOf<Figure>()

    // Handle figures that exist in this frame
    for ((name, thisFigure) in thisFiguresByName) {
        val otherFigure = otherFiguresByName[name]
        if (otherFigure != null) {
            // Figure exists in both frames - interpolate
            interpolatedFigures.add(thisFigure.lerp(otherFigure, t))
        } else {
            // Figure only exists in this frame - show until midpoint
            if (t < 0.5f) {
                interpolatedFigures.add(thisFigure.deepCopy())
            }
        }
    }

    // Handle figures that only exist in other frame - appear at midpoint
    for ((name, otherFigure) in otherFiguresByName) {
        if (name !in thisFiguresByName && t >= 0.5f) {
            interpolatedFigures.add(otherFigure.deepCopy())
        }
    }

    // Interpolate viewport if transition is Lerp
    val interpolatedViewport = when (viewportTransition) {
        ViewportTransition.Lerp -> viewport.lerp(other.viewport, t)
        ViewportTransition.None -> viewport
    }

    return FigureFrame(
        figures = interpolatedFigures,
        viewport = interpolatedViewport,
        viewportTransition = viewportTransition
    )
}

/**
 * Expands a list of keyframes into interpolated frames for smooth playback.
 *
 * @param keyframes List of keyframes (user-created frames)
 * @param keyframeFps Rate at which keyframes are spaced (e.g., 3 = 3 keyframes per second of animation)
 * @param targetFps Desired playback framerate (e.g., 24)
 * @return Expanded list of frames ready for smooth playback
 *
 * Example: With keyframeFps=3 and targetFps=24, each keyframe pair gets 8 interpolated frames between them.
 * 3 keyframes would expand to: (3-1) * 8 + 1 = 17 frames
 */
fun expandFrames(
    keyframes: List<FigureFrame>,
    keyframeFps: Int = 3,
    targetFps: Int = 24
): List<FigureFrame> {
    // Edge cases
    if (keyframes.isEmpty()) return emptyList()
    if (keyframes.size == 1) return listOf(keyframes.first().deepCopy())

    val framesPerKeyframe = targetFps / keyframeFps
    val result = mutableListOf<FigureFrame>()

    for (i in 0 until keyframes.lastIndex) {
        val from = keyframes[i]
        val to = keyframes[i + 1]

        // Generate interpolated frames between this keyframe pair
        for (step in 0 until framesPerKeyframe) {
            val t = step.toFloat() / framesPerKeyframe
            result.add(from.lerp(to, t))
        }
    }

    // Add the final keyframe
    result.add(keyframes.last().deepCopy())

    return result
}
