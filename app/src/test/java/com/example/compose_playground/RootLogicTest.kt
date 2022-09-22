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
import app.cash.molecule.RecompositionClock
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import com.example.compose_playground.greeting.GreetingAction
import com.example.compose_playground.greeting.GreetingState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RootLogicTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Test
    fun launchRootPresenter() = runTest {
        val events = Channel<RootAction>()
        val expected = MainState.Greeting(GreetingState.Greeting(1, "page 1"))
        moleculeFlow(clock = RecompositionClock.Immediate) {
            RootPresenter(
                actions = events.receiveAsFlow(),
                mainPresenter = {
                    mainPresenterFake(action = MainAction.PopStack.Flop, result = expected)
                }
            )
        }
            .test {
                assertEquals(RootState.ServiceFlow(), awaitItem())
                events.send(RootAction.Main(MainAction.LaunchGreeting(GreetingAction.Display(0))))
                assertEquals(RootState.UserFlow(MainState.Greeting(GreetingState.Greeting(1, "page 1"))), awaitItem())
            }
    }

    @Composable
    fun mainPresenterFake(action: MainAction, result: MainState): MainState {
        return result
    }
}
