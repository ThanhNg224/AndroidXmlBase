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
        defStyleAttr: Int = com.google.android.material.R.attr.materialSwitchStyle,
    ) : MaterialSwitch(context, attrs, defStyleAttr) {
        init {
            val checkedTrackColor = ContextCompat.getColor(context, R.color.color_primary)
            val uncheckedTrackColor = ContextCompat.getColor(context, R.color.color_surface_variant)

            val checkedThumbColor = ContextCompat.getColor(context, R.color.color_surface)
            val uncheckedThumbColor = ContextCompat.getColor(context, R.color.color_outline)

            trackTintList = checkedStateList(checkedTrackColor, uncheckedTrackColor)
            trackDecorationTintList = checkedStateList(checkedTrackColor, uncheckedTrackColor)
            thumbTintList = checkedStateList(checkedThumbColor, uncheckedThumbColor)

            // Prevent text overlapping with the switch widget
            val density = context.resources.displayMetrics.density
            switchPadding = (16 * density).toInt()

            textOn = ""
            textOff = ""
            showText = false
        }

        private fun checkedStateList(
            checkedColor: Int,
            uncheckedColor: Int,
        ): ColorStateList =
            ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf(), // Default unchecked state fallback
                ),
                intArrayOf(checkedColor, uncheckedColor),
            )
    }
