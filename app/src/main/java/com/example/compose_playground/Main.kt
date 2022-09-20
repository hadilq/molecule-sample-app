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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.cash.molecule.RecompositionClock
import app.cash.molecule.launchMolecule
import com.example.compose_playground.greeting.Greeting
import com.example.compose_playground.greeting.GreetingAction
import com.example.compose_playground.greeting.GreetingPresenter
import com.example.compose_playground.greeting.GreetingState
import com.example.compose_playground.ui.theme.ComposeplaygroundTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Stable
sealed interface MainState {
    data class Greeting(val greeting: GreetingState) : MainState
}

sealed interface MainAction {
    val initialAction: MainAction
        get() = LaunchGreeting(GreetingAction.Display(0))

    data class LaunchGreeting(val greetingAction: GreetingAction) : MainAction
    sealed interface PopStack : MainAction {
        object Flip : PopStack
        object Flop : PopStack
    }
}

class MainActivity : ComponentActivity() {

    private val viewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProvider(this)[MainViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("MainActivity", "onCreate")
        setContent {
            ComposeplaygroundTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    val state by viewModel.state.collectAsState()
                    Main(action = { viewModel.action(it) }, state)
                }
            }
            BackHandler(true) {
                Log.d("MainActivity", "onBackPressed")
                viewModel.action(MainAction.PopStack.Flip)
                viewModel.action(MainAction.PopStack.Flop)
            }
        }
    }
}

class MainViewModel : ViewModel() {

    private val _action: MutableSharedFlow<MainAction> = MutableSharedFlow()

    val state: StateFlow<MainState> =
        viewModelScope.launchMolecule(clock = RecompositionClock.Immediate) {
            val action by _action.collectAsState(
                initial = MainAction.LaunchGreeting(GreetingAction.Display(0))
            )
            Log.d("MainViewModel.state", "action $action")
            MainPresenter(action = action)
        }

    val action: (MainAction) -> Unit = { a ->
        Log.d("MainViewModel.action", "action $a")
        viewModelScope.launch {
            _action.emit(a)
        }
    }
}

@Composable
fun MainPresenter(action: MainAction): MainState {
    var stack by remember { mutableStateOf(persistentListOf<MainState>()) }
    var lastAction: MainAction by remember {
        mutableStateOf(
            MainAction.LaunchGreeting(
                GreetingAction.Display(
                    0
                )
            )
        )
    }
    when (@Suppress("UnnecessaryVariable") val a = action) {
        is MainAction.LaunchGreeting -> {
            val greeting = GreetingPresenter(action = a.greetingAction)
            val main = MainState.Greeting(greeting)
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
    Log.d("MainPresenter", "stack $stack action $action")
    lastAction = action
    return stack.last()
}


@Composable
fun Main(action: (MainAction) -> Unit, state: MainState) {
    when (state) {
        is MainState.Greeting -> Greeting(
            action = {
                when (it) {
                    is GreetingAction.Display -> action(MainAction.LaunchGreeting(it))
                    is GreetingAction.Previous -> {
                        action(MainAction.PopStack.Flip)
                        action(MainAction.PopStack.Flop)
                    }
                }
            },
            state = state.greeting
        )
    }
}
