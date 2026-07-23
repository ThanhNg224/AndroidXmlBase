package com.example.androidxmlbase.sample.demo.domain.repository

import com.example.androidxmlbase.core.architecture.result.DomainResult
import com.example.androidxmlbase.sample.demo.domain.model.DemoWeather
import kotlinx.coroutines.flow.Flow

interface DemoRepository {
    fun observeCount(): Flow<Int>

    suspend fun saveCount(count: Int)

    suspend fun fetchWeather(): DomainResult<DemoWeather>
}
