package com.example.featureBook.module.mapper

import com.example.featureBook.model.domain.BookUiModel
import com.example.featureBook.model.domain.SortOrder
import com.example.featureBook.model.local.BookEntity
import com.example.featureBook.model.network.Book

fun Book.toEntity(): BookEntity = BookEntity(
    id = id,
    title = title,
    author = author,
    coverUrl = coverUrl,
    publishedYear = publishedYear,
    rating = rating,
    description = description,
    genres = genres.joinToString(","),
    createdAt = createdAt
)

fun BookEntity.toUiModel(): BookUiModel = BookUiModel(
    id = id,
    title = title,
    author = author,
    coverUrl = coverUrl,
    publishedYear = publishedYear,
    rating = rating,
    description = description ?: "",
    genres = if (genres.isBlank()) emptyList() else genres.split(",")
)

fun Book.toUiModel(): BookUiModel = BookUiModel(
    id = id,
    title = title,
    author = author,
    coverUrl = coverUrl,
    publishedYear = publishedYear,
    rating = rating,
    description = description ?: "",
    genres = genres
)

fun List<BookUiModel>.sortedBySortOrder(order: SortOrder): List<BookUiModel> =
    if (order == SortOrder.ASCENDING) {
        sortedBy { it.title.firstOrNull()?.lowercaseChar() }
    } else {
        sortedByDescending { it.title.firstOrNull()?.lowercaseChar() }
    }
