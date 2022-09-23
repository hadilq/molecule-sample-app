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
import com.example.compose_playground.greeting.Greeting
import com.example.compose_playground.greeting.GreetingAction
import com.example.compose_playground.greeting.GreetingPresenter
import com.example.compose_playground.greeting.GreetingState
import com.example.compose_playground.ui.theme.ComposeplaygroundTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

@Stable
data class MainState(
    val stack: PersistentList<MainPageState>,
    val lastAction: MainAction,
)

@Stable
sealed interface MainPageState {
    data class Greeting(val greeting: GreetingState) : MainPageState
}

@Stable
sealed interface MainAction {
    data class LaunchGreeting(val greetingAction: GreetingAction) : MainAction

    sealed interface PopStack : MainAction {
        object Flip : PopStack
        object Flop : PopStack
    }
}

private val initialMainAction: MainAction = MainAction.LaunchGreeting(GreetingAction.Display(0))

/**
 * Because `MainAction.PopStack` shouldn't have properties, we use the flip-flop strategy to avoid
 * recomposition to fall into an infinite loop!
 */
fun ((MainAction) -> Unit).popStack() {
    invoke(MainAction.PopStack.Flip)
    invoke(MainAction.PopStack.Flop)
}

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
                        is RootState.UserFlow ->
                            Main(action = mainAction, s.mainState)
                        else -> {}
                    }
                }
            }
            BackHandler(true) {
                Log.d("MainActivity", "onBackPressed")
                mainAction.popStack()
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
    action: MainAction,
    state: MainState,
    greetingPresenter: @Composable (action: GreetingAction) -> GreetingState = {
        GreetingPresenter(it)
    },
): MainState {
    var stack = state.stack
    val lastAction = state.lastAction
    if (action == initialMainAction && stack.isNotEmpty()) return state
    when (action) {
        is MainAction.LaunchGreeting -> {
            val greeting = greetingPresenter(action = action.greetingAction)
            val main = MainPageState.Greeting(greeting)
            if (stack.isEmpty() || stack.last() != main) {
                stack = stack.add(main)
            }
        }
        is MainAction.PopStack.Flip -> Unit
        is MainAction.PopStack.Flop -> {
            if (lastAction == MainAction.PopStack.Flip && stack.size > 1) {
                stack = stack.removeAt(stack.size - 1)
            }
        }
    }
    Log.d("MainPresenter", "stack $stack action $action previous: $lastAction")
    return MainState(stack, action)
}

/**
 * The view counterpart of `MainPresenter`. Notice it has shorter lifetime than `MainPresenter`.
 */
@Composable
fun Main(action: (MainAction) -> Unit, state: MainState) {
    when (val pageState = state.stack.last()) {
        is MainPageState.Greeting -> Greeting(
            action = {
                when (it) {
                    is GreetingAction.Display -> action(MainAction.LaunchGreeting(it))
                    is GreetingAction.Previous -> {
                        action.popStack()
                    }
                }
            },
            state = pageState.greeting
        )
    }
}
