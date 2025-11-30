package org.company.app.utils

import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

fun Lifecycle.coroutineScope(dispatcher: CoroutineDispatcher): CoroutineScope {
  val scope = CoroutineScope(SupervisorJob() + dispatcher)

  if (state != Lifecycle.State.DESTROYED) {
    doOnDestroy {
      scope.cancel()
    }
  } else {
    scope.cancel()
  }

  return scope
}