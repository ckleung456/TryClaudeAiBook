package com.example.featureBook.usecase

import com.example.core.domain.DataError
import com.example.core.domain.Result
import com.example.featureBook.model.domain.SortOrder
import com.example.featureBook.module.local.BooksCacheRepository
import com.example.featureBook.module.mapper.sortedBySortOrder
import com.example.featureBook.module.mapper.toEntity
import com.example.featureBook.module.mapper.toBookUi
import com.example.featureBook.module.network.BooksRemoteRepository
import com.example.core.presentation.FlowUseCase
import com.example.featureBook.model.domain.LoadBooksIntermediate
import com.example.featureBook.model.domain.LoadBooksResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LoadBooksUseCase @Inject constructor(
    private val remote: BooksRemoteRepository,
    private val cache: BooksCacheRepository
) : FlowUseCase<SortOrder, LoadBooksIntermediate, LoadBooksResult, DataError>() {

    override suspend fun doWork(
        input: SortOrder
    ): Flow<Result<LoadBooksIntermediate, DataError>> = flow {
        // 1. Emit cached data immediately (offline-first)
        emit(Result.Success(LoadBooksIntermediate(cache.observeBooks().first(), input, isFromCache = true)))

        // 2. Refresh from remote
        when (val remoteResult = remote.loadBooks()) {
            is Result.Success -> {
                if (remoteResult.data.isNotEmpty()) {
                    cache.saveBooks(remoteResult.data.map { it.toEntity() })
                    emit(Result.Success(LoadBooksIntermediate(cache.observeBooks().first(), input, isFromCache = false)))
                }
            }
            is Result.Error -> emit(Result.Error(remoteResult.error))
        }
    }

    override suspend fun onSucceedDataHandling(
        intermediate: LoadBooksIntermediate
    ): LoadBooksResult = withContext(Dispatchers.IO) {
        val data = intermediate.entities.map { it.toBookUi() }.sortedBySortOrder(intermediate.sortOrder)
        return@withContext LoadBooksResult(books = data, isFromCache = intermediate.isFromCache)
    }
}
