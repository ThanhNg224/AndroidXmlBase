package com.example.androidxmlbase.feature.demo.data.mapper

import com.example.androidxmlbase.core.architecture.AppError
import com.example.androidxmlbase.core.architecture.DomainResult
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
        val expected: DomainResult<String>,
    )

    private val cases =
        listOf(
            Case(
                name = "success maps to Success with the message",
                input = ApiResult.Success(DemoMessageDto(message = "hello")),
                expected = DomainResult.Success("hello"),
            ),
            Case(
                name = "http error maps to Http app error",
                input = ApiResult.HttpError(code = 404, message = "Not Found"),
                expected = DomainResult.Error(AppError.Http(code = 404, serverMessage = "Not Found")),
            ),
            Case(
                name = "network error maps to Network app error carrying the cause",
                input = ApiResult.NetworkError(networkCause),
                expected = DomainResult.Error(AppError.Network(networkCause)),
            ),
            Case(
                name = "parse error maps to Parse app error carrying the cause",
                input = ApiResult.ParseError(parseCause),
                expected = DomainResult.Error(AppError.Parse(parseCause)),
            ),
            Case(
                name = "empty body maps to EmptyBody app error",
                input = ApiResult.EmptyBody,
                expected = DomainResult.Error(AppError.EmptyBody),
            ),
        )

    @Test
    fun `toDomainResult maps every ApiResult branch to the expected DomainResult`() {
        cases.forEach { case ->
            assertEquals(case.name, case.expected, case.input.toDomainResult())
        }
    }
}
