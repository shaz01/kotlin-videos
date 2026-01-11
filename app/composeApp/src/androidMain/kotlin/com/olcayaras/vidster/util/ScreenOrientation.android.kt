package com.olcayaras.vidster.util

import android.app.Activity
import android.content.pm.ActivityInfo
import java.lang.ref.WeakReference

/**
 * Holder for the current activity reference.
 * Must be set in Activity.onCreate() and cleared in onDestroy().
 */
object ActivityHolder {
    private var activityRef: WeakReference<Activity>? = null

    fun setActivity(activity: Activity) {
        activityRef = WeakReference(activity)
    }

    fun clearActivity() {
        activityRef = null
    }

    fun getActivity(): Activity? = activityRef?.get()
}

actual fun setScreenOrientation(orientation: ScreenOrientationType) {
    val activity = ActivityHolder.getActivity() ?: return

    activity.requestedOrientation = when (orientation) {
        ScreenOrientationType.Landscape -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        ScreenOrientationType.Unspecified -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
}
