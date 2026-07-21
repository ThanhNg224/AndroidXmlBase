package com.example.androidxmlbase.feature.demo.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.androidxmlbase.core.architecture.StateViewModel
import com.example.androidxmlbase.core.architecture.result.AppError
import com.example.androidxmlbase.core.architecture.result.DomainResult
import com.example.androidxmlbase.feature.demo.domain.usecase.FetchDemoMessageUseCase
import com.example.androidxmlbase.feature.demo.domain.usecase.IncrementCounterUseCase
import com.example.androidxmlbase.feature.demo.domain.usecase.ObserveDemoCountUseCase
import com.example.androidxmlbase.feature.demo.domain.usecase.SaveDemoCountUseCase
import com.example.androidxmlbase.feature.demo.presentation.state.DemoMessageError
import com.example.androidxmlbase.feature.demo.presentation.state.DemoMessageState
import com.example.androidxmlbase.feature.demo.presentation.state.DemoUiEffect
import com.example.androidxmlbase.feature.demo.presentation.state.DemoUiEvent
import com.example.androidxmlbase.feature.demo.presentation.state.DemoUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DemoViewModel
    @Inject
    constructor(
        private val incrementCounter: IncrementCounterUseCase,
        private val observeDemoCount: ObserveDemoCountUseCase,
        private val saveDemoCount: SaveDemoCountUseCase,
        private val fetchDemoMessage: FetchDemoMessageUseCase,
    ) : StateViewModel<DemoUiState, DemoUiEvent, DemoUiEffect>(DemoUiState()) {
        private var isInitialCountLoaded = false

        init {
            viewModelScope.launch {
                val initialCount = observeDemoCount().first()
                setState { copy(count = initialCount) }
                isInitialCountLoaded = true
                observeDemoCount().drop(1).collect { count -> setState { copy(count = count) } }
            }
            viewModelScope.launch {
                val result = fetchDemoMessage(Unit)
                setState { copy(message = result.toMessageState()) }
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
                sendEffect(DemoUiEffect.ShowMaxCountReached)
            }
        }

        private fun DomainResult<String>.toMessageState(): DemoMessageState =
            when (this) {
                is DomainResult.Success -> DemoMessageState.Success(data)
                is DomainResult.Error -> DemoMessageState.Error(error.toMessageError())
            }

        private fun AppError.toMessageError(): DemoMessageError =
            when (this) {
                is AppError.Http -> DemoMessageError.SERVER
                is AppError.Network -> DemoMessageError.NO_CONNECTION
                is AppError.Parse -> DemoMessageError.UNEXPECTED_RESPONSE
                AppError.EmptyBody -> DemoMessageError.EMPTY_RESPONSE
            }
    }
