package com.example.androidxmlbase.feature.demo.presentation.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.androidxmlbase.core.architecture.ResultState
import com.example.androidxmlbase.core.architecture.fold
import com.example.androidxmlbase.core.storage.DataStoreSettingsStore
import com.example.androidxmlbase.core.storage.appSettingsDataStore
import com.example.androidxmlbase.databinding.ActivityDemoBinding
import com.example.androidxmlbase.feature.demo.presentation.state.DemoUiEffect
import com.example.androidxmlbase.feature.demo.presentation.state.DemoUiEvent
import com.example.androidxmlbase.feature.demo.presentation.viewmodel.DemoViewModel
import com.example.androidxmlbase.feature.demo.presentation.viewmodel.DemoViewModelFactory
import kotlinx.coroutines.launch

class DemoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDemoBinding

    private val viewModel: DemoViewModel by lazy {
        val settingsStore = DataStoreSettingsStore(applicationContext.appSettingsDataStore)
        val factory = DemoViewModelFactory(applicationContext, settingsStore)
        ViewModelProvider(this, factory).get(DemoViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnIncrement.setOnClickListener {
            viewModel.onEvent(DemoUiEvent.IncrementClicked)
        }

        observeState()
        observeEffects()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    binding.tvCount.text = state.count.toString()
                    binding.tvMessage.text = state.message.toDisplayText()
                }
            }
        }
    }

    private fun ResultState<String>.toDisplayText(): String = fold(
        onLoading = { "Loading…" },
        onSuccess = { message -> message },
        onError = { message, _ -> message },
    )

    private fun observeEffects() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effect.collect { effect ->
                    when (effect) {
                        is DemoUiEffect.ShowToast ->
                            Toast.makeText(this@DemoActivity, effect.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
