package com.example.androidxmlbase.feature.demo.data.mapper

import com.example.androidxmlbase.core.architecture.ResultState
import com.example.androidxmlbase.core.network.ApiResult
import com.example.androidxmlbase.feature.demo.data.dto.DemoMessageDto

fun ApiResult<DemoMessageDto>.toResultState(): ResultState<String> =
    when (this) {
        is ApiResult.Success -> ResultState.Success(data.message)
        is ApiResult.HttpError -> ResultState.Error("Server error ($code)")
        is ApiResult.NetworkError -> ResultState.Error("No connection", cause)
        is ApiResult.ParseError -> ResultState.Error("Unexpected response", cause)
        ApiResult.EmptyBody -> ResultState.Error("Empty response")
    }
