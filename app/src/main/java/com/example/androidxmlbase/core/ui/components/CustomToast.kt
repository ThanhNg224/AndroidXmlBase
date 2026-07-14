package com.example.androidxmlbase.core.ui.components

import android.view.View
import androidx.core.content.ContextCompat
import com.example.androidxmlbase.R
import com.example.androidxmlbase.core.ui.util.Shape
import com.example.androidxmlbase.core.ui.util.ShapeUtils
import com.google.android.material.snackbar.Snackbar

/**
 * Drop-in replacement for a custom-styled `Toast` that renders on this base's design tokens
 * (surface background, on-surface text). Backed by [Snackbar] rather than a custom `Toast` view:
 * `Toast.setView` is deprecated from API 30 onward and custom toast views are suppressed while
 * the host app is backgrounded, whereas a [Snackbar] is anchored to a visible [View] and always
 * renders reliably in the foreground — the modern, Android-recommended mechanism for this exact
 * use case, and Material Components (already a dependency) ships it for free.
 */
object CustomToast {
    fun show(
        anchorView: View,
        message: String,
        duration: Int = Snackbar.LENGTH_SHORT,
    ): Snackbar {
        val context = anchorView.context
        val density = context.resources.displayMetrics.density

        val snackbar = Snackbar.make(anchorView, message, duration)
        snackbar.setTextColor(ContextCompat.getColor(context, R.color.color_on_surface))
        snackbar.view.background =
            ShapeUtils.buildDrawable(
                shape = Shape.RECTANGLE,
                cornerRadiusPx = CORNER_RADIUS_DP * density,
                fillColor = ContextCompat.getColor(context, R.color.color_surface),
            )
        snackbar.show()
        return snackbar
    }

    private const val CORNER_RADIUS_DP = 8f
}
