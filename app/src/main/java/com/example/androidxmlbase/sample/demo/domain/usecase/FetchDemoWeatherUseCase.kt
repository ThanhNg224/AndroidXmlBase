package com.example.androidxmlbase.sample.demo.domain.usecase

import com.example.androidxmlbase.core.architecture.UseCase
import com.example.androidxmlbase.core.architecture.result.DomainResult
import com.example.androidxmlbase.sample.demo.domain.model.DemoWeather
import com.example.androidxmlbase.sample.demo.domain.repository.DemoRepository
import javax.inject.Inject

class FetchDemoWeatherUseCase
    @Inject
    constructor(
        private val repository: DemoRepository,
    ) : UseCase<Unit, DomainResult<DemoWeather>> {
        override suspend fun invoke(params: Unit): DomainResult<DemoWeather> = repository.fetchWeather()
    }
