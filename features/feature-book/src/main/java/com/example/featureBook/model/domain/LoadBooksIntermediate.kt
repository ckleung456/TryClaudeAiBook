package com.example.featureBook.model.domain

import com.example.featureBook.model.local.BookEntity

data class LoadBooksIntermediate(
    val entities: List<BookEntity>,
    val sortOrder: SortOrder,
    val isFromCache: Boolean
)

