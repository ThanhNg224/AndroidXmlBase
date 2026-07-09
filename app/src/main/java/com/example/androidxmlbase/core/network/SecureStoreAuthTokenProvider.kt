package com.example.androidxmlbase.core.network

import com.example.androidxmlbase.core.storage.SecureStore
import com.example.androidxmlbase.core.storage.SecureStoreKeys
import javax.inject.Inject

class SecureStoreAuthTokenProvider
    @Inject
    constructor(
        private val secureStore: SecureStore,
    ) : AuthTokenProvider {
        override suspend fun getToken(): String? = secureStore.getString(SecureStoreKeys.AUTH_TOKEN)
    }
