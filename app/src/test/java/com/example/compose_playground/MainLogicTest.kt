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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import app.cash.molecule.RecompositionClock
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import com.example.compose_playground.greeting.GreetingAction
import com.example.compose_playground.greeting.GreetingState
import com.example.compose_playground.greeting.givenGreetingAction
import com.example.compose_playground.greeting.givenGreetingState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MainLogicTest {

    @Test
    fun launchMainPresenter() = runTest {
        val downstreamStates = MutableSharedFlow<MainState>()
        val upstreamActions = Channel<MainAction>()
        moleculeFlow(clock = RecompositionClock.Immediate) {
            MainPresenter(
                downstreamStates = downstreamStates,
                state = initialMainState,
                upstreamActions = upstreamActions.receiveAsFlow(),
                greetingPresenter = { ss, a -> greetingPresenterFake(ss, a) }
            )
            initialMainState
        }
            .test {
                assertEquals(initialMainState, awaitItem())
                downstreamStates.test {
                    upstreamActions.send(givenMainAction)
                    assertEquals(givenMainState, awaitItem())
                }
            }
    }

    @Composable
    fun greetingPresenterFake(
        states: MutableSharedFlow<GreetingState>,
        actions: Flow<GreetingAction>,
    ) {
        LaunchedEffect(actions) {
            actions.collect {
                states.emit(givenGreetingState)
            }
        }
    }

}

val givenMainAction = MainAction.LaunchGreeting(givenGreetingAction)
val givenMainState =
    MainState(stack = persistentListOf(MainPageState.Greeting(givenGreetingState)))
