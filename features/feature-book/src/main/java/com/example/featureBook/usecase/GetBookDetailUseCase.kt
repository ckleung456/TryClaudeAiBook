package com.example.featureBook.usecase

import com.example.featureBook.model.domain.BookUiModel
import com.example.featureBook.module.local.BooksCacheRepository
import com.example.featureBook.module.mapper.toEntity
import com.example.featureBook.module.mapper.toUiModel
import com.example.featureBook.module.network.BooksRemoteRepository
import javax.inject.Inject

open class GetBookDetailUseCase @Inject constructor(
    private val remote: BooksRemoteRepository,
    private val cache: BooksCacheRepository
) {
    open suspend operator fun invoke(bookId: String): Result<BookUiModel> = runCatching {
        cache.getBookById(bookId)?.toUiModel()
            ?: remote.getBookDetail(bookId)
                ?.also { cache.saveBooks(listOf(it.toEntity())) }
                ?.toUiModel()
            ?: error("Book not found: $bookId")
    }
}
