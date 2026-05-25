package com.example.featureBook.fake

import com.example.featureBook.model.network.Book
import com.example.featureBook.module.network.BooksRemoteRepository

class FakeBooksRemoteRepository : BooksRemoteRepository() {
    var books: List<Book> = emptyList()
    var shouldThrow: Boolean = false

    override suspend fun loadBooks(): List<Book> {
        if (shouldThrow) throw RuntimeException("Network error")
        return books
    }

    override suspend fun getBookDetail(bookId: String): Book? {
        if (shouldThrow) throw RuntimeException("Network error")
        return books.find { it.id == bookId }
    }
}
