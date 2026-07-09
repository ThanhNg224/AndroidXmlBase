package com.example.androidxmlbase.core.di

import android.content.Context
import com.example.androidxmlbase.core.architecture.AppDispatchers
import com.example.androidxmlbase.core.architecture.DefaultAppDispatchers
import com.example.androidxmlbase.core.localization.LocaleManager
import com.example.androidxmlbase.core.storage.DataStoreSettingsStore
import com.example.androidxmlbase.core.storage.EncryptedSecureStore
import com.example.androidxmlbase.core.storage.SecureStore
import com.example.androidxmlbase.core.storage.SettingsStore
import com.example.androidxmlbase.core.storage.appSettingsDataStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppCoreBindingsModule {
    @Binds
    @Singleton
    abstract fun bindAppDispatchers(implementation: DefaultAppDispatchers): AppDispatchers

    @Binds
    @Singleton
    abstract fun bindSecureStore(implementation: EncryptedSecureStore): SecureStore
}

@Module
@InstallIn(SingletonComponent::class)
object AppCoreModule {
    @Provides
    @Singleton
    fun provideSettingsStore(
        @ApplicationContext context: Context,
    ): SettingsStore = DataStoreSettingsStore(context.appSettingsDataStore)

    @Provides
    @Singleton
    fun provideLocaleManager(): LocaleManager = LocaleManager()
}
