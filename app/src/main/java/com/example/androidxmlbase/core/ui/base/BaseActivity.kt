package com.example.androidxmlbase.core.ui.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.example.androidxmlbase.R
import com.example.androidxmlbase.core.architecture.result.ResultState
import com.example.androidxmlbase.core.ui.components.FullScreenLoaderView
import com.example.androidxmlbase.core.ui.components.PromptDialogFragment
import com.example.androidxmlbase.core.ui.responsive.ResponsiveConfig
import com.example.androidxmlbase.core.ui.responsive.ResponsiveContextWrapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Common base for XML + ViewBinding activities: inflates [VB], applies the responsive
 * `attachBaseContext` wrapping every screen needs, and offers
 * [collectOnStarted] for lifecycle-safe Flow collection.
 */
abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {
    private var bindingOrNull: VB? = null
    protected val binding: VB
        get() = requireNotNull(bindingOrNull) { "binding accessed before onCreate() completed" }

    protected abstract fun inflateBinding(inflater: LayoutInflater): VB

    protected open val responsiveConfig: ResponsiveConfig = ResponsiveConfig()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ResponsiveContextWrapper.wrap(newBase, responsiveConfig))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingOrNull = inflateBinding(layoutInflater)
        setContentView(binding.root)
        onBindingReady(savedInstanceState)
    }

    /** Subclasses do their view/ViewModel wiring here instead of overriding `onCreate`. */
    protected abstract fun onBindingReady(savedInstanceState: Bundle?)

    override fun onDestroy() {
        super.onDestroy()
        bindingOrNull = null
    }

    protected fun <T> Flow<T>.collectOnStarted(action: suspend (T) -> Unit) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
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
        val root = findViewById<ViewGroup>(android.R.id.content)
        var loader = root.findViewById<FullScreenLoaderView>(R.id.full_screen_loader)
        if (loader == null) {
            loader =
                FullScreenLoaderView(this).apply {
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
        loader.show()
    }

    private fun hideFullScreenLoader() {
        val root = findViewById<ViewGroup>(android.R.id.content)
        root.findViewById<FullScreenLoaderView>(R.id.full_screen_loader)?.hide()
    }

    private fun showErrorPrompt(message: String) {
        PromptDialogFragment
            .newInstance(message = message)
            .show(supportFragmentManager, "error_prompt_dialog")
    }
}
