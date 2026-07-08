package com.example.androidxmlbase.feature.demo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.androidxmlbase.feature.demo.domain.usecase.IncrementCounterUseCase

class DemoViewModelFactory(
    private val incrementCounter: IncrementCounterUseCase = IncrementCounterUseCase(),
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DemoViewModel(incrementCounter) as T
    }
}
