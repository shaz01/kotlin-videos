package org.company.app

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.MainScope
import org.koin.core.component.KoinComponent

abstract class ViewModel<Event, Model>(
    val ctx: ComponentContext
) : MoleculePresenter<Event, Model>(), ComponentContext by ctx, KoinComponent {
    internal val mainScope by lazy { MainScope() }
}