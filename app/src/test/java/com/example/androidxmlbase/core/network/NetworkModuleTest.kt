package com.example.androidxmlbase.core.network

import com.example.androidxmlbase.core.network.auth.NoOpAuthTokenProvider
import com.example.androidxmlbase.core.network.connectivity.ConnectivityChecker
import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkModuleTest {
    @Test
    fun `creates client with thirty second connect read and write timeouts`() {
        val client =
            NetworkModule.createOkHttpClient(
                config = ApiConfig(baseUrl = "https://example.com/", enableLogging = false),
                authTokenProvider = NoOpAuthTokenProvider(),
                connectivityChecker = ConnectedConnectivityChecker,
            )

        assertEquals(30_000, client.connectTimeoutMillis)
        assertEquals(30_000, client.readTimeoutMillis)
        assertEquals(30_000, client.writeTimeoutMillis)
    }

    private data object ConnectedConnectivityChecker : ConnectivityChecker {
        override fun isConnected(): Boolean = true
    }
}
