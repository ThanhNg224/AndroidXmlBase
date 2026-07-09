package com.example.androidxmlbase.core.di

import android.content.Context
import com.example.androidxmlbase.BuildConfig
import com.example.androidxmlbase.core.network.AndroidConnectivityChecker
import com.example.androidxmlbase.core.network.ApiClient
import com.example.androidxmlbase.core.network.ApiConfig
import com.example.androidxmlbase.core.network.AuthTokenProvider
import com.example.androidxmlbase.core.network.ConnectivityChecker
import com.example.androidxmlbase.core.network.FileTransferClient
import com.example.androidxmlbase.core.network.NetworkModule
import com.example.androidxmlbase.core.network.OkHttpFileTransferClient
import com.example.androidxmlbase.core.network.RetrofitApiClient
import com.example.androidxmlbase.core.network.SecureStoreAuthTokenProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkBindingsModule {
    @Binds
    @Singleton
    abstract fun bindApiClient(implementation: RetrofitApiClient): ApiClient

    @Binds
    @Singleton
    abstract fun bindAuthTokenProvider(implementation: SecureStoreAuthTokenProvider): AuthTokenProvider

    @Binds
    @Singleton
    abstract fun bindFileTransferClient(implementation: OkHttpFileTransferClient): FileTransferClient
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkDiModule {
    @Provides
    @Singleton
    fun provideApiConfig(): ApiConfig =
        ApiConfig(
            baseUrl = BuildConfig.API_BASE_URL,
            enableLogging = BuildConfig.API_ENABLE_LOGGING,
        )

    @Provides
    @Singleton
    fun provideConnectivityChecker(
        @ApplicationContext context: Context,
    ): ConnectivityChecker = AndroidConnectivityChecker(context)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        config: ApiConfig,
        authTokenProvider: AuthTokenProvider,
        connectivityChecker: ConnectivityChecker,
    ): OkHttpClient =
        NetworkModule.createOkHttpClient(
            config = config,
            authTokenProvider = authTokenProvider,
            connectivityChecker = connectivityChecker,
        )

    @Provides
    @Singleton
    fun provideRetrofit(
        config: ApiConfig,
        okHttpClient: OkHttpClient,
    ): Retrofit =
        NetworkModule.createRetrofit(
            config = config,
            okHttpClient = okHttpClient,
        )
}
