package com.example.androidxmlbase.core.startup

import com.example.androidxmlbase.core.di.ApplicationScope
import com.example.androidxmlbase.core.storage.database.DbPassphraseProvider
import com.example.androidxmlbase.core.ui.theme.ThemeManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope

/**
 * androidx.startup `Initializer`s are instantiated by reflection (no-arg constructor) from a
 * `ContentProvider` that runs before `Application.onCreate()`, so they can't use constructor
 * injection. This `EntryPoint` is how they reach Hilt-provided singletons instead. Safe to call
 * this early: Hilt's component is built lazily on first access, not on `Application.onCreate()`.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppStartupEntryPoint {
    fun dbPassphraseProvider(): DbPassphraseProvider

    fun themeManager(): ThemeManager

    @ApplicationScope
    fun applicationScope(): CoroutineScope
}
