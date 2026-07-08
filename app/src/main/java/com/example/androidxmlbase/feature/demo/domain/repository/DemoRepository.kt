package com.example.androidxmlbase.feature.demo.domain.repository

import kotlinx.coroutines.flow.Flow

interface DemoRepository {
    fun observeCount(): Flow<Int>
    suspend fun saveCount(count: Int)
}
