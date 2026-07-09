package com.example.androidxmlbase.feature.demo.data.repository

import com.example.androidxmlbase.core.architecture.ResultState
import com.example.androidxmlbase.core.storage.SettingsKey
import com.example.androidxmlbase.core.storage.SettingsStore
import com.example.androidxmlbase.feature.demo.data.datasource.DemoRemoteDataSource
import com.example.androidxmlbase.feature.demo.data.mapper.toResultState
import com.example.androidxmlbase.feature.demo.domain.repository.DemoRepository
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

        override suspend fun fetchMessage(): ResultState<String> = remoteDataSource.fetchMessage().toResultState()

        private companion object {
            val DEMO_COUNTER_COUNT = SettingsKey.IntKey(name = "demo_counter_count", defaultValue = 0)
        }
    }
