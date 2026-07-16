package com.example.androidxmlbase

import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.androidxmlbase.core.localization.LocaleManager
import com.example.androidxmlbase.core.navigation.ActivityDestination
import com.example.androidxmlbase.core.navigation.ActivityNavigator
import com.example.androidxmlbase.core.ui.base.BaseActivity
import com.example.androidxmlbase.core.ui.theme.ThemeManager
import com.example.androidxmlbase.databinding.ActivityMainBinding
import com.example.androidxmlbase.feature.demo.presentation.ui.DemoActivity
import com.example.androidxmlbase.feature.designsystem.presentation.ui.DesignSystemActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {
    @Inject
    lateinit var localeManager: LocaleManager

    @Inject
    lateinit var activityNavigator: ActivityNavigator

    @Inject
    lateinit var themeManager: ThemeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { !themeManager.isThemeApplied.value }
    }

    override fun inflateBinding(inflater: LayoutInflater): ActivityMainBinding = ActivityMainBinding.inflate(inflater)

    override fun onBindingReady(savedInstanceState: Bundle?) {
        binding.btnOpenDemo.setOnClickListener {
            activityNavigator.navigate(this, ActivityDestination(DemoActivity::class))
        }

        binding.btnLangEn.setOnClickListener {
            localeManager.setLanguage("en")
        }
        binding.btnLangVi.setOnClickListener {
            localeManager.setLanguage("vi")
        }
        binding.btnDesignSystem.setOnClickListener {
            activityNavigator.navigate(this, ActivityDestination(DesignSystemActivity::class))
        }
    }
}
