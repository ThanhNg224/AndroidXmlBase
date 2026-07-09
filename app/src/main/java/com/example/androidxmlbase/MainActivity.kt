package com.example.androidxmlbase

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.androidxmlbase.core.localization.LocaleContextWrapper
import com.example.androidxmlbase.core.localization.LocaleManager
import com.example.androidxmlbase.core.localization.SettingsStoreLocaleStore
import com.example.androidxmlbase.core.storage.AppSettingsKeys
import com.example.androidxmlbase.core.storage.DataStoreSettingsStore
import com.example.androidxmlbase.core.storage.SettingsStore
import com.example.androidxmlbase.core.storage.appSettingsDataStore
import com.example.androidxmlbase.core.ui.responsive.ResponsiveConfig
import com.example.androidxmlbase.core.ui.responsive.ResponsiveContextWrapper
import com.example.androidxmlbase.databinding.ActivityMainBinding
import com.example.androidxmlbase.feature.demo.presentation.ui.DemoActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var settingsStore: SettingsStore

    override fun attachBaseContext(newBase: Context) {
        settingsStore = DataStoreSettingsStore(newBase.applicationContext.appSettingsDataStore)
        val languageCode = runBlocking { settingsStore.get(AppSettingsKeys.LANGUAGE_CODE) }
        val localeWrapped = LocaleContextWrapper.wrap(newBase, languageCode)
        // Locale first, then responsive clamp — clamp should act on the already-locale-resolved configuration.
        val responsiveWrapped = ResponsiveContextWrapper.wrap(localeWrapped, ResponsiveConfig())
        super.attachBaseContext(responsiveWrapped)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnOpenDemo.setOnClickListener {
            startActivity(Intent(this, DemoActivity::class.java))
        }

        val localeManager = LocaleManager(SettingsStoreLocaleStore(settingsStore))
        binding.btnLangEn.setOnClickListener {
            lifecycleScope.launch { localeManager.setLanguage("en") }
        }
        binding.btnLangVi.setOnClickListener {
            lifecycleScope.launch { localeManager.setLanguage("vi") }
        }
    }
}
