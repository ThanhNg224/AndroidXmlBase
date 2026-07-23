package com.example.androidxmlbase.feature.settings.domain.usecase

import com.example.androidxmlbase.core.ui.theme.AppTheme
import com.example.androidxmlbase.feature.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveThemeUseCase
    @Inject
    constructor(
        private val repository: SettingsRepository,
    ) {
        operator fun invoke(): Flow<AppTheme> = repository.observeTheme()
    }
