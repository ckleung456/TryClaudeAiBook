package com.example.featureBook.module.network

import com.example.core.domain.DataError
import com.example.core.domain.Result
import com.example.featureBook.model.domain.MockBookData
import com.example.featureBook.model.network.Book
import com.example.featureBook.model.network.BooksResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
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

    open suspend fun loadBooks(): Result<List<Book>, DataError.Network> = withContext(Dispatchers.IO) {
        try {
            delay(10000)
            Result.Success(json.decodeFromString<BooksResponse>(MockBookData.booksJson).books)
        } catch (e: SerializationException) {
            Result.Error(DataError.Network.SERIALIZATION)
        } catch (e: Exception) {
            Result.Error(DataError.Network.UNKNOWN)
        }
    }

    open suspend fun getBookDetail(bookId: String): Result<Book, DataError.Network> = withContext(Dispatchers.IO) {
        delay(500)
        when (val result = loadBooks()) {
            is Result.Success -> result.data.find { it.id == bookId }
                ?.let { Result.Success(it) }
                ?: Result.Error(DataError.Network.NOT_FOUND)
            is Result.Error -> result
        }
    }
}
