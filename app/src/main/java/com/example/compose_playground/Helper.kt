package com.example.compose_playground

import androidx.compose.runtime.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Observer upstream of actions and notify the [block] with the new action.
 */
@Composable
fun <A : Any> rememberActionFlow(
    upstreamActions: Flow<A>,
    block: @Composable (A) -> Unit,
) {
    var rememberedAction: A? by remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        upstreamActions.collect { action ->
            rememberedAction = action
        }
    }

    rememberedAction?.let { action ->
        block(action)
    }
}

/**
 * Prepare a [MutableSharedFlow] for a nested composable, to map and pass the upstream actions.
 */
@Composable
fun <A : Any, NA : Any> rememberNestedActionFlow(
    action: A,
    mapAction: (A) -> NA,
    block: @Composable (MutableSharedFlow<NA>) -> Unit,
) {
    val nestedActionFlow = remember { MutableSharedFlow<NA>() }
    block(nestedActionFlow)
    LaunchedEffect(action) {
        nestedActionFlow.emit(mapAction(action))
    }
}

/**
 * Prepare a [MutableSharedFlow] for a nested composable, to map and propagate the downstream states.
 */
@Composable
fun <S : Any, NS : Any> rememberNestedStateFlow(
    downstreamStates: MutableSharedFlow<S>,
    state: S,
    mapState: (NS) -> S,
    block: @Composable (MutableSharedFlow<NS>) -> Unit,
) {
    val stateFlow = remember { MutableSharedFlow<NS>() }

    LaunchedEffect(state) {
        stateFlow.collect { s ->
            downstreamStates.emit(mapState(s))
        }
    }

    block(stateFlow)
}

/**
 * Prepare two [MutableSharedFlow] for a nested composable, by using [rememberNestedActionFlow],
 * and [rememberNestedStateFlow].
 */
@Composable
fun <S : Any, NS : Any, A : Any, NA : Any> rememberStateAction(
    downstreamStates: MutableSharedFlow<S>,
    state: S,
    action: A,
    mapState: (NS) -> S,
    mapAction: (A) -> NA,
    block: @Composable (MutableSharedFlow<NS>, MutableSharedFlow<NA>) -> Unit,
) {
    rememberNestedStateFlow(
        downstreamStates = downstreamStates,
        state = state,
        mapState = mapState
    ) { nestedStateFlow ->
        rememberNestedActionFlow(
            action = action,
            mapAction = mapAction
        ) { nestedActionFlow ->
            block(nestedStateFlow, nestedActionFlow)
        }
    }
}
