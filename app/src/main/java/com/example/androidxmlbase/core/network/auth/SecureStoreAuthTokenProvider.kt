package com.example.androidxmlbase.core.network.auth

import com.example.androidxmlbase.core.storage.secure.SecureStore
import com.example.androidxmlbase.core.storage.secure.SecureStoreKeys
import javax.inject.Inject

class SecureStoreAuthTokenProvider
    @Inject
    constructor(
        private val secureStore: SecureStore,
    ) : AuthTokenProvider {
        override suspend fun getToken(): String? = secureStore.getString(SecureStoreKeys.AUTH_TOKEN)
    }
