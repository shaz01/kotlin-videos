package com.olcayaras.vidster.ui.screens.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun DetailScreen(
    model: DetailState,
    take: (DetailEvent) -> Unit,
) {
    DetailScreen(
        name = model.name,
        details = model.details,
        navigateBack = { take(DetailEvent.NavigateBack) }
    )
}

@Composable
fun DetailScreen(
    name: String,
    details: String,
    navigateBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Details for name $name: $details")
        Button(onClick = navigateBack) {
            Text("Back to home screen")
        }
    }
}