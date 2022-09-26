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

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.compose_playground.greeting.*
import com.example.compose_playground.ui.theme.ComposeplaygroundTheme
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.*

typealias MainPresenterType = @Composable (states: MutableSharedFlow<MainState>, state: MainState, actions: Flow<MainAction>) -> Unit

@Stable
data class MainState(
    val stack: PersistentList<MainPageState>,
)

@Stable
sealed interface MainPageState {
    data class Greeting(val greeting: GreetingState) : MainPageState
}

@Stable
sealed interface MainAction {
    data class LaunchGreeting(val greetingAction: GreetingAction) : MainAction

    data class PopStack(val id: UUID = UUID.randomUUID()) : MainAction
}

val initialMainAction: MainAction = MainAction.LaunchGreeting(initialGreetingAction)
val initialMainState: MainState = MainState(persistentListOf())

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("MainActivity", "onCreate")
        rootLogic.action(RootAction.Main(initialMainAction))
        val mainAction: (MainAction) -> Unit = { rootLogic.action(RootAction.Main(it)) }
        setContent {
            ComposeplaygroundTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    val state by rootLogic.state.collectAsState()
                    when (@Suppress("UnnecessaryVariable") val s = state) {
                        is RootState.UserFlow -> Main(action = mainAction, s.mainState)
                        else -> {}
                    }
                }
            }
            BackHandler(true) {
                Log.d("MainActivity", "onBackPressed")
                mainAction(MainAction.PopStack())
            }
        }
    }
}

/**
 * The logic of `Main`. Notice it has aa longer lifetime than `Main`. It's lifecycle must be
 * equivalent of the lifecycle of `ViewModel` of the `MainActivity`.
 */
@Composable
fun MainPresenter(
    downstreamStates: MutableSharedFlow<MainState>,
    state: MainState,
    upstreamActions: Flow<MainAction>,
    greetingPresenter: GreetingPresenterType = { ss, a -> GreetingPresenter(ss, a) },
) {
    rememberActionFlow(upstreamActions = upstreamActions) { action ->
        /**
         * On Configuration change, the mainAction is `initialMainAction`, but stack may not be empty,
         * so we should ignore the action.
         */
        if (action == initialMainAction && state.stack.isNotEmpty()) return@rememberActionFlow
        Log.d("MainPresenter", "state $state action: $action")
        when (action) {
            is MainAction.LaunchGreeting -> {
                fun mapState(greetingState: GreetingState) =
                    MainState(state.stack.add(MainPageState.Greeting(greetingState)))

                fun mapAction(launchGreeting: MainAction.LaunchGreeting) =
                    launchGreeting.greetingAction

                rememberStateAction(
                    downstreamStates = downstreamStates,
                    state = state,
                    action = action,
                    mapState = ::mapState,
                    mapAction = ::mapAction
                ) { greetingStateFlow, greetingActionFlow ->
                    Log.d("MainPresenter", "MainAction.LaunchGreeting is called")
                    greetingPresenter(states = greetingStateFlow, actions = greetingActionFlow)
                }
            }
            is MainAction.PopStack -> {
                LaunchedEffect(action) {
                    Log.d(
                        "MainPresenter",
                        "LaunchedEffect: MainAction.PopStack stack ${state.stack}"
                    )
                    val stack = if (state.stack.size > 1) {
                        state.stack.removeAt(state.stack.size - 1)
                    } else state.stack
                    Log.d(
                        "MainPresenter",
                        "LaunchedEffect: MainAction.PopStack stack after: $stack"
                    )
                    downstreamStates.emit(MainState(stack))
                }
            }
        }
    }
}

/**
 * The view counterpart of `MainPresenter`. Notice it has shorter lifetime than `MainPresenter`.
 */
@Composable
fun Main(action: (MainAction) -> Unit, state: MainState) {
    if (state.stack.isEmpty()) return
    Log.d("Main", "is called")
    when (val pageState = state.stack.last()) {
        is MainPageState.Greeting -> Greeting(
            action = {
                when (it) {
                    is GreetingAction.Display -> action(MainAction.LaunchGreeting(it))
                    is GreetingAction.Previous -> {
                        action(MainAction.PopStack())
                    }
                }
            },
            state = pageState.greeting
        )
    }
}
