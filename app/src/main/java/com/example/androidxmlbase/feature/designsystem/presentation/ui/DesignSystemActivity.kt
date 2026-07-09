package com.example.androidxmlbase.feature.designsystem.presentation.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.androidxmlbase.R
import com.example.androidxmlbase.core.architecture.ResultState
import com.example.androidxmlbase.core.architecture.fold
import com.example.androidxmlbase.core.ui.components.CustomToast
import com.example.androidxmlbase.databinding.ActivityDesignSystemBinding
import com.example.androidxmlbase.feature.designsystem.presentation.state.DesignSystemUiEvent
import com.example.androidxmlbase.feature.designsystem.presentation.viewmodel.DesignSystemViewModel
import kotlinx.coroutines.launch

class DesignSystemActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDesignSystemBinding

    private val viewModel: DesignSystemViewModel by lazy {
        ViewModelProvider(this)[DesignSystemViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDesignSystemBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnShowToast.setOnClickListener {
            CustomToast.show(this, getString(R.string.design_system_toast_message))
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
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state -> render(state.demoResult) }
            }
        }
    }

    private fun render(result: ResultState<Unit>) {
        binding.progressDemoResult.visibility = if (result is ResultState.Loading) View.VISIBLE else View.GONE
        binding.tvDemoResult.text = result.fold(
            onLoading = { getString(R.string.design_system_result_loading) },
            onSuccess = { getString(R.string.design_system_result_success) },
            onError = { message, _ -> message },
        )
    }
}
