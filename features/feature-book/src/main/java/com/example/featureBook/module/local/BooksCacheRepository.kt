package com.example.featureBook.module.local

import com.example.core.domain.DataError
import com.example.core.domain.EmptyResult
import com.example.core.domain.Result
import com.example.featureBook.model.local.BookEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class BooksCacheRepository @Inject constructor(private val dao: BookDao) {
    fun observeBooks(): Flow<List<BookEntity>> = dao.observeBooks()

    suspend fun getBookById(id: String): Result<BookEntity, DataError.Local> = try {
        dao.getBookById(id)?.let { Result.Success(it) } ?: Result.Error(DataError.Local.NOT_FOUND)
    } catch (e: Exception) {
        Result.Error(DataError.Local.UNKNOWN)
    }

    suspend fun saveBooks(books: List<BookEntity>): EmptyResult<DataError.Local> = try {
        dao.upsertBooks(books)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(DataError.Local.UNKNOWN)
    }
}
