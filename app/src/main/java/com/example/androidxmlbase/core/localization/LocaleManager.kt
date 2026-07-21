package com.example.androidxmlbase.core.localization

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

interface AppLocaleApplier {
    fun applyLocales(tag: String)

    fun currentLocaleTags(): String
}

class AppCompatLocaleApplier : AppLocaleApplier {
    override fun applyLocales(tag: String) {
        val locales =
            if (tag.isBlank()) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(tag)
            }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    override fun currentLocaleTags(): String = AppCompatDelegate.getApplicationLocales().toLanguageTags()
}

class LocaleManager(
    private val localeApplier: AppLocaleApplier = AppCompatLocaleApplier(),
) {
    fun setLanguage(language: AppLanguage) {
        localeApplier.applyLocales(language.languageTag)
    }

    fun useSystemLanguage() {
        localeApplier.applyLocales("")
    }

    fun currentLanguage(): AppLanguage? = AppLanguage.findByLanguageTag(localeApplier.currentLocaleTags())
}
