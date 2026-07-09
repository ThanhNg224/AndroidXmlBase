package com.example.androidxmlbase.core.localization

import com.example.androidxmlbase.core.storage.AppSettingsKeys
import com.example.androidxmlbase.core.storage.SettingsStore
import kotlinx.coroutines.flow.Flow

interface LocaleStore {
    fun observeLanguageCode(): Flow<String>
    suspend fun setLanguageCode(code: String)
}

class SettingsStoreLocaleStore(private val settingsStore: SettingsStore) : LocaleStore {
    override fun observeLanguageCode(): Flow<String> = settingsStore.observe(AppSettingsKeys.LANGUAGE_CODE)
    override suspend fun setLanguageCode(code: String) = settingsStore.set(AppSettingsKeys.LANGUAGE_CODE, code)
}
