package com.example.androidxmlbase.core.ui.components

import android.content.Context
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.androidxmlbase.R
import com.example.androidxmlbase.core.ui.util.Shape
import com.example.androidxmlbase.core.ui.util.ShapeUtils

/**
 * Drop-in replacement for `Toast.makeText(context, message, duration).show()` that renders the
 * message on this base's design tokens (surface background, on-surface text) instead of the
 * platform's default toast chrome. Kept as a static object API so any existing
 * `Toast.makeText(...).show()` call site (e.g. `DemoActivity`'s `ShowToast` effect handler) can
 * switch to [CustomToast.show] with the same call shape — that wiring itself is out of scope for
 * this task.
 *
 * Note: `Toast.setView` is deprecated from API 30 onward and custom toast views are suppressed
 * while the host app is backgrounded. That's an acceptable tradeoff for this base project's
 * foreground demo usage; a production app targeting background toasts would need a different
 * mechanism (e.g. a Snackbar anchored to a visible root view).
 */
object CustomToast {

    fun show(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        val density = context.resources.displayMetrics.density
        val paddingHorizontalPx = (PADDING_HORIZONTAL_DP * density).toInt()
        val paddingVerticalPx = (PADDING_VERTICAL_DP * density).toInt()

        val textView = TextView(context).apply {
            text = message
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(context, R.color.color_on_surface))
            setPadding(paddingHorizontalPx, paddingVerticalPx, paddingHorizontalPx, paddingVerticalPx)
            background = ShapeUtils.buildDrawable(
                shape = Shape.RECTANGLE,
                cornerRadiusPx = CORNER_RADIUS_DP * density,
                fillColor = ContextCompat.getColor(context, R.color.color_surface),
            )
        }

        @Suppress("DEPRECATION")
        Toast(context).apply {
            this.duration = duration
            view = textView
        }.show()
    }

    private const val PADDING_HORIZONTAL_DP = 16f
    private const val PADDING_VERTICAL_DP = 12f
    private const val CORNER_RADIUS_DP = 8f
}
