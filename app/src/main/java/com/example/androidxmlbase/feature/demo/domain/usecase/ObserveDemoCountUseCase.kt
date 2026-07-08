package com.example.androidxmlbase.feature.demo.domain.usecase

import com.example.androidxmlbase.feature.demo.domain.repository.DemoRepository
import kotlinx.coroutines.flow.Flow

class ObserveDemoCountUseCase(
    private val repository: DemoRepository,
) {
    operator fun invoke(): Flow<Int> = repository.observeCount()
}
