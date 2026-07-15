package com.example.androidxmlbase.core.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.viewbinding.ViewBinding
import com.example.androidxmlbase.R

enum class DialogAnimation {
    NONE,
    FADE,
    SLIDE,
    SCALE,
}

/**
 * Base class for all customized DialogFragments. Handles standard margins,
 * tablet width limits, corner rounding, and entry/exit animation styling.
 */
abstract class BaseDialogFragment<VB : ViewBinding> : DialogFragment() {
    private var bindingOrNull: VB? = null
    protected val binding: VB
        get() = requireNotNull(bindingOrNull) { "binding accessed before onViewCreated() completed" }

    protected abstract fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): VB

    /** Chosen entrance/exit transition animation style. Override this to customize. */
    protected open val dialogAnimation: DialogAnimation = DialogAnimation.SCALE

    /** Background drawable. Override to use custom shapes. */
    protected open val backgroundDrawableRes: Int = R.drawable.bg_dialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        bindingOrNull = inflateBinding(inflater, container)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        onBindingReady(savedInstanceState)
    }

    /** Subclasses do their wiring here instead of overriding onViewCreated */
    protected abstract fun onBindingReady(savedInstanceState: Bundle?)

    override fun onStart() {
        super.onStart()
        setupDialogWindow()
    }

    private fun setupDialogWindow() {
        dialog?.window?.let { window ->
            // Apply standard rounded background
            window.setBackgroundDrawableResource(backgroundDrawableRes)

            // Setup width with margin & cap at maximum layout width
            val screenWidth = resources.displayMetrics.widthPixels
            val margin = resources.getDimensionPixelSize(R.dimen.dialog_screen_margin) * 2
            val maxWidth = resources.getDimensionPixelSize(R.dimen.dialog_max_width)
            val calculatedWidth = (screenWidth - margin).coerceAtMost(maxWidth)

            window.setLayout(calculatedWidth, ViewGroup.LayoutParams.WRAP_CONTENT)

            // Apply selected transition animation
            val animStyle =
                when (dialogAnimation) {
                    DialogAnimation.NONE -> 0
                    DialogAnimation.FADE -> R.style.Animation_AndroidXmlBase_Dialog_Fade
                    DialogAnimation.SLIDE -> R.style.Animation_AndroidXmlBase_Dialog_Slide
                    DialogAnimation.SCALE -> R.style.Animation_AndroidXmlBase_Dialog_Scale
                }
            if (animStyle != 0) {
                window.setWindowAnimations(animStyle)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bindingOrNull = null
    }
}
