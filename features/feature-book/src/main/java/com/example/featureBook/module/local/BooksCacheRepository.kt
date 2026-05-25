package com.example.featureBook.module.local

import com.example.featureBook.model.local.BookEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class BooksCacheRepository @Inject constructor(private val dao: BookDao) {
    fun observeBooks(): Flow<List<BookEntity>> = dao.observeBooks()
    suspend fun getBookById(id: String): BookEntity? = dao.getBookById(id)
    suspend fun saveBooks(books: List<BookEntity>) = dao.upsertBooks(books)
}
