package com.example.androidxmlbase.core.architecture

interface UseCase<in P, R> {
    suspend operator fun invoke(params: P): R
}
