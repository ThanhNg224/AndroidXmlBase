package com.example.androidxmlbase.feature.designsystem.presentation.viewmodel

import com.example.androidxmlbase.core.architecture.ResultState
import com.example.androidxmlbase.core.architecture.StateViewModel
import com.example.androidxmlbase.core.architecture.UiEffect
import com.example.androidxmlbase.feature.designsystem.presentation.state.DesignSystemUiEvent
import com.example.androidxmlbase.feature.designsystem.presentation.state.DesignSystemUiState

/**
 * Pure UI-state demo for the [ResultState] loading/success/error showcase — no repository/use
 * case, no async work, so `onEvent` sets state synchronously. Parameterized directly with the
 * base [UiEffect] marker (rather than a dedicated `DesignSystemUiEffect`) since this screen never
 * emits a one-shot effect.
 */
class DesignSystemViewModel : StateViewModel<DesignSystemUiState, DesignSystemUiEvent, UiEffect>(DesignSystemUiState()) {

    override fun onEvent(event: DesignSystemUiEvent) {
        when (event) {
            is DesignSystemUiEvent.ShowLoadingClicked -> setState { copy(demoResult = ResultState.Loading) }
            is DesignSystemUiEvent.ShowSuccessClicked -> setState { copy(demoResult = ResultState.Success(Unit)) }
            is DesignSystemUiEvent.ShowErrorClicked -> setState { copy(demoResult = ResultState.Error(SAMPLE_ERROR_MESSAGE)) }
        }
    }

    private companion object {
        const val SAMPLE_ERROR_MESSAGE = "Sample error"
    }
}
