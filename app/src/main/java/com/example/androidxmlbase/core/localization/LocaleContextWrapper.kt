package com.example.androidxmlbase.core.localization

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleContextWrapper {
    fun wrap(
        context: Context,
        languageCode: String,
    ): Context {
        if (languageCode.isBlank()) return context
        val tag = LocaleTagMapper.toRegionalTag(languageCode)
        val locale = Locale.forLanguageTag(tag)
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        return context.createConfigurationContext(configuration)
    }
}
