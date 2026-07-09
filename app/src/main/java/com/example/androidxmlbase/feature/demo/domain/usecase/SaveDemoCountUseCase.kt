package com.example.androidxmlbase.feature.demo.domain.usecase

import com.example.androidxmlbase.core.architecture.UseCase
import com.example.androidxmlbase.feature.demo.domain.repository.DemoRepository
import javax.inject.Inject

class SaveDemoCountUseCase
    @Inject
    constructor(
        private val repository: DemoRepository,
    ) : UseCase<Int, Unit> {
        override suspend fun invoke(params: Int) {
            repository.saveCount(params)
        }
    }
