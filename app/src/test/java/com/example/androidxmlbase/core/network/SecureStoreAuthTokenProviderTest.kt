package com.example.androidxmlbase.core.network

import com.example.androidxmlbase.core.storage.SecureStore
import com.example.androidxmlbase.core.storage.SecureStoreKey
import com.example.androidxmlbase.core.storage.SecureStoreKeys
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SecureStoreAuthTokenProviderTest {
    private class FakeSecureStore : SecureStore {
        private val values = mutableMapOf<SecureStoreKey, String>()

        override suspend fun getString(key: SecureStoreKey): String? = values[key]

        override suspend fun putString(
            key: SecureStoreKey,
            value: String,
        ) {
            values[key] = value
        }

        override suspend fun remove(key: SecureStoreKey) {
            values.remove(key)
        }

        override suspend fun clear() {
            values.clear()
        }
    }

    @Test
    fun `returns auth token from secure store`() =
        runTest {
            val store = FakeSecureStore()
            store.putString(SecureStoreKeys.AUTH_TOKEN, "secret-token")

            val provider = SecureStoreAuthTokenProvider(store)

            assertEquals("secret-token", provider.getToken())
        }

    @Test
    fun `returns null when auth token is absent`() =
        runTest {
            val provider = SecureStoreAuthTokenProvider(FakeSecureStore())

            assertNull(provider.getToken())
        }
}
