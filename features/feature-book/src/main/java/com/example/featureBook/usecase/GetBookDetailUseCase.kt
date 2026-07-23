package com.example.featureBook.usecase

import com.example.core.domain.DataError
import com.example.core.domain.Result
import com.example.featureBook.model.domain.BookUi
import com.example.featureBook.module.local.BooksCacheRepository
import com.example.featureBook.module.mapper.toEntity
import com.example.featureBook.module.mapper.toBookUi
import com.example.featureBook.module.network.BooksRemoteRepository
import com.example.core.presentation.FlowUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetBookDetailUseCase @Inject constructor(
    private val remote: BooksRemoteRepository,
    private val cache: BooksCacheRepository
) : FlowUseCase<String, BookUi, BookUi, DataError>() {

    override suspend fun doWork(input: String): Flow<Result<BookUi, DataError>> = flow {
        when (val cached = cache.getBookById(input)) {
            is Result.Success -> emit(Result.Success(cached.data.toBookUi()))
            is Result.Error -> {
                when (val remoteResult = remote.getBookDetail(input)) {
                    is Result.Success -> {
                        cache.saveBooks(listOf(remoteResult.data.toEntity()))
                        emit(Result.Success(remoteResult.data.toBookUi()))
                    }
                    is Result.Error -> emit(Result.Error(remoteResult.error))
                }
            }
        }
    }

    override suspend fun onSucceedDataHandling(intermediate: BookUi): BookUi = intermediate
}
