package com.example.androidxmlbase.feature.designsystem.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.viewModels
import com.example.androidxmlbase.R
import com.example.androidxmlbase.core.architecture.ResultState
import com.example.androidxmlbase.core.architecture.fold
import com.example.androidxmlbase.core.ui.base.BaseActivity
import com.example.androidxmlbase.core.ui.base.toRenderState
import com.example.androidxmlbase.core.ui.components.CustomToast
import com.example.androidxmlbase.databinding.ActivityDesignSystemBinding
import com.example.androidxmlbase.feature.designsystem.presentation.state.DesignSystemUiEvent
import com.example.androidxmlbase.feature.designsystem.presentation.viewmodel.DesignSystemViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DesignSystemActivity : BaseActivity<ActivityDesignSystemBinding>() {
    private val viewModel: DesignSystemViewModel by viewModels()

    override fun inflateBinding(inflater: LayoutInflater): ActivityDesignSystemBinding = ActivityDesignSystemBinding.inflate(inflater)

    override fun onBindingReady(savedInstanceState: Bundle?) {
        binding.btnShowToast.setOnClickListener {
            CustomToast.show(binding.root, getString(R.string.design_system_toast_message))
        }
        binding.btnShowLoading.setOnClickListener {
            viewModel.onEvent(DesignSystemUiEvent.ShowLoadingClicked)
        }
        binding.btnShowSuccess.setOnClickListener {
            viewModel.onEvent(DesignSystemUiEvent.ShowSuccessClicked)
        }
        binding.btnShowError.setOnClickListener {
            viewModel.onEvent(DesignSystemUiEvent.ShowErrorClicked)
        }

        observeState()
    }

    private fun observeState() {
        viewModel.state.collectOnStarted { state -> render(state.demoResult) }
    }

    private fun render(result: ResultState<Unit>) {
        val renderState = result.toRenderState()
        binding.progressDemoResult.visibility = if (renderState.isLoadingVisible) View.VISIBLE else View.GONE
        binding.tvDemoResult.text =
            result.fold(
                onLoading = { getString(R.string.design_system_result_loading) },
                onSuccess = { getString(R.string.design_system_result_success) },
                onError = { message, _ -> message },
            )
    }
}
