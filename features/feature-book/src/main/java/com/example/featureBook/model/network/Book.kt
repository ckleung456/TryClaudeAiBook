package com.example.featureBook.model.network

import kotlinx.serialization.Serializable
import java.time.LocalTime

@Serializable
data class Book(
    val id: String,
    val title: String,
    val author: String,
    val coverUrl: String,
    val publishedYear: Int,
    val rating: Double,
    val description: String? = null,
    val genres: List<String> = emptyList(),
    val createdAt: Long = LocalTime.now().toNanoOfDay()
)

@Serializable
data class BooksResponse(
    val books: List<Book>,
    val total: Int,
    val page: Int,
    val limit: Int,
    val totalPages: Int,
    val hasNext: Boolean
)
