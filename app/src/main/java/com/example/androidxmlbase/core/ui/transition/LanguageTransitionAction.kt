package com.example.androidxmlbase.core.ui.transition

import android.os.Bundle
import com.example.androidxmlbase.core.localization.AppLanguage
import com.example.androidxmlbase.core.localization.LocaleManager
import com.example.androidxmlbase.core.ui.util.getTyped
import javax.inject.Inject

class LanguageTransitionAction
    @Inject
    constructor(
        private val localeManager: LocaleManager,
    ) : TransitionAction {
        override suspend fun perform(extras: Bundle) {
            val tag = extras.getTyped(EXTRA_LANGUAGE_TAG, String::class.java)
            AppLanguage.findByLanguageTag(tag.orEmpty())?.let(localeManager::setLanguage)
                ?: localeManager.useSystemLanguage()
        }

        companion object {
            const val KEY = "language"
            const val EXTRA_LANGUAGE_TAG = "extra_language_tag"
        }
    }
