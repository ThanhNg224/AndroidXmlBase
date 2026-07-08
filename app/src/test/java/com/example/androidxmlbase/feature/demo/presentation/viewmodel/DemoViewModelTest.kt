package com.example.androidxmlbase.feature.demo.presentation.viewmodel

import app.cash.turbine.test
import com.example.androidxmlbase.feature.demo.domain.repository.DemoRepository
import com.example.androidxmlbase.feature.demo.domain.usecase.IncrementCounterUseCase
import com.example.androidxmlbase.feature.demo.domain.usecase.ObserveDemoCountUseCase
import com.example.androidxmlbase.feature.demo.domain.usecase.SaveDemoCountUseCase
import com.example.androidxmlbase.feature.demo.presentation.state.DemoUiEffect
import com.example.androidxmlbase.feature.demo.presentation.state.DemoUiEvent
import com.example.androidxmlbase.testutil.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DemoViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeDemoRepository(initialCount: Int = 0) : DemoRepository {
        private val countFlow = MutableStateFlow(initialCount)
        val savedCounts = mutableListOf<Int>()

        override fun observeCount(): Flow<Int> = countFlow

        override suspend fun saveCount(count: Int) {
            savedCounts.add(count)
            countFlow.value = count
        }
    }

    /**
     * Unlike [FakeDemoRepository], [observeCount] here genuinely suspends until
     * [releaseInitialLoad] is called, mimicking real DataStore's first read requiring an
     * actual suspension instead of resolving synchronously.
     */
    private class DeferredFakeDemoRepository(initialCount: Int) : DemoRepository {
        private val initialLoadGate = CompletableDeferred<Unit>()
        private val countFlow = MutableStateFlow(initialCount)
        val savedCounts = mutableListOf<Int>()

        fun releaseInitialLoad() {
            initialLoadGate.complete(Unit)
        }

        override fun observeCount(): Flow<Int> = flow {
            initialLoadGate.await()
            emitAll(countFlow)
        }

        override suspend fun saveCount(count: Int) {
            savedCounts.add(count)
            countFlow.value = count
        }
    }

    private fun createViewModel(repository: DemoRepository): DemoViewModel {
        return DemoViewModel(
            incrementCounter = IncrementCounterUseCase(),
            observeDemoCount = ObserveDemoCountUseCase(repository),
            saveDemoCount = SaveDemoCountUseCase(repository),
        )
    }

    @Test
    fun `increment event increases the count in state`() = runTest {
        val viewModel = createViewModel(FakeDemoRepository())

        viewModel.state.test {
            assertEquals(0, awaitItem().count)
            viewModel.onEvent(DemoUiEvent.IncrementClicked)
            assertEquals(1, awaitItem().count)
        }
    }

    @Test
    fun `reaching the max count emits a show-toast effect`() = runTest {
        val viewModel = createViewModel(FakeDemoRepository())

        viewModel.effect.test {
            repeat(10) { viewModel.onEvent(DemoUiEvent.IncrementClicked) }
            assertEquals(DemoUiEffect.ShowToast("Max count reached"), awaitItem())
        }
    }

    @Test
    fun `initial state reflects the count already persisted in the repository`() = runTest {
        val viewModel = createViewModel(FakeDemoRepository(initialCount = 5))

        viewModel.state.test {
            assertEquals(5, awaitItem().count)
        }
    }

    @Test
    fun `incrementing saves the new count to the repository`() = runTest {
        val repository = FakeDemoRepository()
        val viewModel = createViewModel(repository)

        viewModel.state.test {
            awaitItem()
            viewModel.onEvent(DemoUiEvent.IncrementClicked)
            awaitItem()
        }

        assertEquals(listOf(1), repository.savedCounts)
    }

    @Test
    fun `count changes saved elsewhere in the repository are reflected in state`() = runTest {
        val repository = FakeDemoRepository()
        val viewModel = createViewModel(repository)

        viewModel.state.test {
            assertEquals(0, awaitItem().count)
            repository.saveCount(3)
            assertEquals(3, awaitItem().count)
        }
    }

    @Test
    fun `increment issued before the initial persisted count loads is ignored`() = runTest {
        val repository = DeferredFakeDemoRepository(initialCount = 5)
        val viewModel = createViewModel(repository)

        viewModel.state.test {
            // The initial DataStore read hasn't resolved yet, so state is still the default.
            assertEquals(0, awaitItem().count)

            viewModel.onEvent(DemoUiEvent.IncrementClicked)
            // Must be a no-op: no state change, and definitely no save of a value computed
            // from the stale default.
            expectNoEvents()
            assertEquals(emptyList<Int>(), repository.savedCounts)

            // The persisted value finally loads.
            repository.releaseInitialLoad()
            assertEquals(5, awaitItem().count)

            // A subsequent increment now behaves normally.
            viewModel.onEvent(DemoUiEvent.IncrementClicked)
            assertEquals(6, awaitItem().count)
        }

        assertEquals(listOf(6), repository.savedCounts)
    }
}
