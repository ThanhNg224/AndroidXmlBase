package com.example.androidxmlbase.core.ui.base

import com.example.androidxmlbase.core.architecture.ResultState
import org.junit.Assert.assertEquals
import org.junit.Test

class ResultRenderStateTest {
    @Test
    fun `Loading maps to loading-visible render state`() {
        val result: ResultState<String> = ResultState.Loading

        val renderState = result.toRenderState()

        assertEquals(
            ResultRenderState(
                isLoadingVisible = true,
                isContentVisible = false,
                isErrorVisible = false,
                errorMessage = null,
            ),
            renderState,
        )
    }

    @Test
    fun `Success maps to content-visible render state`() {
        val result: ResultState<String> = ResultState.Success("hello")

        val renderState = result.toRenderState()

        assertEquals(
            ResultRenderState(
                isLoadingVisible = false,
                isContentVisible = true,
                isErrorVisible = false,
                errorMessage = null,
            ),
            renderState,
        )
    }

    @Test
    fun `Error maps to error-visible render state and passes the message through`() {
        val result: ResultState<String> = ResultState.Error("boom", cause = IllegalStateException("boom"))

        val renderState = result.toRenderState()

        assertEquals(
            ResultRenderState(
                isLoadingVisible = false,
                isContentVisible = false,
                isErrorVisible = true,
                errorMessage = "boom",
            ),
            renderState,
        )
    }
}
