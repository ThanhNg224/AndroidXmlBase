package com.example.androidxmlbase.feature.demo.data.mapper

import com.example.androidxmlbase.core.architecture.AppError
import com.example.androidxmlbase.core.architecture.DomainResult
import com.example.androidxmlbase.core.network.ApiResult
import com.example.androidxmlbase.feature.demo.data.dto.DemoMessageDto

fun ApiResult<DemoMessageDto>.toDomainResult(): DomainResult<String> =
    when (this) {
        is ApiResult.Success -> DomainResult.Success(data.message)
        is ApiResult.HttpError -> DomainResult.Error(AppError.Http(code = code, serverMessage = message))
        is ApiResult.NetworkError -> DomainResult.Error(AppError.Network(cause))
        is ApiResult.ParseError -> DomainResult.Error(AppError.Parse(cause))
        ApiResult.EmptyBody -> DomainResult.Error(AppError.EmptyBody)
    }
