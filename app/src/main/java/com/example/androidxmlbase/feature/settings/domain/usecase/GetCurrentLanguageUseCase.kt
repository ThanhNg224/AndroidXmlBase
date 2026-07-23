package com.example.androidxmlbase.feature.settings.domain.usecase

import com.example.androidxmlbase.core.localization.AppLanguage
import com.example.androidxmlbase.feature.settings.domain.repository.SettingsRepository
import javax.inject.Inject

class GetCurrentLanguageUseCase
    @Inject
    constructor(
        private val repository: SettingsRepository,
    ) {
        operator fun invoke(): AppLanguage? = repository.getCurrentLanguage()
    }
