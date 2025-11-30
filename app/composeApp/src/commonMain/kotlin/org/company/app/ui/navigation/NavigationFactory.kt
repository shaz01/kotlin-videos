package org.company.app.ui.navigation

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import kotlinx.serialization.KSerializer
import org.company.app.ViewModel

interface NavigationFactory<Route : Any> {
    val initialRoute: Route
    val kSerializer: KSerializer<Route>

    fun createChild(route: Route, componentContext: ComponentContext, navigation: StackNavigation<Route>): ViewModel<*, *>

    @Composable
    fun childContent(viewModel: ViewModel<*,*>)
}
