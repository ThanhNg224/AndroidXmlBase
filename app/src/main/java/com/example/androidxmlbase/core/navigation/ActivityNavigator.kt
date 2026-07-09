package com.example.androidxmlbase.core.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import javax.inject.Inject

class ActivityNavigator
    @Inject
    constructor() {
        fun navigate(
            context: Context,
            destination: ActivityDestination,
        ) {
            val intent =
                Intent(context, destination.activityClass.java).apply {
                    destination.extras?.let { putExtras(BundleCompat.copyOf(it)) }
                    addFlags(destination.options.toIntentFlags())
                    if (context !is Activity) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
            context.startActivity(intent)
            if (destination.options.noAnimation && context is Activity) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    context.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
                } else {
                    @Suppress("DEPRECATION")
                    context.overridePendingTransition(0, 0)
                }
            }
        }

        fun finish(activity: Activity) {
            activity.finish()
        }
    }
