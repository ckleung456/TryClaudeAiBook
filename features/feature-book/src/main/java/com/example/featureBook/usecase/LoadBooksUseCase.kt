package com.example.featureBook.usecase

import com.example.core.domain.DataError
import com.example.core.domain.Result
import com.example.featureBook.model.domain.BookUi
import com.example.featureBook.model.domain.SortOrder
import com.example.featureBook.model.local.BookEntity
import com.example.featureBook.module.local.BooksCacheRepository
import com.example.featureBook.module.mapper.sortedBySortOrder
import com.example.featureBook.module.mapper.toEntity
import com.example.featureBook.module.mapper.toBookUi
import com.example.featureBook.module.network.BooksRemoteRepository
import com.example.core.presentation.FlowUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class LoadBooksUseCase @Inject constructor(
    private val remote: BooksRemoteRepository,
    private val cache: BooksCacheRepository
) : FlowUseCase<SortOrder, Pair<List<BookEntity>, SortOrder>, List<BookUi>, DataError>() {

    override suspend fun doWork(
        input: SortOrder
    ): Flow<Result<Pair<List<BookEntity>, SortOrder>, DataError>> = flow {
        // 1. Emit cached data immediately (offline-first)
        emit(Result.Success(cache.observeBooks().first() to input))

        // 2. Refresh from remote
        when (val remoteResult = remote.loadBooks()) {
            is Result.Success -> {
                if (remoteResult.data.isNotEmpty()) {
                    cache.saveBooks(remoteResult.data.map { it.toEntity() })
                    emit(Result.Success(cache.observeBooks().first() to input))
                }
            }
            is Result.Error -> emit(Result.Error(remoteResult.error))
        }
    }

    override suspend fun onSucceedDataHandling(
        intermediate: Pair<List<BookEntity>, SortOrder>
    ): List<BookUi> {
        val (books, sortOrder) = intermediate
        return books.map { it.toBookUi() }.sortedBySortOrder(sortOrder)
    }
}
