package com.example.featureBook.usecase

import com.example.featureBook.model.domain.BookUiModel
import com.example.featureBook.model.domain.SortOrder
import com.example.featureBook.module.local.BooksCacheRepository
import com.example.featureBook.module.mapper.sortedBySortOrder
import com.example.featureBook.module.mapper.toEntity
import com.example.featureBook.module.mapper.toUiModel
import com.example.featureBook.module.network.BooksRemoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

open class LoadBooksUseCase @Inject constructor(
    private val remote: BooksRemoteRepository,
    private val cache: BooksCacheRepository
) {
    open operator fun invoke(sortOrder: SortOrder): Flow<Result<List<BookUiModel>>> = flow {
        // 1. Emit cached data immediately (offline-first)
        val cached = cache.observeBooks().first()
        emit(Result.success(cached.map { it.toUiModel() }.sortedBySortOrder(sortOrder)))

        // 2. Refresh from remote
        try {
            val books = remote.loadBooks()
            if (books.isNotEmpty()) {
                cache.saveBooks(books.map { it.toEntity() })
                val refreshed = cache.observeBooks().first()
                emit(Result.success(refreshed.map { it.toUiModel() }.sortedBySortOrder(sortOrder)))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
