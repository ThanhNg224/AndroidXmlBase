package com.example.androidxmlbase.sample.demo.data.repository

import com.example.androidxmlbase.core.architecture.result.DomainResult
import com.example.androidxmlbase.core.storage.settings.SettingsKey
import com.example.androidxmlbase.core.storage.settings.SettingsStore
import com.example.androidxmlbase.sample.demo.data.datasource.DemoRemoteDataSource
import com.example.androidxmlbase.sample.demo.data.mapper.toDomainResult
import com.example.androidxmlbase.sample.demo.domain.model.DemoWeather
import com.example.androidxmlbase.sample.demo.domain.repository.DemoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DemoRepositoryImpl
    @Inject
    constructor(
        private val settingsStore: SettingsStore,
        private val remoteDataSource: DemoRemoteDataSource,
    ) : DemoRepository {
        override fun observeCount(): Flow<Int> = settingsStore.observe(DEMO_COUNTER_COUNT)

        override suspend fun saveCount(count: Int) {
            settingsStore.set(DEMO_COUNTER_COUNT, count)
        }

        override suspend fun fetchWeather(): DomainResult<DemoWeather> = remoteDataSource.fetchCurrentWeather().toDomainResult()

        private companion object {
            val DEMO_COUNTER_COUNT = SettingsKey.IntKey(name = "demo_counter_count", defaultValue = 0)
        }
    }
