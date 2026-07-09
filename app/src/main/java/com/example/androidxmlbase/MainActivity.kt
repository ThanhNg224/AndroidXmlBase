package com.example.androidxmlbase

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.lifecycle.lifecycleScope
import com.example.androidxmlbase.core.localization.LocaleManager
import com.example.androidxmlbase.core.localization.SettingsStoreLocaleStore
import com.example.androidxmlbase.core.storage.DataStoreSettingsStore
import com.example.androidxmlbase.core.storage.appSettingsDataStore
import com.example.androidxmlbase.core.ui.base.BaseActivity
import com.example.androidxmlbase.databinding.ActivityMainBinding
import com.example.androidxmlbase.feature.demo.presentation.ui.DemoActivity
import com.example.androidxmlbase.feature.designsystem.presentation.ui.DesignSystemActivity
import kotlinx.coroutines.launch

class MainActivity : BaseActivity<ActivityMainBinding>() {

    override fun inflateBinding(inflater: LayoutInflater): ActivityMainBinding =
        ActivityMainBinding.inflate(inflater)

    override fun onBindingReady(savedInstanceState: Bundle?) {
        binding.btnOpenDemo.setOnClickListener {
            startActivity(Intent(this, DemoActivity::class.java))
        }

        val settingsStore = DataStoreSettingsStore(applicationContext.appSettingsDataStore)
        val localeManager = LocaleManager(SettingsStoreLocaleStore(settingsStore))
        binding.btnLangEn.setOnClickListener {
            lifecycleScope.launch { localeManager.setLanguage("en") }
        }
        binding.btnLangVi.setOnClickListener {
            lifecycleScope.launch { localeManager.setLanguage("vi") }
        }
        binding.btnDesignSystem.setOnClickListener {
            startActivity(Intent(this, DesignSystemActivity::class.java))
        }
    }
}
