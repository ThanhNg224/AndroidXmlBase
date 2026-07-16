package com.example.androidxmlbase

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Startup work (logging, DB passphrase warm-up, theme application) runs via the
 * `androidx.startup` `Initializer`s in `core/startup/`, registered in AndroidManifest.xml —
 * not here. See `docs/CORE_MODULES.md` → `core/startup`.
 */
@HiltAndroidApp
class AndroidXmlBaseApplication : Application()
