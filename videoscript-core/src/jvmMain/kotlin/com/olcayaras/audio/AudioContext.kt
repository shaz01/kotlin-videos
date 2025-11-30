package com.olcayaras.audio

import org.lwjgl.openal.AL
import org.lwjgl.openal.ALC
import org.lwjgl.openal.ALC10
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.nio.IntBuffer

internal class AudioContext {
    private val device: Long
    private val context: Long

    init {
        device = ALC10.alcOpenDevice(null as ByteBuffer?)
        if (device == MemoryUtil.NULL) {
            throw RuntimeException("Failed to open OpenAL device")
        }

        context = ALC10.alcCreateContext(device, null as IntBuffer?)
        if (context == MemoryUtil.NULL) {
            throw RuntimeException("Failed to create OpenAL context")
        }

        ALC10.alcMakeContextCurrent(context)
        AL.createCapabilities(ALC.createCapabilities(device))
    }

    fun cleanup() {
        ALC10.alcMakeContextCurrent(MemoryUtil.NULL)
        ALC10.alcDestroyContext(context)
        ALC10.alcCloseDevice(device)
    }
}