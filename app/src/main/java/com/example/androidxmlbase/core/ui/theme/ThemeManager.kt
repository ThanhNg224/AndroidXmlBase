package com.example.androidxmlbase.core.ui.theme

import androidx.appcompat.app.AppCompatDelegate
import com.example.androidxmlbase.core.storage.settings.AppSettingsKeys
import com.example.androidxmlbase.core.storage.settings.SettingsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface ThemeManager {
    val currentTheme: Flow<AppTheme>

    suspend fun getTheme(): AppTheme

    suspend fun setTheme(theme: AppTheme)

    fun applyTheme(theme: AppTheme)
}

@Singleton
class AndroidThemeManager
    @Inject
    constructor(
        private val settingsStore: SettingsStore,
    ) : ThemeManager {
        override val currentTheme: Flow<AppTheme> =
            settingsStore
                .observe(AppSettingsKeys.THEME_MODE)
                .map { AppTheme.fromKey(it) }

        override suspend fun getTheme(): AppTheme {
            val key = settingsStore.get(AppSettingsKeys.THEME_MODE)
            return AppTheme.fromKey(key)
        }

        override suspend fun setTheme(theme: AppTheme) {
            settingsStore.set(AppSettingsKeys.THEME_MODE, theme.key)
            applyTheme(theme)
        }

        override fun applyTheme(theme: AppTheme) {
            val nightMode =
                when (theme) {
                    AppTheme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                    AppTheme.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                    AppTheme.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
            AppCompatDelegate.setDefaultNightMode(nightMode)
        }
    }
