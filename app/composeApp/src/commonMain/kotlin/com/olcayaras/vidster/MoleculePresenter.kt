package com.olcayaras.vidster

import androidx.compose.runtime.Composable
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow

abstract class MoleculePresenter<Event, Model> {
    internal val backgroundScope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 20)

    fun take(event: Event) {
        if (!events.tryEmit(event)) {
            error("Event buffer overflow.")
        }
    }

    @Composable
    abstract fun models(events: Flow<Event>): Model

    val models: StateFlow<Model> by lazy(LazyThreadSafetyMode.NONE) {
        backgroundScope.launchMolecule(mode = RecompositionMode.Immediate) {
            models(events)
        }
    }
}