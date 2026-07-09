@file:Suppress("DEPRECATION")

package com.example.androidxmlbase.core.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.androidxmlbase.core.architecture.AppDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import javax.inject.Inject

class EncryptedSecureStore
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val dispatchers: AppDispatchers,
    ) : SecureStore {
        private val preferences: SharedPreferences by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            createEncryptedPreferences()
        }

        override suspend fun getString(key: SecureStoreKey): String? =
            withContext(dispatchers.io) {
                preferences.getString(key.name, null)
            }

        override suspend fun putString(
            key: SecureStoreKey,
            value: String,
        ) {
            withContext(dispatchers.io) {
                preferences.edit(commit = true) {
                    putString(key.name, value)
                }
            }
        }

        override suspend fun remove(key: SecureStoreKey) {
            withContext(dispatchers.io) {
                preferences.edit(commit = true) {
                    remove(key.name)
                }
            }
        }

        override suspend fun clear() {
            withContext(dispatchers.io) {
                preferences.edit(commit = true) {
                    clear()
                }
            }
        }

        @Suppress("DEPRECATION")
        private fun createEncryptedPreferences(): SharedPreferences {
            val masterKey =
                MasterKey
                    .Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
            return EncryptedSharedPreferences.create(
                context,
                FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }

        private companion object {
            const val FILE_NAME = "secure_store"
        }
    }
