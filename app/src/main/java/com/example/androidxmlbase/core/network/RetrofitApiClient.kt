package com.example.androidxmlbase.core.network

import java.io.IOException
import kotlinx.coroutines.CancellationException
import retrofit2.Response

class RetrofitApiClient : ApiClient {
    override suspend fun <T> execute(call: suspend () -> Response<T>): ApiResult<T> {
        return try {
            val response = call()
            if (response.isSuccessful) {
                response.body()?.let { ApiResult.Success(it) } ?: ApiResult.EmptyBody
            } else {
                ApiResult.HttpError(response.code(), response.message())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            ApiResult.NetworkError(e)
        } catch (e: Exception) {
            ApiResult.ParseError(e)
        }
    }
}
