package com.olcayaras.figures

/**
 * State for an infinite canvas with pan and zoom support.
 *
 * @param offsetX Horizontal pan offset in canvas coordinates
 * @param offsetY Vertical pan offset in canvas coordinates
 * @param scale Zoom level (1.0 = 100%, 2.0 = 200%, etc.)
 */
data class CanvasState(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scale: Float = 1f
) {
    companion object {
        const val MIN_SCALE = 0.01f
        const val MAX_SCALE = 100f
    }

    /**
     * Apply a pan delta to the canvas state.
     */
    fun pan(deltaX: Float, deltaY: Float): CanvasState = copy(
        offsetX = offsetX + deltaX,
        offsetY = offsetY + deltaY
    )

    /**
     * Apply a zoom centered around a pivot point.
     * @param zoomDelta Multiplicative zoom factor (e.g., 1.1 to zoom in 10%)
     * @param pivotX X coordinate of zoom center in screen space
     * @param pivotY Y coordinate of zoom center in screen space
     */
    fun zoom(zoomDelta: Float, pivotX: Float, pivotY: Float): CanvasState {
        val newScale = (scale * zoomDelta).coerceIn(MIN_SCALE, MAX_SCALE)
        val actualZoom = newScale / scale

        // Adjust offset to keep pivot point stationary
        val newOffsetX = pivotX - (pivotX - offsetX) * actualZoom
        val newOffsetY = pivotY - (pivotY - offsetY) * actualZoom

        return copy(
            offsetX = newOffsetX,
            offsetY = newOffsetY,
            scale = newScale
        )
    }

    /**
     * Convert a screen coordinate to canvas (world) coordinate.
     */
    fun screenToCanvas(screenX: Float, screenY: Float): Pair<Float, Float> {
        val canvasX = (screenX - offsetX) / scale
        val canvasY = (screenY - offsetY) / scale
        return canvasX to canvasY
    }

    /**
     * Convert a canvas (world) coordinate to screen coordinate.
     */
    fun canvasToScreen(canvasX: Float, canvasY: Float): Pair<Float, Float> {
        val screenX = canvasX * scale + offsetX
        val screenY = canvasY * scale + offsetY
        return screenX to screenY
    }
}
