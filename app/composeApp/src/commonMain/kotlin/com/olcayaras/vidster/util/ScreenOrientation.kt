package com.olcayaras.vidster.util

/**
 * Types of screen orientation that can be requested.
 */
enum class ScreenOrientationType {
    /** Force landscape orientation (sensor-based landscape). */
    Landscape,
    /** Allow any orientation (system default). */
    Unspecified
}

/**
 * Request a specific screen orientation.
 * On Android, this changes the activity's requested orientation.
 * On other platforms, this is a no-op.
 */
expect fun setScreenOrientation(orientation: ScreenOrientationType)
