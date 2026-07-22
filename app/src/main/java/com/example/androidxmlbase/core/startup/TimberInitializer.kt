package com.example.androidxmlbase.core.startup

import android.content.Context
import androidx.startup.Initializer
import com.example.androidxmlbase.BuildConfig
import com.example.androidxmlbase.core.logging.ReleaseTree
import timber.log.Timber

class TimberInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        Timber.plant(if (BuildConfig.DEBUG) Timber.DebugTree() else ReleaseTree())
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
