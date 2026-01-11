package com.olcayaras.vidster.data

import com.olcayaras.figures.FigureFrame
import com.olcayaras.figures.Viewport
import com.olcayaras.figures.deepCopy
import com.olcayaras.figures.getMockFigure
import io.github.aakira.napier.Napier
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readString
import io.github.vinceglb.filekit.writeString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Repository for saving and loading animation projects using FileKit.
 * Projects are stored as JSON files in the app's files directory.
 */
class ProjectRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val projectsDir: PlatformFile
        get() = FileKit.filesDir / PROJECTS_DIR

    /**
     * Get a flow of all saved projects (lightweight info only).
     */
    fun getProjects(): Flow<List<ProjectInfo>> = flow {
        emit(loadProjectList())
    }

    /**
     * Load the list of saved projects.
     */
    suspend fun loadProjectList(): List<ProjectInfo> = withContext(Dispatchers.IO) {
        try {
            ensureProjectsDir()
            val files = projectsDir.list()
            files
                .filter { it.name.endsWith(FILE_EXTENSION) }
                .mapNotNull { file ->
                    try {
                        val content = file.readString()
                        val project = json.decodeFromString<VidsterProject>(content)
                        ProjectInfo(id = project.id, name = project.name)
                    } catch (e: Exception) {
                        Napier.e(e) { "Failed to read project: ${file.name}" }
                        null
                    }
                }
                .sortedBy { it.name }
        } catch (e: Exception) {
            Napier.e(e) { "Failed to list projects" }
            emptyList()
        }
    }

    /**
     * Load a project by its ID.
     */
    suspend fun loadProject(id: String): VidsterProject? = withContext(Dispatchers.IO) {
        try {
            val file = getProjectFile(id)
            if (!file.exists()) {
                Napier.e { "Project file not found: $id" }
                return@withContext null
            }
            val content = file.readString()
            json.decodeFromString<VidsterProject>(content)
        } catch (e: Exception) {
            Napier.e(e) { "Failed to load project: $id" }
            null
        }
    }

    /**
     * Save a project.
     */
    suspend fun saveProject(project: VidsterProject): Boolean = withContext(Dispatchers.IO) {
        try {
            ensureProjectsDir()
            val file = getProjectFile(project.id)
            val content = json.encodeToString(project)
            file.writeString(content)
            Napier.d { "Saved project: ${project.name}" }
            true
        } catch (e: Exception) {
            Napier.e(e) { "Failed to save project: ${project.name}" }
            false
        }
    }

    /**
     * Save just the frames for an existing project.
     */
    suspend fun saveProjectFrames(id: String, frames: List<FigureFrame>): Boolean = withContext(Dispatchers.IO) {
        try {
            val existingProject = loadProject(id) ?: return@withContext false
            val updatedProject = existingProject.copy(frames = frames.map { it.deepCopy() })
            saveProject(updatedProject)
        } catch (e: Exception) {
            Napier.e(e) { "Failed to save project frames: $id" }
            false
        }
    }

    /**
     * Delete a project.
     */
    suspend fun deleteProject(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = getProjectFile(id)
            file.delete()
            Napier.d { "Deleted project: $id" }
            true
        } catch (e: Exception) {
            Napier.e(e) { "Failed to delete project: $id" }
            false
        }
    }

    /**
     * Rename a project by ID.
     */
    suspend fun renameProject(id: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val existingProject = loadProject(id) ?: return@withContext false
            val updatedProject = existingProject.copy(name = newName)
            saveProject(updatedProject)
        } catch (e: Exception) {
            Napier.e(e) { "Failed to rename project: $id" }
            false
        }
    }

    /**
     * Create a new project with default content.
     */
    suspend fun createNewProject(name: String): VidsterProject = withContext(Dispatchers.IO) {
        val project = VidsterProject(
            id = VidsterProject.generateId(),
            name = name,
            frames = listOf(
                FigureFrame(
                    figures = listOf(getMockFigure(x = 400f, y = 300f)),
                    viewport = Viewport()
                )
            )
        )
        saveProject(project)
        project
    }

    private fun getProjectFile(id: String): PlatformFile {
        return projectsDir / "$id$FILE_EXTENSION"
    }

    private fun ensureProjectsDir() {
        if (!projectsDir.exists()) {
            projectsDir.createDirectories()
        }
    }

    companion object {
        private const val PROJECTS_DIR = "projects"
        private const val FILE_EXTENSION = ".vid"
    }
}
