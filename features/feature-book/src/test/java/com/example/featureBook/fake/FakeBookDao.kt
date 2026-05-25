package com.example.featureBook.fake

import com.example.featureBook.model.local.BookEntity
import com.example.featureBook.module.local.BookDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeBookDao : BookDao {
    private val booksFlow = MutableStateFlow<List<BookEntity>>(emptyList())

    fun seed(books: List<BookEntity>) {
        booksFlow.value = books
    }

    override fun observeBooks(): Flow<List<BookEntity>> = booksFlow

    override suspend fun getBookById(id: String): BookEntity? =
        booksFlow.value.find { it.id == id }

    override suspend fun upsertBooks(books: List<BookEntity>) {
        val merged = (booksFlow.value + books).distinctBy { it.id }
        booksFlow.value = merged
    }

    override suspend fun clearAll() {
        booksFlow.value = emptyList()
    }
}
