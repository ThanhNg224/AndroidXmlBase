package com.example.androidxmlbase.core.storage

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

private const val APP_SETTINGS_DATASTORE_NAME = "app_settings"

val Context.appSettingsDataStore by preferencesDataStore(name = APP_SETTINGS_DATASTORE_NAME)
