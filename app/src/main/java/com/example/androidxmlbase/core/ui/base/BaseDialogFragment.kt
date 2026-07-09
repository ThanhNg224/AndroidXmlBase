package com.example.androidxmlbase.core.ui.base

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

abstract class BaseDialogFragment<VB : ViewBinding> : DialogFragment() {
    private var bindingOrNull: VB? = null
    protected val binding: VB
        get() = requireNotNull(bindingOrNull) { "binding accessed outside the Dialog view lifecycle" }

    protected abstract fun inflateBinding(inflater: LayoutInflater): VB

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        bindingOrNull = inflateBinding(layoutInflater)
        return AlertDialog
            .Builder(requireContext())
            .setView(binding.root)
            .create()
    }

    override fun onStart() {
        super.onStart()
        onBindingReady()
    }

    protected abstract fun onBindingReady()

    override fun onDestroyView() {
        bindingOrNull = null
        super.onDestroyView()
    }

    protected fun <T> Flow<T>.collectOnStarted(action: suspend (T) -> Unit) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                collect(action)
            }
        }
    }
}
