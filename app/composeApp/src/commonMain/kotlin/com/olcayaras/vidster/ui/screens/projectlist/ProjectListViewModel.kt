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
    data object CreateNewProject : ProjectListEvent
    data class OpenProject(val id: String) : ProjectListEvent
    data class DeleteProject(val id: String) : ProjectListEvent
    data object RefreshProjects : ProjectListEvent
}

data class ProjectListState(
    val projects: List<ProjectInfo> = emptyList(),
    val isLoading: Boolean = true
)

class ProjectListViewModel(
    c: ComponentContext,
    private val navigation: StackNavigation<Route>,
) : ViewModel<ProjectListEvent, ProjectListState>(c) {

    private val repository = ProjectRepository()

    private val _projects = MutableStateFlow<List<ProjectInfo>>(emptyList())
    private val _isLoading = MutableStateFlow(true)

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
    private fun createNewProject() {
        backgroundScope.launch {
            _isLoading.value = true
            val project = repository.createNewProject("Untitled")
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

    @Composable
    override fun models(events: Flow<ProjectListEvent>): ProjectListState {
        val projects by _projects.collectAsState()
        val isLoading by _isLoading.collectAsState()

        LaunchedEffect(events) {
            events.collect { event ->
                when (event) {
                    is ProjectListEvent.CreateNewProject -> createNewProject()
                    is ProjectListEvent.OpenProject -> openProject(event.id)
                    is ProjectListEvent.DeleteProject -> deleteProject(event.id)
                    is ProjectListEvent.RefreshProjects -> loadProjects()
                }
            }
        }

        return ProjectListState(
            projects = projects,
            isLoading = isLoading
        )
    }
}
