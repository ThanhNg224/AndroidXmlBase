package com.example.androidxmlbase.feature.demo.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.androidxmlbase.core.network.AndroidConnectivityChecker
import com.example.androidxmlbase.core.network.ApiConfig
import com.example.androidxmlbase.core.network.NetworkModule
import com.example.androidxmlbase.core.network.NoOpAuthTokenProvider
import com.example.androidxmlbase.core.network.RetrofitApiClient
import com.example.androidxmlbase.core.storage.SettingsStore
import com.example.androidxmlbase.feature.demo.data.datasource.DemoApiService
import com.example.androidxmlbase.feature.demo.data.datasource.DemoRemoteDataSourceImpl
import com.example.androidxmlbase.feature.demo.data.repository.DemoRepositoryImpl
import com.example.androidxmlbase.feature.demo.domain.usecase.FetchDemoMessageUseCase
import com.example.androidxmlbase.feature.demo.domain.usecase.IncrementCounterUseCase
import com.example.androidxmlbase.feature.demo.domain.usecase.ObserveDemoCountUseCase
import com.example.androidxmlbase.feature.demo.domain.usecase.SaveDemoCountUseCase

class DemoViewModelFactory(
    private val context: Context,
    private val settingsStore: SettingsStore,
    private val incrementCounter: IncrementCounterUseCase = IncrementCounterUseCase(),
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Placeholder base URL — replace with the real backend before building a product on this base.
        val apiConfig = ApiConfig(baseUrl = "https://example.com/", enableLogging = false)
        val retrofit = NetworkModule.createRetrofit(
            config = apiConfig,
            authTokenProvider = NoOpAuthTokenProvider(),
            connectivityChecker = AndroidConnectivityChecker(context),
        )
        val apiService = retrofit.create(DemoApiService::class.java)
        val remoteDataSource = DemoRemoteDataSourceImpl(apiService, RetrofitApiClient())
        val repository = DemoRepositoryImpl(settingsStore, remoteDataSource)
        return DemoViewModel(
            incrementCounter = incrementCounter,
            observeDemoCount = ObserveDemoCountUseCase(repository),
            saveDemoCount = SaveDemoCountUseCase(repository),
            fetchDemoMessage = FetchDemoMessageUseCase(repository),
        ) as T
    }
}
