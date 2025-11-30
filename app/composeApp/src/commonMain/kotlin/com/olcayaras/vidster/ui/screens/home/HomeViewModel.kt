package com.olcayaras.vidster.ui.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import com.olcayaras.vidster.ViewModel
import com.olcayaras.vidster.ui.Route

sealed interface HomeEvent {
    data class SetName(val name: String) : HomeEvent
    object NavigateToDetailScreen : HomeEvent
}

data class HomeState(
    val name: String,
)

class HomeViewModel(c: ComponentContext, private val navigation: StackNavigation<Route>) : ViewModel<HomeEvent, HomeState>(c) {
    private val _name = MutableStateFlow("")

    fun setName(name: String) {
        _name.value = name
    }

    fun navigateToDetailScreen() {
        navigation.bringToFront(Route.Detail(_name.value))
    }

    @Composable
    override fun models(events: Flow<HomeEvent>): HomeState {
        val name by _name.collectAsState()
        LaunchedEffect(events) {
            events.collect {
                when (it) {
                    is HomeEvent.SetName -> setName(it.name)
                    HomeEvent.NavigateToDetailScreen -> navigateToDetailScreen()
                }
            }
        }

        return HomeState(name)
    }
}