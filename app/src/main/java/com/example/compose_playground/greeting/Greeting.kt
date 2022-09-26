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
package com.example.compose_playground.greeting

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.compose_playground.rememberActionFlow
import com.example.compose_playground.ui.theme.ComposeplaygroundTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

typealias GreetingPresenterType = @Composable (states: MutableSharedFlow<GreetingState>, actions: Flow<GreetingAction>) -> Unit

@Stable

sealed interface GreetingState {
    data class Greeting(
        val count: Int,
        val name: String,
    ) : GreetingState
}

@Stable
sealed interface GreetingAction {
    data class Display(val count: Int) : GreetingAction
    object Previous : GreetingAction
}

val initialGreetingAction = GreetingAction.Display(0)
val initialGreetingState = GreetingState.Greeting(1, "page 1")

@Composable
fun GreetingPresenter(
    downstreamStates: MutableSharedFlow<GreetingState>,
    upstreamActions: Flow<GreetingAction>,
) {
    rememberActionFlow(upstreamActions = upstreamActions) { action ->
        Log.d("GreetingPresenter", "action $action")
        when (action) {
            is GreetingAction.Display -> {
                val count = action.count + 1
                LaunchedEffect(count) {
                    Log.d("GreetingPresenter", "LaunchedEffect: count $count")
                    downstreamStates.emit(GreetingState.Greeting(count, "page $count"))
                }
            }
            is GreetingAction.Previous -> throw IllegalStateException("It should be handled by its parent")
        }
    }
}

@Composable
fun Greeting(action: (GreetingAction) -> Unit, state: GreetingState) {
    when (state) {
        is GreetingState.Greeting -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .padding(5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = "Hello ${state.name}!")
                Button(
                    modifier = Modifier.padding(5.dp),
                    onClick = { action(GreetingAction.Display(state.count)) }) {
                    Column(
                        modifier = Modifier
                            .padding(5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "Next page")
                    }
                }
                Button(
                    modifier = Modifier.padding(5.dp),
                    onClick = { action(GreetingAction.Previous) }) {
                    Column(
                        modifier = Modifier
                            .padding(5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "Previous page")
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ComposeplaygroundTheme {
        Greeting({}, GreetingState.Greeting(0, "Android"))
    }
}
