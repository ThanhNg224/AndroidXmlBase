package com.example.androidxmlbase.feature.demo.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.viewModels
import com.example.androidxmlbase.R
import com.example.androidxmlbase.core.architecture.ResultState
import com.example.androidxmlbase.core.ui.base.BaseActivity
import com.example.androidxmlbase.core.ui.base.setOnDebouncedClickListener
import com.example.androidxmlbase.core.ui.base.toRenderState
import com.example.androidxmlbase.databinding.ActivityDemoBinding
import com.example.androidxmlbase.feature.demo.presentation.state.DemoUiEffect
import com.example.androidxmlbase.feature.demo.presentation.state.DemoUiEvent
import com.example.androidxmlbase.feature.demo.presentation.viewmodel.DemoViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DemoActivity : BaseActivity<ActivityDemoBinding>() {
    private val viewModel: DemoViewModel by viewModels()

    override fun inflateBinding(inflater: LayoutInflater): ActivityDemoBinding = ActivityDemoBinding.inflate(inflater)

    override fun onBindingReady(savedInstanceState: Bundle?) {
        // Increment is the button most likely to be rapid-tapped, so it's the one debounced.
        binding.btnIncrement.setOnDebouncedClickListener {
            viewModel.onEvent(DemoUiEvent.IncrementClicked)
        }

        observeState()
        observeEffects()
    }

    private fun observeState() {
        viewModel.state.collectOnStarted { state ->
            binding.tvCount.text = getString(R.string.demo_count_format, state.count)
            binding.tvMessage.text = state.message.toDisplayText()
        }
    }

    // No dedicated loading/error View exists here (single tvMessage does triple duty), so
    // toRenderState() is used for its isLoadingVisible/errorMessage fields instead of a
    // visibility toggle — same output as the previous ResultState.fold-based text, no fold left
    // unused/unreachable.
    private fun ResultState<String>.toDisplayText(): String {
        val renderState = toRenderState()
        return when {
            renderState.isLoadingVisible -> getString(R.string.demo_message_loading)
            this is ResultState.Success -> data
            else -> renderState.errorMessage.orEmpty()
        }
    }

    private fun observeEffects() {
        viewModel.effect.collectOnStarted { effect ->
            when (effect) {
                DemoUiEffect.ShowMaxCountReached ->
                    Toast
                        .makeText(
                            this@DemoActivity,
                            getString(R.string.demo_max_count_reached),
                            Toast.LENGTH_SHORT,
                        ).show()
            }
        }
    }
}
