package org.company.app.ui.screens.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.company.app.ViewModel
import org.company.app.network.DetailApi
import org.company.app.ui.Route
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

sealed interface DetailEvent {
    object NavigateBack : DetailEvent
}

data class DetailState(
    val name: String,
    val details: String,
)

class DetailViewModel(
    c: ComponentContext,
    private val navigation: StackNavigation<Route>,
    private val name: String,
) : ViewModel<DetailEvent, DetailState>(c), KoinComponent {
    private val detailApi by inject<DetailApi>()
    private val _details = MutableStateFlow("")

    private fun navigateBack() = navigation.pop()

    @Composable
    override fun models(events: Flow<DetailEvent>): DetailState {
        val details by _details.collectAsState()

        LaunchedEffect(Unit) {
            _details.value = detailApi.getDetails(name)
        }

        LaunchedEffect(events) {
            events.collect {
                when (it) {
                    DetailEvent.NavigateBack -> navigateBack()
                }
            }
        }

        return DetailState(name, details)
    }
}