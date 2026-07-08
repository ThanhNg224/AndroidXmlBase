package com.example.androidxmlbase.feature.demo.data.repository

import com.example.androidxmlbase.core.storage.SettingsKey
import com.example.androidxmlbase.core.storage.SettingsStore
import com.example.androidxmlbase.feature.demo.domain.repository.DemoRepository
import kotlinx.coroutines.flow.Flow

class DemoRepositoryImpl(
    private val settingsStore: SettingsStore,
) : DemoRepository {

    override fun observeCount(): Flow<Int> {
        return settingsStore.observe(DEMO_COUNTER_COUNT)
    }

    override suspend fun saveCount(count: Int) {
        settingsStore.set(DEMO_COUNTER_COUNT, count)
    }

    private companion object {
        val DEMO_COUNTER_COUNT = SettingsKey.IntKey(name = "demo_counter_count", defaultValue = 0)
    }
}
