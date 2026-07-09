package com.example.androidxmlbase.feature.designsystem.presentation.state

import com.example.androidxmlbase.core.architecture.ResultState
import com.example.androidxmlbase.core.architecture.UiState

data class DesignSystemUiState(
    val demoResult: ResultState<Unit> = ResultState.Loading,
) : UiState
