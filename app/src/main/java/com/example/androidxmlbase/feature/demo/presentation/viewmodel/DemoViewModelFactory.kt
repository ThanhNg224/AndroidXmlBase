package com.example.androidxmlbase.feature.demo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.androidxmlbase.core.storage.SettingsStore
import com.example.androidxmlbase.feature.demo.data.repository.DemoRepositoryImpl
import com.example.androidxmlbase.feature.demo.domain.usecase.IncrementCounterUseCase
import com.example.androidxmlbase.feature.demo.domain.usecase.ObserveDemoCountUseCase
import com.example.androidxmlbase.feature.demo.domain.usecase.SaveDemoCountUseCase

class DemoViewModelFactory(
    private val settingsStore: SettingsStore,
    private val incrementCounter: IncrementCounterUseCase = IncrementCounterUseCase(),
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repository = DemoRepositoryImpl(settingsStore)
        return DemoViewModel(
            incrementCounter = incrementCounter,
            observeDemoCount = ObserveDemoCountUseCase(repository),
            saveDemoCount = SaveDemoCountUseCase(repository),
        ) as T
    }
}
