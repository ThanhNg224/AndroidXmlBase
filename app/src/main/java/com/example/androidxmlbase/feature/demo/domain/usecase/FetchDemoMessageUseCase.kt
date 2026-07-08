package com.example.androidxmlbase.feature.demo.domain.usecase

import com.example.androidxmlbase.core.architecture.ResultState
import com.example.androidxmlbase.core.architecture.UseCase
import com.example.androidxmlbase.feature.demo.domain.repository.DemoRepository

class FetchDemoMessageUseCase(private val repository: DemoRepository) : UseCase<Unit, ResultState<String>> {
    override suspend fun invoke(params: Unit): ResultState<String> = repository.fetchMessage()
}
