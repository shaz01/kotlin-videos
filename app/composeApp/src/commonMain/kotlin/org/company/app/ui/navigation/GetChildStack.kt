package org.company.app.ui.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import org.company.app.ViewModel

/**
 * A convenience function for ViewModels to create child stacks.
 */
fun <T : Any> ComponentContext.getChildStack(
    navigation: StackNavigation<T>,
    factory: NavigationFactory<T>
): Value<ChildStack<T, ViewModel<*, *>>> {
    return childStack(
        source = navigation,
        serializer = factory.kSerializer,
        initialConfiguration = factory.initialRoute,
        handleBackButton = true,
        childFactory = { route: T, ctx: ComponentContext ->
            factory.createChild(route, ctx, navigation)
        },
    )
}
