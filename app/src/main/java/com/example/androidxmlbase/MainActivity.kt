package com.example.androidxmlbase

import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.androidxmlbase.core.localization.AppLanguage
import com.example.androidxmlbase.core.localization.LocaleManager
import com.example.androidxmlbase.core.navigation.ActivityDestination
import com.example.androidxmlbase.core.navigation.ActivityNavigator
import com.example.androidxmlbase.core.ui.base.BaseActivity
import com.example.androidxmlbase.core.ui.base.TransitionActivity
import com.example.androidxmlbase.core.ui.theme.ThemeManager
import com.example.androidxmlbase.core.ui.transition.LanguageTransitionAction
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

    private var isLanguageChangeInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { !themeManager.isThemeApplied.value }
    }

    override fun inflateBinding(inflater: LayoutInflater): ActivityMainBinding = ActivityMainBinding.inflate(inflater)

    override fun onBindingReady(savedInstanceState: Bundle?) {
        bindLanguageSelector()

        binding.btnOpenDemo.setOnClickListener {
            activityNavigator.navigate(this, ActivityDestination(DemoActivity::class))
        }

        binding.btnDesignSystem.setOnClickListener {
            activityNavigator.navigate(this, ActivityDestination(DesignSystemActivity::class))
        }
    }

    private fun bindLanguageSelector() {
        binding.languageSelector.check(
            when (localeManager.currentLanguage()) {
                AppLanguage.ENGLISH -> R.id.btnLangEnglish
                AppLanguage.VIETNAMESE -> R.id.btnLangVietnamese
                null -> R.id.btnLangSystem
            },
        )
        binding.languageSelector.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            requestLanguageChange(
                when (checkedId) {
                    R.id.btnLangSystem -> null
                    R.id.btnLangEnglish -> AppLanguage.ENGLISH
                    R.id.btnLangVietnamese -> AppLanguage.VIETNAMESE
                    else -> return@addOnButtonCheckedListener
                },
            )
        }
    }

    private fun requestLanguageChange(language: AppLanguage?) {
        if (isLanguageChangeInProgress || language == localeManager.currentLanguage()) return

        isLanguageChangeInProgress = true
        startActivity(
            TransitionActivity.createIntent(
                context = this,
                actionKey = LanguageTransitionAction.KEY,
                extras =
                    Bundle().apply {
                        putString(LanguageTransitionAction.EXTRA_LANGUAGE_TAG, language?.languageTag.orEmpty())
                    },
            ),
        )
    }
}
