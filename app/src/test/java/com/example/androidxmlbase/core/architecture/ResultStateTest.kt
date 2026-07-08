package com.example.androidxmlbase.core.architecture

import org.junit.Assert.assertEquals
import org.junit.Test

class ResultStateTest {

    @Test
    fun `fold invokes onSuccess branch for Success`() {
        val result: ResultState<Int> = ResultState.Success(42)

        val text = result.fold(
            onLoading = { "loading" },
            onSuccess = { "success:$it" },
            onError = { message, _ -> "error:$message" },
        )

        assertEquals("success:42", text)
    }

    @Test
    fun `fold invokes onError branch for Error`() {
        val result: ResultState<Int> = ResultState.Error("boom")

        val text = result.fold(
            onLoading = { "loading" },
            onSuccess = { "success:$it" },
            onError = { message, _ -> "error:$message" },
        )

        assertEquals("error:boom", text)
    }

    @Test
    fun `fold invokes onLoading branch for Loading`() {
        val result: ResultState<Int> = ResultState.Loading

        val text = result.fold(
            onLoading = { "loading" },
            onSuccess = { "success:$it" },
            onError = { message, _ -> "error:$message" },
        )

        assertEquals("loading", text)
    }
}
