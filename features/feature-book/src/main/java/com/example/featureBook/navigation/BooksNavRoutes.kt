package com.example.featureBook.navigation

import kotlinx.serialization.Serializable

@Serializable
object BooksListRoute

@Serializable
data class BookDetailRoute(val bookId: String)
