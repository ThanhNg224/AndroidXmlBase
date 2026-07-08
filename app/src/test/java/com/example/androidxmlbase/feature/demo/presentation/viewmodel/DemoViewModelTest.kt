package com.example.androidxmlbase.feature.demo.presentation.viewmodel

import app.cash.turbine.test
import com.example.androidxmlbase.feature.demo.domain.usecase.IncrementCounterUseCase
import com.example.androidxmlbase.feature.demo.presentation.state.DemoUiEffect
import com.example.androidxmlbase.feature.demo.presentation.state.DemoUiEvent
import com.example.androidxmlbase.testutil.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DemoViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `increment event increases the count in state`() = runTest {
        val viewModel = DemoViewModel(IncrementCounterUseCase())

        viewModel.state.test {
            assertEquals(0, awaitItem().count)
            viewModel.onEvent(DemoUiEvent.IncrementClicked)
            assertEquals(1, awaitItem().count)
        }
    }

    @Test
    fun `reaching the max count emits a show-toast effect`() = runTest {
        val viewModel = DemoViewModel(IncrementCounterUseCase())

        viewModel.effect.test {
            repeat(10) { viewModel.onEvent(DemoUiEvent.IncrementClicked) }
            assertEquals(DemoUiEffect.ShowToast("Max count reached"), awaitItem())
        }
    }
}
