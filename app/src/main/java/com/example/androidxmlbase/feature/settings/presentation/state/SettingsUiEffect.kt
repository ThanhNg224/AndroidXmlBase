package com.example.androidxmlbase.feature.settings.presentation.state

import com.example.androidxmlbase.core.architecture.UiEffect
import com.example.androidxmlbase.core.localization.AppLanguage

sealed interface SettingsUiEffect : UiEffect {
    data class ApplyLanguage(
        val language: AppLanguage?,
    ) : SettingsUiEffect
}
