package com.example.androidxmlbase.core.network.interceptor

import com.example.androidxmlbase.core.network.AuthTokenProvider
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthTokenInterceptor(private val authTokenProvider: AuthTokenProvider) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { authTokenProvider.getToken() }
        val request = chain.request().let { original ->
            if (token != null) original.newBuilder().addHeader("Authorization", "Bearer $token").build() else original
        }
        return chain.proceed(request)
    }
}
