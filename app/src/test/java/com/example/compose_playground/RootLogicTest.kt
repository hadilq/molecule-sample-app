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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
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
        val downstreamStates = MutableSharedFlow<RootState>()
        val upstreamActions = Channel<RootAction>()
        moleculeFlow(clock = RecompositionClock.Immediate) {
            RootPresenter(
                downstreamStates = downstreamStates,
                state = givenRootState,
                upstreamActions = upstreamActions.receiveAsFlow(),
                mainPresenter = { ss, _, a -> mainPresenterFake(ss, a) }
            )
            initialRootState
        }
            .test {
                assertEquals(initialRootState, awaitItem())
                downstreamStates.test {
                    upstreamActions.send(givenRootAction)
                    assertEquals(givenRootState, awaitItem())
                }
            }
    }

    @Composable
    fun mainPresenterFake(
        downstreamStates: MutableSharedFlow<MainState>,
        upstreamActions: Flow<MainAction>,
    ) {
        LaunchedEffect(upstreamActions) {
            upstreamActions.collect {
                downstreamStates.emit(givenMainState)
            }
        }
    }
}

val givenRootState = RootState.UserFlow(givenMainState)
val givenRootAction = RootAction.Main(givenMainAction)
