package com.example.androidxmlbase

import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.androidxmlbase.core.navigation.ActivityDestination
import com.example.androidxmlbase.core.navigation.ActivityNavigator
import com.example.androidxmlbase.core.ui.base.BaseActivity
import com.example.androidxmlbase.core.ui.theme.ThemeManager
import com.example.androidxmlbase.databinding.ActivityMainBinding
import com.example.androidxmlbase.feature.settings.presentation.ui.SettingsActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {
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
        val navController =
            (supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment)
                .navController
        binding.bottomNavigation.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.topAppBar.title = destination.label
        }

        binding.topAppBar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.actionSettings -> {
                    activityNavigator.navigate(this, ActivityDestination(SettingsActivity::class))
                    true
                }

                else -> false
            }
        }
    }
}
