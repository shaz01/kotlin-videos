package com.olcayaras.vidster.utils

/**
 * No-op implementation for desktop.
 * Desktop apps don't have orientation constraints.
 */
actual fun setScreenOrientation(orientation: ScreenOrientationType) {
    // No-op on desktop - window orientation is controlled by the OS/user
}
