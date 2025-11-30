package com.olcayaras.lib

import androidx.compose.runtime.Composable
import kotlin.jvm.JvmInline
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@JvmInline
value class Frame(val count: Int) {
    @Composable
    fun toDuration() = (count * 1000 / LocalFPS.current).milliseconds

    @Composable
    fun toMillis() = count * 1000 / LocalFPS.current

    @Composable
    fun toSeconds() = count / LocalFPS.current
}

@get:Composable
val Duration.ofFrames: Int get() = (this.inWholeMilliseconds * LocalFPS.current / 1000).toInt()

fun Duration.ofFrames(fps: Int) = (this.inWholeMilliseconds * fps / 1000).toInt()



@get:Composable
val Int.frames get() = Frame(this)

fun Int.asDuration(fps: Int) = (this * 1000 / fps).milliseconds