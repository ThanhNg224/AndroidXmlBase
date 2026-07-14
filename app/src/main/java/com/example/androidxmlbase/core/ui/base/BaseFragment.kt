package com.example.androidxmlbase.core.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.example.androidxmlbase.R
import com.example.androidxmlbase.core.architecture.result.ResultState
import com.example.androidxmlbase.core.ui.components.FullScreenLoaderView
import com.example.androidxmlbase.core.ui.components.PromptDialogFragment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

abstract class BaseFragment<VB : ViewBinding> : Fragment() {
    private var bindingOrNull: VB? = null
    protected val binding: VB
        get() = requireNotNull(bindingOrNull) { "binding accessed outside the Fragment view lifecycle" }

    protected abstract fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): VB

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        bindingOrNull = inflateBinding(inflater, container)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        onBindingReady(view, savedInstanceState)
    }

    protected abstract fun onBindingReady(
        view: View,
        savedInstanceState: Bundle?,
    )

    override fun onDestroyView() {
        bindingOrNull = null
        super.onDestroyView()
    }

    protected fun <T> Flow<T>.collectOnStarted(action: suspend (T) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                collect(action)
            }
        }
    }

    /**
     * Binds a [ResultState] Flow, displaying a full-screen loading overlay on Loading,
     * showing an error dialog on Error, and executing [onSuccess] when data is loaded.
     */
    protected fun <T> bindResultState(
        flow: Flow<ResultState<T>>,
        onSuccess: (T) -> Unit,
    ) {
        flow.collectOnStarted { result ->
            when (result) {
                is ResultState.Loading -> {
                    showFullScreenLoader()
                }
                is ResultState.Success -> {
                    hideFullScreenLoader()
                    onSuccess(result.data)
                }
                is ResultState.Error -> {
                    hideFullScreenLoader()
                    showErrorPrompt(result.message)
                }
            }
        }
    }

    private fun showFullScreenLoader() {
        val root = activity?.findViewById<ViewGroup>(android.R.id.content) ?: return
        var loader = root.findViewById<FullScreenLoaderView>(R.id.full_screen_loader)
        if (loader == null) {
            context?.let { ctx ->
                loader =
                    FullScreenLoaderView(ctx).apply {
                        id = R.id.full_screen_loader
                    }
                root.addView(
                    loader,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
            }
        }
        loader?.show()
    }

    private fun hideFullScreenLoader() {
        val root = activity?.findViewById<ViewGroup>(android.R.id.content) ?: return
        root.findViewById<FullScreenLoaderView>(R.id.full_screen_loader)?.hide()
    }

    private fun showErrorPrompt(message: String) {
        PromptDialogFragment
            .newInstance(message = message)
            .show(childFragmentManager, "error_prompt_dialog")
    }
}
