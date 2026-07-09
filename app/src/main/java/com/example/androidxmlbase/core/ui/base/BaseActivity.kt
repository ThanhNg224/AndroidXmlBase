package com.example.androidxmlbase.core.ui.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.example.androidxmlbase.core.localization.LocaleContextWrapper
import com.example.androidxmlbase.core.storage.AppSettingsKeys
import com.example.androidxmlbase.core.storage.DataStoreSettingsStore
import com.example.androidxmlbase.core.storage.appSettingsDataStore
import com.example.androidxmlbase.core.ui.responsive.ResponsiveConfig
import com.example.androidxmlbase.core.ui.responsive.ResponsiveContextWrapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Common base for XML + ViewBinding activities: inflates [VB], applies the
 * locale + responsive `attachBaseContext` wrapping every screen needs, and offers
 * [collectOnStarted] for lifecycle-safe Flow collection.
 */
abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    private var _binding: VB? = null
    protected val binding: VB
        get() = requireNotNull(_binding) { "binding accessed before onCreate() completed" }

    protected abstract fun inflateBinding(inflater: LayoutInflater): VB

    protected open val responsiveConfig: ResponsiveConfig = ResponsiveConfig()

    override fun attachBaseContext(newBase: Context) {
        val settingsStore = DataStoreSettingsStore(newBase.applicationContext.appSettingsDataStore)
        val languageCode = runBlocking { settingsStore.get(AppSettingsKeys.LANGUAGE_CODE) }
        val localeWrapped = LocaleContextWrapper.wrap(newBase, languageCode)
        // Locale first, then responsive clamp — clamp should act on the already-locale-resolved configuration.
        super.attachBaseContext(ResponsiveContextWrapper.wrap(localeWrapped, responsiveConfig))
    }

    final override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = inflateBinding(layoutInflater)
        setContentView(binding.root)
        onBindingReady(savedInstanceState)
    }

    /** Subclasses do their view/ViewModel wiring here instead of overriding `onCreate`. */
    protected abstract fun onBindingReady(savedInstanceState: Bundle?)

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    protected fun <T> Flow<T>.collectOnStarted(action: suspend (T) -> Unit) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                collect(action)
            }
        }
    }
}
