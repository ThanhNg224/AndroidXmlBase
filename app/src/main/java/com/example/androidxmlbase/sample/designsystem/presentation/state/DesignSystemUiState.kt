package com.example.androidxmlbase.sample.designsystem.presentation.state

import com.example.androidxmlbase.core.architecture.UiState
import com.example.androidxmlbase.core.architecture.result.ResultState

data class DesignSystemUiState(
    val demoResult: ResultState<Unit> = ResultState.Loading,
) : UiState
