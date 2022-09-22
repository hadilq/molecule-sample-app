/**
 * Copyright 2022 Hadi Lashkari Ghouchani
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.compose_playground

import android.util.Log
import androidx.compose.runtime.*
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.example.compose_playground.greeting.GreetingAction
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * Singleton root of app's logic tree.
 */
val rootLogic: RootLogic = RootLogicImpl()

@Stable
sealed class RootState {
    abstract val probablyAccountInfo: Any

    data class UserFlow(
        val mainState: MainState,
        override val probablyAccountInfo: Any = Unit
    ) : RootState()

    data class ServiceFlow(
        override val probablyAccountInfo: Any = Unit
    ) : RootState()
}

@Stable
sealed interface RootAction {
    object Launch : RootAction
    data class Main(val mainAction: MainAction) : RootAction
}

interface RootLogic {
    val scope: Scope
    val state: StateFlow<RootState>
    val action: (RootAction) -> Unit

    class Scope(context: CoroutineContext) : CoroutineScope {
        override val coroutineContext: CoroutineContext = context
    }
}

private class RootLogicImpl : RootLogic {

    private val _action: MutableSharedFlow<RootAction> = MutableSharedFlow()

    override val scope: RootLogic.Scope =
        RootLogic.Scope(SupervisorJob() + Dispatchers.Main.immediate)

    override val state: StateFlow<RootState> =
        scope.launchMolecule(clock = RecompositionClock.Immediate) {
            RootPresenter(actions = _action)
        }

    override val action: (RootAction) -> Unit
        get() = { a ->
            scope.launch {
                Log.d("RootCompose", "action: action $a")
                _action.emit(a)
            }
        }

}

/**
 * The logic of root node. Notice it doesn't have any view compose counterpart.
 */
@Composable
fun RootPresenter(
    actions: Flow<RootAction>,
    mainPresenter: @Composable (action: MainAction, state: MainState) -> MainState = { action, state -> MainPresenter(action, state) },
): RootState {
    var lastMainState by remember {
        mutableStateOf(MainState(persistentListOf(), MainAction.LaunchGreeting(GreetingAction.Display(0))))
    }
    val action by actions.collectAsState(initial = RootAction.Launch)
    val a = action
    Log.d("RootPresenter", "action $a")
    return when (a) {
        is RootAction.Launch -> {
            RootState.ServiceFlow()
        }
        is RootAction.Main -> {
            lastMainState = mainPresenter(action = a.mainAction, lastMainState)
            RootState.UserFlow(lastMainState)
        }
    }
}
