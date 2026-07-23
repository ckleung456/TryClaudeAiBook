package com.example.featureBook.fake

import com.example.core.domain.DataError
import com.example.core.domain.Result
import com.example.featureBook.model.network.Book
import com.example.featureBook.module.network.BooksRemoteRepository

class FakeBooksRemoteRepository : BooksRemoteRepository() {
    var books: List<Book> = emptyList()
    var shouldThrow: Boolean = false

    override suspend fun loadBooks(): Result<List<Book>, DataError.Network> {
        if (shouldThrow) return Result.Error(DataError.Network.UNKNOWN)
        return Result.Success(books)
    }

    override suspend fun getBookDetail(bookId: String): Result<Book, DataError.Network> {
        if (shouldThrow) return Result.Error(DataError.Network.UNKNOWN)
        return books.find { it.id == bookId }
            ?.let { Result.Success(it) }
            ?: Result.Error(DataError.Network.NOT_FOUND)
    }
}
