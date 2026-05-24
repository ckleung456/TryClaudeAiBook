package com.example.featureBook.module.network

import android.util.Log
import com.example.featureBook.model.network.Book
import com.example.featureBook.model.network.BooksResponse
import com.example.featureBook.model.domain.MockBookData
import kotlinx.serialization.json.Json
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.delay
import javax.inject.Inject

@ViewModelScoped
class BooksRepository @Inject constructor(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
) {
    suspend fun loadBooks() : List<Book> = try {
        delay(1000)
        val response = json.decodeFromString<BooksResponse>(MockBookData.booksJson)
        response.books
    } catch (ex: Exception) {
        Log.w("BooksRepository", "Error parsing")
        emptyList()
    }

    suspend fun searchBook(bookId: String): Book? {
        delay(1000)
        return loadBooks().find { it.id == bookId }
    }
}