package com.olcayaras.vidster.ui.screens.projectlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.push
import com.olcayaras.vidster.ViewModel
import com.olcayaras.vidster.data.ProjectInfo
import com.olcayaras.vidster.data.ProjectRepository
import com.olcayaras.vidster.ui.Route
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

sealed interface ProjectListEvent {
    data object ShowCreateDialog : ProjectListEvent
    data class ShowRenameDialog(val id: String) : ProjectListEvent
    data class UpdateDialogName(val name: String) : ProjectListEvent
    data object ConfirmDialog : ProjectListEvent
    data object DismissDialog : ProjectListEvent
    data class OpenProject(val id: String) : ProjectListEvent
    data class DeleteProject(val id: String) : ProjectListEvent
    data object RefreshProjects : ProjectListEvent
}

enum class ProjectNameDialogMode {
    Create,
    Rename
}

data class ProjectNameDialogState(
    val isVisible: Boolean = false,
    val mode: ProjectNameDialogMode = ProjectNameDialogMode.Create,
    val name: String = "",
    val error: String? = null,
    val targetProjectId: String? = null
)

data class ProjectListState(
    val projects: List<ProjectInfo> = emptyList(),
    val isLoading: Boolean = true,
    val dialogState: ProjectNameDialogState = ProjectNameDialogState()
)

class ProjectListViewModel(
    c: ComponentContext,
    private val navigation: StackNavigation<Route>,
) : ViewModel<ProjectListEvent, ProjectListState>(c) {

    private val repository = ProjectRepository()

    private val _projects = MutableStateFlow<List<ProjectInfo>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    private val _dialogState = MutableStateFlow(ProjectNameDialogState())

    init {
        loadProjects()
    }

    private fun loadProjects() {
        backgroundScope.launch {
            _isLoading.value = true
            _projects.value = repository.loadProjectList()
            _isLoading.value = false
        }
    }

    @OptIn(DelicateDecomposeApi::class)
    private fun createNewProject(name: String) {
        backgroundScope.launch {
            _isLoading.value = true
            val project = repository.createNewProject(name)
            _projects.value = (_projects.value + ProjectInfo(project.id, project.name))
                .distinctBy { it.id }
                .sortedBy { it.name }
            _isLoading.value = false
            mainScope.launch {
                navigation.push(Route.Editor(projectId = project.id))
            }
        }
    }

    @OptIn(DelicateDecomposeApi::class)
    private fun openProject(id: String) {
        navigation.push(Route.Editor(projectId = id))
    }

    private fun deleteProject(id: String) {
        backgroundScope.launch {
            repository.deleteProject(id)
            loadProjects()
        }
    }

    private fun showCreateDialog() {
        _dialogState.value = ProjectNameDialogState(
            isVisible = true,
            mode = ProjectNameDialogMode.Create,
            name = generateDefaultProjectName()
        )
    }

    private fun showRenameDialog(id: String) {
        val project = _projects.value.firstOrNull { it.id == id } ?: return
        _dialogState.value = ProjectNameDialogState(
            isVisible = true,
            mode = ProjectNameDialogMode.Rename,
            name = project.name,
            targetProjectId = project.id
        )
    }

    private fun updateDialogName(name: String) {
        val current = _dialogState.value
        _dialogState.value = current.copy(name = name, error = null)
    }

    private fun dismissDialog() {
        _dialogState.value = ProjectNameDialogState()
    }

    private fun confirmDialog() {
        val current = _dialogState.value
        val trimmedName = current.name.trim()
        val error = validateProjectName(trimmedName, current.targetProjectId)
        if (error != null) {
            _dialogState.value = current.copy(error = error)
            return
        }
        when (current.mode) {
            ProjectNameDialogMode.Create -> createNewProject(trimmedName)
            ProjectNameDialogMode.Rename -> renameProject(current.targetProjectId, trimmedName)
        }
        dismissDialog()
    }

    private fun renameProject(id: String?, name: String) {
        if (id == null) {
            return
        }
        backgroundScope.launch {
            _isLoading.value = true
            val success = repository.renameProject(id, name)
            if (success) {
                _projects.value = _projects.value
                    .map { project -> if (project.id == id) project.copy(name = name) else project }
                    .sortedBy { it.name }
            }
            _isLoading.value = false
        }
    }

    private fun validateProjectName(name: String, targetId: String?): String? {
        if (name.isBlank()) {
            return "Name can't be empty."
        }
        val duplicate = _projects.value.firstOrNull { project ->
            project.id != targetId && project.name.equals(name, ignoreCase = true)
        }
        return if (duplicate != null) {
            "A project with this name already exists."
        } else {
            null
        }
    }

    private fun generateDefaultProjectName(): String {
        val existingNames = _projects.value.map { it.name.lowercase() }.toSet()
        val baseName = "Untitled"
        if (baseName.lowercase() !in existingNames) {
            return baseName
        }
        var index = 1
        var candidate = "$baseName $index"
        while (candidate.lowercase() in existingNames) {
            index += 1
            candidate = "$baseName $index"
        }
        return candidate
    }

    @Composable
    override fun models(events: Flow<ProjectListEvent>): ProjectListState {
        val projects by _projects.collectAsState()
        val isLoading by _isLoading.collectAsState()
        val dialogState by _dialogState.collectAsState()

        LaunchedEffect(events) {
            events.collect { event ->
                when (event) {
                    ProjectListEvent.ShowCreateDialog -> showCreateDialog()
                    is ProjectListEvent.ShowRenameDialog -> showRenameDialog(event.id)
                    is ProjectListEvent.UpdateDialogName -> updateDialogName(event.name)
                    ProjectListEvent.ConfirmDialog -> confirmDialog()
                    ProjectListEvent.DismissDialog -> dismissDialog()
                    is ProjectListEvent.OpenProject -> openProject(event.id)
                    is ProjectListEvent.DeleteProject -> deleteProject(event.id)
                    is ProjectListEvent.RefreshProjects -> loadProjects()
                }
            }
        }

        return ProjectListState(
            projects = projects,
            isLoading = isLoading,
            dialogState = dialogState
        )
    }
}
