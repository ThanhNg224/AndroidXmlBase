package com.example.androidxmlbase.core.ui.components

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import com.example.androidxmlbase.R
import com.example.androidxmlbase.core.ui.base.setOnDebouncedClickListener
import com.example.androidxmlbase.databinding.DialogPromptBinding

enum class PromptType {
    SUCCESS,
    ERROR,
    INFO,
}

/** Reusable status dialog (Success, Error, Info) with customizable actions. */
class PromptDialogFragment : DialogFragment() {
    private var bindingOrNull: DialogPromptBinding? = null
    private val binding get() = requireNotNull(bindingOrNull)

    var onPrimary: (() -> Unit)? = null
    var onSecondary: (() -> Unit)? = null

    // Legacy properties
    var onRetry: (() -> Unit)?
        get() = onPrimary
        set(value) {
            onPrimary = value
        }
    var onClose: (() -> Unit)?
        get() = onSecondary
        set(value) {
            onSecondary = value
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        bindingOrNull = DialogPromptBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setBackgroundDrawableResource(R.drawable.bg_dialog)
        dialog?.window?.setLayout(dialogWindowWidth(), ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun dialogWindowWidth(): Int {
        val screenWidth = resources.displayMetrics.widthPixels
        val margin = resources.getDimensionPixelSize(R.dimen.dialog_screen_margin) * 2
        val maxWidth = resources.getDimensionPixelSize(R.dimen.dialog_max_width)
        return (screenWidth - margin).coerceAtMost(maxWidth)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArguments()

        binding.tvPromptMessage.text = args.getString(ARG_MESSAGE)

        val technicalCode = args.getString(ARG_TECHNICAL_CODE)
        binding.tvPromptCode.text = technicalCode
        binding.tvPromptCode.visibility = if (technicalCode.isNullOrEmpty()) View.GONE else View.VISIBLE

        val type = args.getString(ARG_TYPE)?.let { PromptType.valueOf(it) } ?: PromptType.ERROR
        val iconRes =
            when (type) {
                PromptType.SUCCESS -> R.drawable.ic_activation_success
                PromptType.ERROR -> R.drawable.ic_failed
                PromptType.INFO -> R.drawable.ic_shield_check
            }
        binding.ivPromptIcon.setImageResource(iconRes)

        val primaryTextRes = args.getInt(ARG_PRIMARY_TEXT_RES, R.string.error_dialog_retry)
        binding.tvPrimaryText.setText(primaryTextRes)
        binding.btnPrimary.setOnDebouncedClickListener {
            emitResult(EVENT_PRIMARY)
            onPrimary?.invoke()
            dismiss()
        }

        val secondaryTextRes = args.getInt(ARG_SECONDARY_TEXT_RES, 0).takeIf { it != 0 }
        if (secondaryTextRes != null) {
            binding.tvSecondaryText.setText(secondaryTextRes)
            binding.btnSecondary.visibility = View.VISIBLE
            binding.btnSecondary.setOnDebouncedClickListener {
                emitResult(EVENT_SECONDARY)
                onSecondary?.invoke()
                dismiss()
            }
        } else {
            binding.btnSecondary.visibility = View.GONE
        }
    }

    private fun emitResult(event: String) {
        parentFragmentManager.setFragmentResult(
            RESULT_KEY,
            Bundle().apply {
                putString(EVENT_KEY, event)
            },
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bindingOrNull = null
    }

    companion object {
        private const val ARG_MESSAGE = "message"
        private const val ARG_TECHNICAL_CODE = "technical_code"
        private const val ARG_TYPE = "type"
        private const val ARG_PRIMARY_TEXT_RES = "primary_text_res"
        private const val ARG_SECONDARY_TEXT_RES = "secondary_text_res"

        const val RESULT_KEY =
            "com.example.androidxmlbase.core.ui.components.PromptDialogFragment.result"

        const val EVENT_KEY = "event"
        const val EVENT_PRIMARY = "primary"
        const val EVENT_SECONDARY = "secondary"

        // Legacy aliases
        const val EVENT_RETRY = EVENT_PRIMARY
        const val EVENT_CLOSE = EVENT_SECONDARY

        fun newInstance(
            message: String,
            technicalCode: String? = null,
            type: PromptType = PromptType.ERROR,
            @StringRes primaryButtonTextResId: Int = R.string.error_dialog_retry,
            @StringRes secondaryButtonTextResId: Int? = R.string.error_dialog_close,
        ): PromptDialogFragment =
            PromptDialogFragment().apply {
                arguments =
                    Bundle().apply {
                        putString(ARG_MESSAGE, message)
                        putString(ARG_TECHNICAL_CODE, technicalCode)
                        putString(ARG_TYPE, type.name)
                        putInt(ARG_PRIMARY_TEXT_RES, primaryButtonTextResId)
                        putInt(ARG_SECONDARY_TEXT_RES, secondaryButtonTextResId ?: 0)
                    }
            }
    }
}
