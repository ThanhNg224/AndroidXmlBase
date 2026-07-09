package com.example.androidxmlbase.feature.demo.presentation.state

sealed interface DemoMessageState {
    data object Loading : DemoMessageState

    data class Success(
        val message: String,
    ) : DemoMessageState

    data class Error(
        val reason: DemoMessageError,
    ) : DemoMessageState
}

enum class DemoMessageError {
    SERVER,
    NO_CONNECTION,
    UNEXPECTED_RESPONSE,
    EMPTY_RESPONSE,
}
