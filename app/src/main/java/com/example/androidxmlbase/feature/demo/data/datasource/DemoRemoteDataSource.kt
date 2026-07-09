package com.example.androidxmlbase.feature.demo.data.datasource

import com.example.androidxmlbase.core.network.ApiClient
import com.example.androidxmlbase.core.network.ApiResult
import com.example.androidxmlbase.feature.demo.data.dto.DemoMessageDto
import javax.inject.Inject

interface DemoRemoteDataSource {
    suspend fun fetchMessage(): ApiResult<DemoMessageDto>
}

class DemoRemoteDataSourceImpl
    @Inject
    constructor(
        private val apiService: DemoApiService,
        private val apiClient: ApiClient,
    ) : DemoRemoteDataSource {
        override suspend fun fetchMessage(): ApiResult<DemoMessageDto> = apiClient.execute { apiService.getMessage() }
    }
