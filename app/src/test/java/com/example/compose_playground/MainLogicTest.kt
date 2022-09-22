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
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MainLogicTest {

    @Test
    fun launchMainPresenter() = runTest {
        val expectedAction = MainAction.LaunchGreeting(GreetingAction.Display(10))
        val expectedState = MainState(
            stack = persistentListOf(MainPageState.Greeting(GreetingState.Greeting(11, "page 11"))),
            lastAction = expectedAction
        )
        moleculeFlow(clock = RecompositionClock.Immediate) {
            MainPresenter(
                action = expectedAction,
                state = expectedState,
                greetingPresenter = {
                    greetingPresenterFake(
                        action = GreetingAction.Display(10),
                        result = GreetingState.Greeting(11, "page 11")
                    )
                }
            )
        }
            .test {
                assertEquals(expectedState, awaitItem())
            }
    }

    @Composable
    fun greetingPresenterFake(action: GreetingAction, result: GreetingState): GreetingState {
        return result
    }
}
