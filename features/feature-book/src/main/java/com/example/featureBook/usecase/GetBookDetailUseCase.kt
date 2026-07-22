package com.example.featureBook.usecase

import com.example.featureBook.model.domain.BookUiModel
import com.example.featureBook.module.local.BooksCacheRepository
import com.example.featureBook.module.mapper.toEntity
import com.example.featureBook.module.mapper.toUiModel
import com.example.featureBook.module.network.BooksRemoteRepository
import com.example.featureBook.usecase.base.FlowUseCase
import com.example.featureBook.usecase.base.UseCaseOutputWithStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetBookDetailUseCase @Inject constructor(
    private val remote: BooksRemoteRepository,
    private val cache: BooksCacheRepository
) : FlowUseCase<String, BookUiModel, BookUiModel>() {

    override suspend fun doWork(input: String): Flow<BookUiModel> = flow {
        val book = cache.getBookById(input)?.toUiModel()
            ?: remote.getBookDetail(input)
                ?.also { cache.saveBooks(listOf(it.toEntity())) }
                ?.toUiModel()
            ?: error("Book not found: $input")
        emit(book)
    }

    override suspend fun onSucceedDataHandling(
        intermediate: BookUiModel
    ): UseCaseOutputWithStatus.Success<BookUiModel> = UseCaseOutputWithStatus.Success(intermediate)
}
