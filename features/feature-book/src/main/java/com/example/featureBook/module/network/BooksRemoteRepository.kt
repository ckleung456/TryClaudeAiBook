package com.example.featureBook.module.network

import android.util.Log
import com.example.featureBook.model.domain.MockBookData
import com.example.featureBook.model.network.Book
import com.example.featureBook.model.network.BooksResponse
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class BooksRemoteRepository @Inject constructor() {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    open suspend fun loadBooks(): List<Book> = try {
        delay(1000)
        json.decodeFromString<BooksResponse>(MockBookData.booksJson).books
    } catch (ex: Exception) {
        Log.w("BooksRemoteRepository", "Error parsing books", ex)
        emptyList()
    }

    open suspend fun getBookDetail(bookId: String): Book? {
        delay(500)
        return loadBooks().find { it.id == bookId }
    }
}
