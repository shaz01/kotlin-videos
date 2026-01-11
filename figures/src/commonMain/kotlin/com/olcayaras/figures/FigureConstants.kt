package com.olcayaras.figures

object FigureConstants {
    // Interaction hit testing
    const val JOINT_HIT_RADIUS = 48f
    const val SEGMENT_HIT_DISTANCE = 20f
    const val VIEWPORT_EDGE_HIT_DISTANCE = 20f

    // Stroke widths
    const val DEFAULT_STROKE_WIDTH = 8f
    const val SELECTED_STROKE_WIDTH = 10f

    // Joint handle sizes
    const val JOINT_HANDLE_RADIUS = 8f
    const val SELECTED_JOINT_HANDLE_RADIUS = 10f

    // Editor interaction
    const val DRAG_THRESHOLD_DISTANCE = 3f

    // Viewport overlay styling
    const val VIEWPORT_OVERLAY_ALPHA = 0.4f
    const val VIEWPORT_BORDER_ALPHA = 0.6f
    const val VIEWPORT_STROKE_WIDTH = 2f
    const val VIEWPORT_DASH_ON = 20f
    const val VIEWPORT_DASH_OFF = 10f
    const val VIEWPORT_LABEL_PADDING = 10f

    // Global scale multiplier for joint lengths
    const val FIGURE_LENGTH_SCALE = 2f

    // Scroll wheel zoom factors
    const val SCROLL_WHEEL_ZOOM_OUT_FACTOR = 0.9f
    const val SCROLL_WHEEL_ZOOM_IN_FACTOR = 1.1f
}
