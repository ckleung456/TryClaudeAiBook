package com.example.featureBook.usecase

import com.example.featureBook.model.domain.BookUiModel
import com.example.featureBook.model.domain.SortOrder
import com.example.featureBook.model.local.BookEntity
import com.example.featureBook.module.local.BooksCacheRepository
import com.example.featureBook.module.mapper.sortedBySortOrder
import com.example.featureBook.module.mapper.toEntity
import com.example.featureBook.module.mapper.toUiModel
import com.example.featureBook.module.network.BooksRemoteRepository
import com.example.featureBook.usecase.base.FlowUseCase
import com.example.featureBook.usecase.base.UseCaseOutputWithStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class LoadBooksUseCase @Inject constructor(
    private val remote: BooksRemoteRepository,
    private val cache: BooksCacheRepository
) : FlowUseCase<SortOrder, Pair<List<BookEntity>, SortOrder>, List<BookUiModel>>() {

    override suspend fun doWork(input: SortOrder): Flow<Pair<List<BookEntity>, SortOrder>> = flow {
        // 1. Emit cached data immediately (offline-first)
        emit(cache.observeBooks().first() to input)

        // 2. Refresh from remote
        val books = remote.loadBooks()
        if (books.isNotEmpty()) {
            cache.saveBooks(books.map { it.toEntity() })
            emit(cache.observeBooks().first() to input)
        }
    }

    override suspend fun onSucceedDataHandling(
        intermediate: Pair<List<BookEntity>, SortOrder>
    ): UseCaseOutputWithStatus.Success<List<BookUiModel>> {
        val (books, sortOrder) = intermediate
        return UseCaseOutputWithStatus.Success(
            books.map { it.toUiModel() }.sortedBySortOrder(sortOrder)
        )
    }
}
