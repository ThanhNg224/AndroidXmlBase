package com.example.androidxmlbase.core.ui.base

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.example.androidxmlbase.R
import com.example.androidxmlbase.core.ui.transition.TransitionAction
import com.google.android.material.color.MaterialColors
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * Opaque full-screen host for a single [TransitionAction]: shows a theme-aware loading
 * animation, runs the action registered under the caller-supplied action key, then finishes.
 * One Activity, one manifest entry — a new transition use case only needs a new
 * [TransitionAction] implementation registered into the Hilt multibinding map, not a new
 * Activity subclass or manifest entry.
 *
 * Extends [AppCompatActivity] (not the lighter `ComponentActivity`) because on API 33+,
 * `AppCompatDelegate.setApplicationLocales`/`getApplicationLocales` (used by
 * [com.example.androidxmlbase.core.ui.transition.LanguageTransitionAction]) route through the
 * framework `LocaleManager` obtained from an *active* `AppCompatDelegate`'s context; with no
 * `AppCompatActivity` alive in the process, both calls silently no-op.
 */
@AndroidEntryPoint
class TransitionActivity : AppCompatActivity() {
    @Inject
    lateinit var actions: Map<String, @JvmSuppressWildcards TransitionAction>

    private var actionHasCompleted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transition)
        tintLoaderToTheme()

        if (savedInstanceState?.getBoolean(STATE_ACTION_COMPLETED) == true) {
            actionHasCompleted = true
            finishAfterSettleDelay()
        } else {
            runActionAfterTransitionIsOpaque()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_ACTION_COMPLETED, actionHasCompleted)
        super.onSaveInstanceState(outState)
    }

    private fun tintLoaderToTheme() {
        val loader = findViewById<LottieAnimationView>(R.id.transitionLoader)
        val primaryColor = MaterialColors.getColor(loader, androidx.appcompat.R.attr.colorPrimary)
        loader.addValueCallback(KeyPath("**"), LottieProperty.COLOR) { primaryColor }
    }

    private fun runActionAfterTransitionIsOpaque() {
        lifecycleScope.launch {
            delay(TRANSITION_ENTER_DURATION_MS.milliseconds)
            val actionKey = requireNotNull(intent.getStringExtra(EXTRA_ACTION_KEY)) { "Missing $EXTRA_ACTION_KEY extra" }
            actions.getValue(actionKey).perform(intent.extras ?: Bundle.EMPTY)
            actionHasCompleted = true
            finishAfterSettleDelay()
        }
    }

    private fun finishAfterSettleDelay() {
        lifecycleScope.launch {
            delay(TRANSITION_SETTLE_DURATION_MS.milliseconds)
            finish()
        }
    }

    companion object {
        private const val EXTRA_ACTION_KEY = "extra_action_key"
        private const val STATE_ACTION_COMPLETED = "state_action_completed"
        private const val TRANSITION_ENTER_DURATION_MS = 320L
        private const val TRANSITION_SETTLE_DURATION_MS = 80L

        fun createIntent(
            context: Context,
            actionKey: String,
            extras: Bundle = Bundle.EMPTY,
        ): Intent =
            Intent(context, TransitionActivity::class.java)
                .putExtra(EXTRA_ACTION_KEY, actionKey)
                .putExtras(extras)
    }
}
