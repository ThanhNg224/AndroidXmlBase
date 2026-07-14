package com.example.androidxmlbase

import android.app.Application
import com.example.androidxmlbase.core.ui.theme.ThemeManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltAndroidApp
class AndroidXmlBaseApplication : Application() {
    @Inject
    lateinit var themeManager: ThemeManager

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        // Synchronously load and apply the theme on startup to prevent launch flashing
        runBlocking {
            val initialTheme = themeManager.getTheme()
            themeManager.applyTheme(initialTheme)
        }

        // Observe dynamic user theme configuration updates at runtime
        themeManager.currentTheme
            .onEach { themeManager.applyTheme(it) }
            .launchIn(applicationScope)
    }
}
