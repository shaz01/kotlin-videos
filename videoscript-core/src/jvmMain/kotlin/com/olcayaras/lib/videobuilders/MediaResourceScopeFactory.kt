package com.olcayaras.lib.videobuilders

actual fun createMediaResourceScope(fps: Int): MediaResourceScope {
    return JvmMediaResourceScope(fps)
}