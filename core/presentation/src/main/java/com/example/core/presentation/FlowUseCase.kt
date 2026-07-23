package com.example.core.presentation

import com.example.core.domain.Error
import com.example.core.domain.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed class UseCaseOutputWithStatus<out RESULT, out E : Error> {
    data object Progress : UseCaseOutputWithStatus<Nothing, Nothing>()
    data class Success<out RESULT>(val result: RESULT) : UseCaseOutputWithStatus<RESULT, Nothing>()
    data class Failed<out E : Error>(val error: E) : UseCaseOutputWithStatus<Nothing, E>()
}

abstract class FlowUseCase<in INPUT, INTERMEDIATE, out RESULT, out E : Error> {

    protected abstract suspend fun doWork(input: INPUT): Flow<Result<INTERMEDIATE, E>>

    protected abstract suspend fun onSucceedDataHandling(
        intermediate: @UnsafeVariance INTERMEDIATE
    ): RESULT

    fun invoke(input: INPUT): Flow<UseCaseOutputWithStatus<RESULT, E>> = flow {
        emit(UseCaseOutputWithStatus.Progress)
        doWork(input = input).collect { result ->
            when (result) {
                is Result.Success -> emit(UseCaseOutputWithStatus.Success(onSucceedDataHandling(result.data)))
                is Result.Error -> emit(UseCaseOutputWithStatus.Failed(result.error))
            }
        }
    }
}
