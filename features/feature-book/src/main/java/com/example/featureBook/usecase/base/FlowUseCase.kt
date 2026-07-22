package com.example.featureBook.usecase.base

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

sealed class UseCaseOutputWithStatus<out RESULT> {
    data class Progress<out RESULT>(val data: Any? = null) : UseCaseOutputWithStatus<RESULT>()
    data class Success<out RESULT>(val result: RESULT) : UseCaseOutputWithStatus<RESULT>()
    data class Failed<out RESULT>(
        val error: Exception,
        val failedResult: RESULT? = null
    ) : UseCaseOutputWithStatus<RESULT>()
}

abstract class FlowUseCase<in INPUT, INTERMEDIATE, out RESULT> {

    protected abstract suspend fun doWork(input: INPUT): Flow<INTERMEDIATE>

    protected open suspend fun errorResult(error: Throwable): RESULT? = null

    open fun isValidInput(input: INPUT): Boolean = true

    abstract suspend fun onSucceedDataHandling(
        intermediate: @UnsafeVariance INTERMEDIATE
    ): UseCaseOutputWithStatus.Success<RESULT>

    fun invoke(input: INPUT): Flow<UseCaseOutputWithStatus<RESULT>> = flow {
        try {
            if (!isValidInput(input)) {
                throw IllegalArgumentException()
            }
            emit(UseCaseOutputWithStatus.Progress())
            doWork(input = input)
                .map { onSucceedDataHandling(intermediate = it) }
                .collect { emit(it) }
        } catch (e: Exception) {
            emit(
                UseCaseOutputWithStatus.Failed(
                    error = e,
                    failedResult = errorResult(error = e)
                )
            )
        }
    }
}
