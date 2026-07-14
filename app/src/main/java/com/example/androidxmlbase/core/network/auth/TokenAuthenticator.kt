package com.example.androidxmlbase.core.network.auth

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Automatically intercepts 401 HTTP responses to attempt a token refresh cycle.
 */
@Singleton
class TokenAuthenticator
    @Inject
    constructor(
        private val tokenProvider: Provider<AuthTokenProvider>,
    ) : Authenticator {
        override fun authenticate(
            route: Route?,
            response: Response,
        ): Request? {
            // Double-check if the failed request already contained a token
            val authorizationHeader = response.request.header("Authorization")

            // Retrieve token
            val token = runBlocking { tokenProvider.get().getToken() }

            if (token != null && token == authorizationHeader) {
                // The request failed with the latest token. Avoid infinite retries.
                return null
            }

            // Re-fetch token (or trigger refresh logic in a fully integrated provider)
            val freshToken = runBlocking { tokenProvider.get().getToken() }

            if (freshToken.isNullOrBlank()) {
                return null
            }

            return response.request
                .newBuilder()
                .header("Authorization", freshToken)
                .build()
        }
    }
