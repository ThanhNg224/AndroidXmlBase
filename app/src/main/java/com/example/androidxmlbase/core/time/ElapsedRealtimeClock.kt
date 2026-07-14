package com.example.androidxmlbase.core.time

import android.os.SystemClock
import javax.inject.Inject

/** Monotonic elapsed time for local security windows; it is not affected by wall-clock changes. */
fun interface ElapsedRealtimeClock {
    fun nowMillis(): Long
}

class AndroidElapsedRealtimeClock
    @Inject
    constructor() : ElapsedRealtimeClock {
        override fun nowMillis(): Long = SystemClock.elapsedRealtime()
    }
