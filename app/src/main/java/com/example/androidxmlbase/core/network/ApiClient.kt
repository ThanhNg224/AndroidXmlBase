package com.example.androidxmlbase.core.network

interface ApiClient {
    suspend fun <T> execute(call: suspend () -> retrofit2.Response<T>): ApiResult<T>
}
