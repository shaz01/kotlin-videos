package com.olcayaras.vidster.ui.screens.editfigure

import com.olcayaras.figures.Figure
import com.olcayaras.figures.Joint
import com.olcayaras.figures.SegmentType
import kotlin.math.PI

private const val DEFAULT_FIGURE_X = 400f
private const val DEFAULT_FIGURE_Y = 300f

/**
 * Templates for creating new figures.
 */
enum class FigureTemplate(val displayName: String) {
    BLANK("Blank"),
    HUMAN("Human");

    fun createFigure(x: Float = DEFAULT_FIGURE_X, y: Float = DEFAULT_FIGURE_Y): Figure {
        return when (this) {
            BLANK -> Figure(
                name = "New Figure",
                root = Joint("root", length = 0f, angle = 0f).apply {
                    // Add one initial joint so there's something visible and selectable
                    children += Joint("joint_1", length = 50f, angle = (-PI / 2).toFloat())
                },
                x = x,
                y = y
            )
            HUMAN -> Figure(
                name = "Human",
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
    }
}
