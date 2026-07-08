package com.example.androidxmlbase.core.network

interface AuthTokenProvider {
    suspend fun getToken(): String?
}

class NoOpAuthTokenProvider : AuthTokenProvider {
    override suspend fun getToken(): String? = null
}
