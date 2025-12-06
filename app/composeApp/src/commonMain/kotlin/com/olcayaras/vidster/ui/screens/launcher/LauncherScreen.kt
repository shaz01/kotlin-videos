package com.olcayaras.vidster.ui.screens.launcher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Plus
import compose.icons.feathericons.MoreVertical
import org.jetbrains.compose.ui.tooling.preview.Preview

data class Project(
    val id: String,
    val name: String,
    val description: String,
    val lastModified: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherScreen(
    modifier: Modifier = Modifier,
    projects: List<Project> = emptyList(),
    onProjectClick: (Project) -> Unit = {},
    onCreateNewProject: () -> Unit = {},
    onProjectMenuClick: (Project) -> Unit = {}
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Compostic",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateNewProject,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = FeatherIcons.Plus,
                    contentDescription = "Create New Project"
                )
            }
        }
    ) { paddingValues ->
        if (projects.isEmpty()) {
            EmptyProjectsView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            ProjectList(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                projects = projects,
                onProjectClick = onProjectClick,
                onProjectMenuClick = onProjectMenuClick
            )
        }
    }
}

@Composable
private fun EmptyProjectsView(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "No projects yet",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Create your first project to get started",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProjectList(
    modifier: Modifier = Modifier,
    projects: List<Project>,
    onProjectClick: (Project) -> Unit,
    onProjectMenuClick: (Project) -> Unit
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(projects) { project ->
            ProjectCard(
                project = project,
                onClick = { onProjectClick(project) },
                onMenuClick = { onProjectMenuClick(project) }
            )
        }
    }
}

@Composable
private fun ProjectCard(
    project: Project,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = project.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Modified: ${project.lastModified}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = FeatherIcons.MoreVertical,
                    contentDescription = "Project Menu"
                )
            }
        }
    }
}

@Preview
@Composable
fun LauncherScreenPreview() {
    val sampleProjects = listOf(
        Project(
            id = "1",
            name = "Welcome Video",
            description = "An introduction video for new users",
            lastModified = "2 hours ago"
        ),
        Project(
            id = "2",
            name = "Product Demo",
            description = "Showcase of product features",
            lastModified = "Yesterday"
        ),
        Project(
            id = "3",
            name = "Tutorial Series",
            description = "Step-by-step guide for beginners",
            lastModified = "Last week"
        )
    )

    LauncherScreen(
        modifier = Modifier.fillMaxSize(),
        projects = sampleProjects
    )
}

@Preview
@Composable
fun LauncherScreenEmptyPreview() {
    LauncherScreen(
        modifier = Modifier.fillMaxSize(),
        projects = emptyList()
    )
}