package com.example.featureBook.fake

import com.example.featureBook.model.local.BookEntity
import com.example.featureBook.module.local.BookDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.yield

class FakeBookDao : BookDao {
    private val booksFlow = MutableStateFlow<List<BookEntity>>(emptyList())

    fun seed(books: List<BookEntity>) {
        booksFlow.value = books
    }

    override fun observeBooks(): Flow<List<BookEntity>> = booksFlow

    override suspend fun getBookById(id: String): BookEntity? {
        yield() // real suspension point so StateFlow-based collectors observe transient states
        return booksFlow.value.find { it.id == id }
    }

    override suspend fun upsertBooks(books: List<BookEntity>) {
        yield()
        val merged = (booksFlow.value + books).distinctBy { it.id }
        booksFlow.value = merged
    }

    override suspend fun clearAll() {
        yield()
        booksFlow.value = emptyList()
    }
}
