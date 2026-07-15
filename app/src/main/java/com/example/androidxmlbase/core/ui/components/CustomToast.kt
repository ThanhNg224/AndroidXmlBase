package com.example.androidxmlbase.core.ui.components

import android.view.View
import androidx.core.content.ContextCompat
import com.example.androidxmlbase.R
import com.example.androidxmlbase.core.ui.util.Shape
import com.example.androidxmlbase.core.ui.util.ShapeUtils
import com.google.android.material.snackbar.Snackbar

/** Custom status toast replacement utilizing a styled [Snackbar] for foreground reliability. */
object CustomToast {
    fun show(
        anchorView: View,
        message: String,
        duration: Int = Snackbar.LENGTH_SHORT,
    ): Snackbar {
        val context = anchorView.context
        val density = context.resources.displayMetrics.density

        val snackbar = Snackbar.make(anchorView, message, duration)
        snackbar.setTextColor(ContextCompat.getColor(context, R.color.color_surface))
        snackbar.view.backgroundTintList = null
        snackbar.view.background =
            ShapeUtils.buildDrawable(
                shape = Shape.RECTANGLE,
                cornerRadiusPx = CORNER_RADIUS_DP * density,
                fillColor = ContextCompat.getColor(context, R.color.color_on_surface),
            )
        snackbar.show()
        return snackbar
    }

    private const val CORNER_RADIUS_DP = 8f
}
