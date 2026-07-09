package com.example.androidxmlbase.feature.demo.data.mapper

import com.example.androidxmlbase.core.architecture.ResultState
import com.example.androidxmlbase.core.network.ApiResult
import com.example.androidxmlbase.feature.demo.data.dto.DemoMessageDto
import org.junit.Assert.assertEquals
import org.junit.Test

class DemoMessageMapperTest {
    private val networkCause = Throwable("network down")
    private val parseCause = Throwable("malformed json")

    private data class Case(
        val name: String,
        val input: ApiResult<DemoMessageDto>,
        val expected: ResultState<String>,
    )

    private val cases =
        listOf(
            Case(
                name = "success maps to Success with the message",
                input = ApiResult.Success(DemoMessageDto(message = "hello")),
                expected = ResultState.Success("hello"),
            ),
            Case(
                name = "http error maps to Error with the status code in the message",
                input = ApiResult.HttpError(code = 404, message = "Not Found"),
                expected = ResultState.Error("Server error (404)"),
            ),
            Case(
                name = "network error maps to Error carrying the cause",
                input = ApiResult.NetworkError(networkCause),
                expected = ResultState.Error("No connection", networkCause),
            ),
            Case(
                name = "parse error maps to Error carrying the cause",
                input = ApiResult.ParseError(parseCause),
                expected = ResultState.Error("Unexpected response", parseCause),
            ),
            Case(
                name = "empty body maps to Error",
                input = ApiResult.EmptyBody,
                expected = ResultState.Error("Empty response"),
            ),
        )

    @Test
    fun `toResultState maps every ApiResult branch to the expected ResultState`() {
        cases.forEach { case ->
            assertEquals(case.name, case.expected, case.input.toResultState())
        }
    }
}
