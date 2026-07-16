package com.example.androidxmlbase

import android.app.Application
import com.example.androidxmlbase.core.logging.ReleaseTree
import com.example.androidxmlbase.core.storage.database.DbPassphraseProvider
import com.example.androidxmlbase.core.ui.theme.ThemeManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class AndroidXmlBaseApplication : Application() {
    @Inject
    lateinit var themeManager: ThemeManager

    @Inject
    lateinit var dbPassphraseProvider: DbPassphraseProvider

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        Timber.plant(if (BuildConfig.DEBUG) Timber.DebugTree() else ReleaseTree())

        applicationScope.launch(Dispatchers.IO) { dbPassphraseProvider.getOrCreate() }

        // Applies as soon as the persisted theme loads. MainActivity's splash screen (Task 3)
        // stays up until ThemeManager.isThemeApplied is true, so there is no main-thread block
        // and no visible flash once the splash dismisses.
        themeManager.currentTheme
            .onEach { themeManager.applyTheme(it) }
            .launchIn(applicationScope)
    }
}
