package com.example.androidxmlbase.sample.demo.presentation.state

import com.example.androidxmlbase.core.architecture.UiEffect

sealed interface DemoUiEffect : UiEffect {
    data object ShowMaxCountReached : DemoUiEffect
}
