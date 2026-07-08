package com.example.androidxmlbase.feature.demo.presentation.state

import com.example.androidxmlbase.core.architecture.UiEffect

sealed interface DemoUiEffect : UiEffect {
    data class ShowToast(val message: String) : DemoUiEffect
}
