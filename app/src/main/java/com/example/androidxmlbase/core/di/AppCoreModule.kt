package com.example.androidxmlbase.core.di

import android.content.Context
import com.example.androidxmlbase.core.architecture.AppDispatchers
import com.example.androidxmlbase.core.architecture.DefaultAppDispatchers
import com.example.androidxmlbase.core.localization.LocaleManager
import com.example.androidxmlbase.core.network.auth.AuthTokenProvider
import com.example.androidxmlbase.core.network.auth.SecureStoreAuthTokenProvider
import com.example.androidxmlbase.core.storage.secure.EncryptedSecureStore
import com.example.androidxmlbase.core.storage.secure.SecureStore
import com.example.androidxmlbase.core.storage.settings.DataStoreSettingsStore
import com.example.androidxmlbase.core.storage.settings.SettingsStore
import com.example.androidxmlbase.core.storage.settings.appSettingsDataStore
import com.example.androidxmlbase.core.time.AndroidElapsedRealtimeClock
import com.example.androidxmlbase.core.time.ElapsedRealtimeClock
import com.example.androidxmlbase.core.ui.text.AndroidStringProvider
import com.example.androidxmlbase.core.ui.text.StringProvider
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

    @Binds
    @Singleton
    abstract fun bindAuthTokenProvider(implementation: SecureStoreAuthTokenProvider): AuthTokenProvider

    @Binds
    @Singleton
    abstract fun bindElapsedRealtimeClock(implementation: AndroidElapsedRealtimeClock): ElapsedRealtimeClock

    @Binds
    @Singleton
    abstract fun bindStringProvider(implementation: AndroidStringProvider): StringProvider
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
