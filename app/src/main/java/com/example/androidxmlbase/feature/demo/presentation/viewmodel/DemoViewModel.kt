package com.example.androidxmlbase.feature.demo.presentation.viewmodel

import com.example.androidxmlbase.core.architecture.StateViewModel
import com.example.androidxmlbase.feature.demo.domain.usecase.IncrementCounterUseCase
import com.example.androidxmlbase.feature.demo.presentation.state.DemoUiEffect
import com.example.androidxmlbase.feature.demo.presentation.state.DemoUiEvent
import com.example.androidxmlbase.feature.demo.presentation.state.DemoUiState

class DemoViewModel(
    private val incrementCounter: IncrementCounterUseCase,
) : StateViewModel<DemoUiState, DemoUiEvent, DemoUiEffect>(DemoUiState()) {

    override fun onEvent(event: DemoUiEvent) {
        when (event) {
            is DemoUiEvent.IncrementClicked -> onIncrementClicked()
        }
    }

    private fun onIncrementClicked() {
        val result = incrementCounter(currentState.count)
        setState { copy(count = result.count) }
        if (result.capped) {
            sendEffect(DemoUiEffect.ShowToast("Max count reached"))
        }
    }
}
