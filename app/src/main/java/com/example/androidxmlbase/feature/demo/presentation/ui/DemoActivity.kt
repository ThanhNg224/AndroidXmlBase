package com.example.androidxmlbase.feature.demo.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.viewModels
import com.example.androidxmlbase.R
import com.example.androidxmlbase.core.ui.base.BaseActivity
import com.example.androidxmlbase.core.ui.base.setOnDebouncedClickListener
import com.example.androidxmlbase.databinding.ActivityDemoBinding
import com.example.androidxmlbase.feature.demo.presentation.state.DemoMessageError
import com.example.androidxmlbase.feature.demo.presentation.state.DemoMessageState
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

    private fun DemoMessageState.toDisplayText(): String =
        when (this) {
            DemoMessageState.Loading -> getString(R.string.demo_message_loading)
            is DemoMessageState.Success -> message
            is DemoMessageState.Error -> reason.toDisplayText()
        }

    private fun DemoMessageError.toDisplayText(): String =
        when (this) {
            DemoMessageError.SERVER -> getString(R.string.demo_message_error_server)
            DemoMessageError.NO_CONNECTION -> getString(R.string.demo_message_error_no_connection)
            DemoMessageError.UNEXPECTED_RESPONSE -> getString(R.string.demo_message_error_unexpected_response)
            DemoMessageError.EMPTY_RESPONSE -> getString(R.string.demo_message_error_empty_response)
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
