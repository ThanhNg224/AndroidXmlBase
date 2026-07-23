package com.example.androidxmlbase.feature.settings.presentation.state

import com.example.androidxmlbase.core.architecture.UiState
import com.example.androidxmlbase.core.localization.AppLanguage
import com.example.androidxmlbase.core.ui.theme.AppTheme

data class SettingsUiState(
    val theme: AppTheme = AppTheme.SYSTEM,
    val language: AppLanguage? = null,
) : UiState
