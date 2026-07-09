package com.example.androidxmlbase.feature.demo.di

import com.example.androidxmlbase.feature.demo.data.datasource.DemoRemoteDataSource
import com.example.androidxmlbase.feature.demo.data.datasource.DemoRemoteDataSourceImpl
import com.example.androidxmlbase.feature.demo.data.repository.DemoRepositoryImpl
import com.example.androidxmlbase.feature.demo.domain.repository.DemoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DemoModule {
    @Binds
    @Singleton
    abstract fun bindDemoRemoteDataSource(implementation: DemoRemoteDataSourceImpl): DemoRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindDemoRepository(implementation: DemoRepositoryImpl): DemoRepository
}
