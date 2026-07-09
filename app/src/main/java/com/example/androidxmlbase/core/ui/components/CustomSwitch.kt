package com.example.androidxmlbase.core.ui.components

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.example.androidxmlbase.R
import com.google.android.material.materialswitch.MaterialSwitch

/**
 * Thin [MaterialSwitch] wrapper that tints the track/thumb from this base's color tokens instead
 * of the platform default, without reimplementing the base widget's touch/accessibility
 * handling. Based on [MaterialSwitch] rather than the older `SwitchCompat`: Material Components
 * is already a dependency, and `MaterialSwitch` is the actively maintained, Material3-styled
 * switch widget it ships — a strictly more modern base than `SwitchCompat` for the same amount
 * of code.
 */
class CustomSwitch
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : MaterialSwitch(context, attrs, defStyleAttr) {
        init {
            val checkedColor = ContextCompat.getColor(context, R.color.color_primary)
            val uncheckedColor = ContextCompat.getColor(context, R.color.color_on_surface)
            val checkedSurfaceColor = ContextCompat.getColor(context, R.color.color_surface)

            trackTintList = checkedStateList(checkedColor, uncheckedColor)
            thumbTintList = checkedStateList(checkedColor, checkedSurfaceColor)
        }

        private fun checkedStateList(
            checkedColor: Int,
            uncheckedColor: Int,
        ): ColorStateList =
            ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf(-android.R.attr.state_checked),
                ),
                intArrayOf(checkedColor, uncheckedColor),
            )
    }
