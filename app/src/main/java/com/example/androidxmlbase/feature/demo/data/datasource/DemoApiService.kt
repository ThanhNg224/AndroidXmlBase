package com.example.androidxmlbase.feature.demo.data.datasource

import com.example.androidxmlbase.feature.demo.data.dto.DemoMessageDto
import retrofit2.Response
import retrofit2.http.GET

interface DemoApiService {
    @GET("api/demo/message")
    suspend fun getMessage(): Response<DemoMessageDto>
}
