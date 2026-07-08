package com.example.androidxmlbase.core.network.interceptor

import com.example.androidxmlbase.core.network.ConnectivityChecker
import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Response

class NoConnectivityException : IOException("No network connection")

class ConnectivityInterceptor(private val connectivityChecker: ConnectivityChecker) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        if (!connectivityChecker.isConnected()) throw NoConnectivityException()
        return chain.proceed(chain.request())
    }
}
