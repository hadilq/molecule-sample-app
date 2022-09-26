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

import app.cash.molecule.RecompositionClock
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GreetingLogicTest {

    @Test
    fun launchMainPresenter() = runTest {
        val downstreamStates = MutableSharedFlow<GreetingState>()
        val upstreamActions = Channel<GreetingAction>()
        moleculeFlow(clock = RecompositionClock.Immediate) {
            GreetingPresenter(
                downstreamStates = downstreamStates,
                upstreamActions = upstreamActions.receiveAsFlow(),
            )
            initialGreetingState
        }
            .test {
                assertEquals(initialGreetingState, awaitItem())
                downstreamStates.test {
                    upstreamActions.send(givenGreetingAction)
                    assertEquals(givenGreetingState, awaitItem())
                }
            }
    }
}

val givenGreetingState = GreetingState.Greeting(11, "page 11")
val givenGreetingAction = GreetingAction.Display(10)
