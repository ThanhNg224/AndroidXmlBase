package com.example.androidxmlbase.core.localization

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.LocaleManagerCompat
import androidx.core.os.LocaleListCompat

/**
 * Process-wide [Context] captured by `core.startup.LocaleContextInitializer` at app startup.
 *
 * [AppCompatDelegate.getApplicationLocales] only works while at least one [AppCompatDelegate] is
 * alive (it looks up the framework `LocaleManager` via an active delegate's `Context` on API 33+
 * — see `androidx.appcompat.app.AppCompatDelegate.getLocaleManagerForApplication`); with none
 * alive, it silently returns an empty locale list even though the per-app locale is actually
 * set. Reading via this captured application [Context] instead
 * ([androidx.core.app.LocaleManagerCompat.getApplicationLocales]) works regardless of whether any
 * Activity is currently alive.
 */
internal object LocaleAppContext {
    @Volatile
    var applicationContext: Context? = null
}

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

    override fun currentLocaleTags(): String {
        val context = LocaleAppContext.applicationContext
        return if (context != null) {
            LocaleManagerCompat.getApplicationLocales(context).toLanguageTags()
        } else {
            AppCompatDelegate.getApplicationLocales().toLanguageTags()
        }
    }
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
