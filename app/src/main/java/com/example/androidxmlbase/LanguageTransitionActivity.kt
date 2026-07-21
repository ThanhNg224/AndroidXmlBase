package com.example.androidxmlbase

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.androidxmlbase.core.localization.AppLanguage
import com.example.androidxmlbase.core.localization.LocaleManager
import com.example.androidxmlbase.core.ui.components.FullScreenLoaderView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Opaque transition screen that covers the locale update before returning to [MainActivity].
 * Its manifest declaration handles the locale configuration change so the screen can remain
 * visible during the hand-off.
 */
@AndroidEntryPoint
class LanguageTransitionActivity : ComponentActivity() {
    @Inject
    lateinit var localeManager: LocaleManager

    private var localeHasBeenApplied = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language_transition)

        findViewById<FullScreenLoaderView>(R.id.languageTransitionLoader)
            .show(R.string.language_change_loading)

        if (savedInstanceState?.getBoolean(STATE_LOCALE_APPLIED) == true) {
            localeHasBeenApplied = true
            finishAfterLocaleUpdate()
        } else {
            applyLanguageAfterTransitionIsOpaque()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_LOCALE_APPLIED, localeHasBeenApplied)
        super.onSaveInstanceState(outState)
    }

    private fun applyLanguageAfterTransitionIsOpaque() {
        lifecycleScope.launch {
            delay(TRANSITION_ENTER_DURATION_MS)
            localeHasBeenApplied = true
            val language = AppLanguage.findByLanguageTag(intent.getStringExtra(EXTRA_LANGUAGE_TAG).orEmpty())
            language?.let(localeManager::setLanguage)
                ?: localeManager.useSystemLanguage()
            finishAfterLocaleUpdate()
        }
    }

    private fun finishAfterLocaleUpdate() {
        lifecycleScope.launch {
            delay(TRANSITION_SETTLE_DURATION_MS)
            finish()
        }
    }

    companion object {
        private const val EXTRA_LANGUAGE_TAG = "extra_language_tag"
        private const val STATE_LOCALE_APPLIED = "state_locale_applied"
        private const val TRANSITION_ENTER_DURATION_MS = 320L
        private const val TRANSITION_SETTLE_DURATION_MS = 80L

        fun createIntent(
            context: Context,
            language: AppLanguage?,
        ): Intent =
            Intent(context, LanguageTransitionActivity::class.java).putExtra(
                EXTRA_LANGUAGE_TAG,
                language?.languageTag.orEmpty(),
            )
    }
}
