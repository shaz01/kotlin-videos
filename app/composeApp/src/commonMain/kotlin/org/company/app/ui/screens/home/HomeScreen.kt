package org.company.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun HomeScreen(
    model: HomeState,
    take: (HomeEvent) -> Unit,
) {
    HomeScreen(
        name = model.name,
        onNameChanged = { take(HomeEvent.SetName(it)) },
        navigate = { take(HomeEvent.NavigateToDetailScreen) }
    )
}


@Composable
fun HomeScreen(
    name: String,
    onNameChanged: (String) -> Unit,
    navigate: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        TextField(
            value = name,
            onValueChange = onNameChanged,
        )
        Button(onClick = navigate) {
            Text("Search")
        }
    }
}