@file:OptIn(ExperimentalDecomposeApi::class)

package com.olcayaras.vidster.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandler
import com.olcayaras.vidster.ViewModel


@Composable
fun <Route : Any> NavigationContent(
    modifier: Modifier = Modifier,
    stack: Value<ChildStack<Route, ViewModel<*, *>>>,
    navigationFactory: NavigationFactory<Route>,
    backHandler: BackHandler,
    onBack: (() -> Unit),
) {
    Scaffold(
        modifier = modifier,
        content = { padding ->
            Children(
                modifier = Modifier.fillMaxSize(),
                stack = stack,
                animation = predictiveBackAnimation(
                    backHandler = backHandler,
                    fallbackAnimation = stackAnimation(slide() + fade()),
                    onBack = onBack
                ),
                content = {
                    navigationFactory.childContent(it.instance)
                },
            )
        }
    )
}

