package com.example.androidxmlbase.feature.demo.domain.repository

import com.example.androidxmlbase.core.architecture.ResultState
import kotlinx.coroutines.flow.Flow

interface DemoRepository {
    fun observeCount(): Flow<Int>

    suspend fun saveCount(count: Int)

    suspend fun fetchMessage(): ResultState<String>
}
