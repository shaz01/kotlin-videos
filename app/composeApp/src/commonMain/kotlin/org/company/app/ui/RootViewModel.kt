package org.company.app.ui.entrypoint

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.value.operator.map
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import org.company.app.ui.Route
import org.company.app.ui.navigation.getChildStack
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.core.component.KoinComponent

class RootViewModel(c: ComponentContext) : ComponentContext by c, KoinComponent, BackHandlerOwner {
    private val navigation = StackNavigation<Route>()
    val childStack = getChildStack(navigation, Route)

    val activeRoute = childStack.map { it.active.configuration }

    init {
        Napier.base(DebugAntilog())
    }

    fun onBackClicked(ifFail: (() -> Unit)? = null) {
        navigation.pop(
            onComplete = { success -> if (!success) ifFail?.invoke() }
        )
    }
}