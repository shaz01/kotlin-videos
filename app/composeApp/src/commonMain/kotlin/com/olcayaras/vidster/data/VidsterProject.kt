package com.olcayaras.vidster.data

import com.olcayaras.figures.FigureFrame
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Represents a saved animation project.
 * @param id Unique identifier for the project
 * @param name User-facing project name
 * @param frames Animation keyframes
 */
@Serializable
data class VidsterProject(
    val id: String,
    val name: String,
    val frames: List<FigureFrame>
) {
    companion object {
        @OptIn(ExperimentalUuidApi::class)
        fun generateId(): String = Uuid.random().toString()
    }
}

/**
 * Lightweight project info for listing (without full frame data).
 */
data class ProjectInfo(
    val id: String,
    val name: String
)
