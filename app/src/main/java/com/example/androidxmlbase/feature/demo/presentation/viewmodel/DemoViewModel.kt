package com.example.androidxmlbase.feature.demo.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.androidxmlbase.core.architecture.StateViewModel
import com.example.androidxmlbase.feature.demo.domain.usecase.IncrementCounterUseCase
import com.example.androidxmlbase.feature.demo.domain.usecase.ObserveDemoCountUseCase
import com.example.androidxmlbase.feature.demo.domain.usecase.SaveDemoCountUseCase
import com.example.androidxmlbase.feature.demo.presentation.state.DemoUiEffect
import com.example.androidxmlbase.feature.demo.presentation.state.DemoUiEvent
import com.example.androidxmlbase.feature.demo.presentation.state.DemoUiState
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DemoViewModel(
    private val incrementCounter: IncrementCounterUseCase,
    private val observeDemoCount: ObserveDemoCountUseCase,
    private val saveDemoCount: SaveDemoCountUseCase,
) : StateViewModel<DemoUiState, DemoUiEvent, DemoUiEffect>(DemoUiState()) {

    // Real DataStore's first read genuinely suspends, so currentState.count can still be the
    // constructor default while that read is in flight. Gate increments on it to avoid
    // computing from the stale default and clobbering the real persisted value.
    private var isInitialCountLoaded = false

    init {
        viewModelScope.launch {
            val initialCount = observeDemoCount().first()
            setState { copy(count = initialCount) }
            isInitialCountLoaded = true
            observeDemoCount().drop(1).collect { count -> setState { copy(count = count) } }
        }
    }

    override fun onEvent(event: DemoUiEvent) {
        when (event) {
            is DemoUiEvent.IncrementClicked -> onIncrementClicked()
        }
    }

    private fun onIncrementClicked() {
        if (!isInitialCountLoaded) return
        val result = incrementCounter(currentState.count)
        setState { copy(count = result.count) }
        viewModelScope.launch { saveDemoCount(result.count) }
        if (result.capped) {
            sendEffect(DemoUiEffect.ShowToast("Max count reached"))
        }
    }
}
