package com.olcayaras.lib.subtitles

import kotlin.time.Duration

private fun Duration.toSRTFormat(): String {
    val totalMillis = inWholeMilliseconds
    val hours = totalMillis / 3600000
    val minutes = (totalMillis % 3600000) / 60000
    val seconds = (totalMillis % 60000) / 1000
    val millis = totalMillis % 1000
    return "%02d:%02d:%02d,%03d".format(hours, minutes, seconds, millis)
}

// Extension function for formatting subtitles to SRT format
fun List<Subtitle>.toSRT(): String = buildString {
    this@toSRT.forEachIndexed { index, subtitle ->
        append("${index + 1}\n")
        append("${subtitle.startTime.toSRTFormat()} --> ${subtitle.endTime.toSRTFormat()}\n")
        append("${subtitle.text}\n\n")
    }
}