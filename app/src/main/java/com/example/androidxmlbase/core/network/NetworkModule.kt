package com.example.androidxmlbase.core.network

import com.example.androidxmlbase.core.network.interceptor.AuthTokenInterceptor
import com.example.androidxmlbase.core.network.interceptor.ConnectivityInterceptor
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

object NetworkModule {
    fun createRetrofit(
        config: ApiConfig,
        authTokenProvider: AuthTokenProvider,
        connectivityChecker: ConnectivityChecker,
    ): Retrofit {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (config.enableLogging) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(ConnectivityInterceptor(connectivityChecker))
            .addInterceptor(AuthTokenInterceptor(authTokenProvider))
            .addInterceptor(loggingInterceptor)
            .build()
        val json = Json { ignoreUnknownKeys = true }
        return Retrofit.Builder()
            .baseUrl(config.baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
}
