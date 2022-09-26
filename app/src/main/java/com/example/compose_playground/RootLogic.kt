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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

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

val initialRootAction = RootAction.Launch
val initialRootState = RootState.ServiceFlow()

/**
 * Singleton root of app's logic tree.
 */
val rootLogic: RootLogic = RootLogicImpl()

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
    private val _state: MutableStateFlow<RootState> = MutableStateFlow(initialRootState)

    override val scope: RootLogic.Scope =
//        RootLogic.Scope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))
        RootLogic.Scope(SupervisorJob() + Dispatchers.Main.immediate)

    override val state: StateFlow<RootState> = _state

    private val molecule: StateFlow<RootState> =
        scope.launchMolecule(clock = RecompositionClock.Immediate) {
            val states: MutableSharedFlow<RootState> = remember { MutableSharedFlow() }
            var rootState: RootState by remember { mutableStateOf(initialRootState) }
            RootPresenter(downstreamStates = states, state = rootState, upstreamActions = _action)
            LaunchedEffect(Unit) {
                states.collect {
                    _state.value = it
                    rootState = it
                }
            }
            initialRootState
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
    downstreamStates: MutableSharedFlow<RootState>,
    state: RootState,
    upstreamActions: Flow<RootAction>,
    mainPresenter: MainPresenterType = { ss, s, a -> MainPresenter(ss, s, a) },
) {
    rememberActionFlow(upstreamActions = upstreamActions) { action ->
        when (action) {
            is RootAction.Launch -> {
                LaunchedEffect(Unit) { downstreamStates.emit(RootState.ServiceFlow()) }
            }
            is RootAction.Main -> {
                fun mapState(mainState: MainState) = RootState.UserFlow(mainState)
                fun mapAction(rooAction: RootAction.Main) = rooAction.mainAction
                rememberStateAction(
                    downstreamStates = downstreamStates,
                    state = state,
                    action = action,
                    mapState = ::mapState,
                    mapAction = ::mapAction,
                ) { mainStateFlow, mainActionFlow ->
                    mainPresenter(
                        states = mainStateFlow,
                        state = (state as? RootState.UserFlow)?.mainState ?: initialMainState,
                        actions = mainActionFlow
                    )
                }
            }
        }
    }
}
