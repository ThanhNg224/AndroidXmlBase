package com.example.androidxmlbase.core.ui.components

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import com.example.androidxmlbase.R

/**
 * Thin [SwitchCompat] wrapper that tints the track/thumb from this base's color tokens instead
 * of the platform default, without reimplementing `SwitchCompat`'s touch/accessibility handling.
 */
class CustomSwitch @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : SwitchCompat(context, attrs, defStyleAttr) {

    init {
        val checkedColor = ContextCompat.getColor(context, R.color.color_primary)
        val uncheckedColor = ContextCompat.getColor(context, R.color.color_on_surface)
        val checkedSurfaceColor = ContextCompat.getColor(context, R.color.color_surface)

        trackTintList = checkedStateList(checkedColor, uncheckedColor)
        thumbTintList = checkedStateList(checkedColor, checkedSurfaceColor)
    }

    private fun checkedStateList(checkedColor: Int, uncheckedColor: Int): ColorStateList = ColorStateList(
        arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_checked),
        ),
        intArrayOf(checkedColor, uncheckedColor),
    )
}
