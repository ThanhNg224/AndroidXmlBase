package com.example.androidxmlbase.feature.settings.presentation.state

import com.example.androidxmlbase.core.architecture.UiEvent
import com.example.androidxmlbase.core.localization.AppLanguage
import com.example.androidxmlbase.core.ui.theme.AppTheme

sealed interface SettingsUiEvent : UiEvent {
    data class ThemeSelected(
        val theme: AppTheme,
    ) : SettingsUiEvent

    data class LanguageSelected(
        val language: AppLanguage?,
    ) : SettingsUiEvent
}
