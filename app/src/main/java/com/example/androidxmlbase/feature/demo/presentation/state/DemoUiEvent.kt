package com.example.androidxmlbase.feature.demo.presentation.state

import com.example.androidxmlbase.core.architecture.UiEvent

sealed interface DemoUiEvent : UiEvent {
    data object IncrementClicked : DemoUiEvent
}
