package com.example.featureBook.module.local

import com.example.core.domain.DataError
import com.example.core.domain.EmptyResult
import com.example.core.domain.Result
import com.example.featureBook.model.local.BookEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BooksCacheRepository @Inject constructor(private val dao: BookDao) {
    fun observeBooks(): Flow<List<BookEntity>> = dao.observeBooks().flowOn(Dispatchers.IO)

    suspend fun getBookById(id: String): Result<BookEntity, DataError.Local> = withContext(Dispatchers.IO) {
        try {
            dao.getBookById(id)?.let { Result.Success(it) } ?: Result.Error(DataError.Local.NOT_FOUND)
        } catch (_: Exception) {
            Result.Error(DataError.Local.UNKNOWN)
        }
    }

    suspend fun saveBooks(books: List<BookEntity>): EmptyResult<DataError.Local> = withContext(Dispatchers.IO) {
        try {
            dao.upsertBooks(books)
            Result.Success(Unit)
        } catch (_: Exception) {
            Result.Error(DataError.Local.UNKNOWN)
        }
    }
}
