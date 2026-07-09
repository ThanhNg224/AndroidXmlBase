package com.example.androidxmlbase.core.localization

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

interface AppLocaleApplier {
    fun applyLocales(tag: String)
}

class AppCompatLocaleApplier : AppLocaleApplier {
    override fun applyLocales(tag: String) {
        val locales = if (tag.isBlank()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)
        AppCompatDelegate.setApplicationLocales(locales)
    }
}

class LocaleManager(
    private val localeStore: LocaleStore,
    private val localeApplier: AppLocaleApplier = AppCompatLocaleApplier(),
) {
    suspend fun setLanguage(languageCode: String) {
        localeStore.setLanguageCode(languageCode)
        val tag = if (languageCode.isBlank()) "" else LocaleTagMapper.toRegionalTag(languageCode)
        localeApplier.applyLocales(tag)
    }
}
