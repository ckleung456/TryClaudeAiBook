package com.example.featureBook.model.domain

data class LoadBooksResult(
    val books: List<BookUi>,
    val isFromCache: Boolean
)
