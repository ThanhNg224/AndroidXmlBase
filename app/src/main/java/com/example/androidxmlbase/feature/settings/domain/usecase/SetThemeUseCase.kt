package com.example.androidxmlbase.feature.settings.domain.usecase

import com.example.androidxmlbase.core.architecture.UseCase
import com.example.androidxmlbase.core.ui.theme.AppTheme
import com.example.androidxmlbase.feature.settings.domain.repository.SettingsRepository
import javax.inject.Inject

class SetThemeUseCase
    @Inject
    constructor(
        private val repository: SettingsRepository,
    ) : UseCase<AppTheme, Unit> {
        override suspend fun invoke(params: AppTheme) {
            repository.setTheme(params)
        }
    }
