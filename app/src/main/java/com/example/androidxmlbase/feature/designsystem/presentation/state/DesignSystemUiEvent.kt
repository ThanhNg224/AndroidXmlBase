package com.example.androidxmlbase.feature.designsystem.presentation.state

import com.example.androidxmlbase.core.architecture.UiEvent

sealed interface DesignSystemUiEvent : UiEvent {
    data object ShowLoadingClicked : DesignSystemUiEvent
    data object ShowSuccessClicked : DesignSystemUiEvent
    data object ShowErrorClicked : DesignSystemUiEvent
}
