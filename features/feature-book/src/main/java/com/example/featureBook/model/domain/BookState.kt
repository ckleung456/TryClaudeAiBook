package com.example.featureBook.model.domain

import com.example.featureBook.model.network.Book

data class BooksState(
    val books: List<Book> = emptyList(),
    val filteredBooks: List<Book> = emptyList(),
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val currentPage: Int = 1,
    val totalPages: Int = 0,
    val totalBooks: Int = 0,
    val viewMode: ViewMode = ViewMode.GRID,
    val hasMore: Boolean = true,
    val filterOptions: FilterOptions = FilterOptions()
)

data class FilterOptions(
    val genre: String? = null,
    val minRating: Double = 0.0,
    val yearFrom: Int? = null,
    val yearTo: Int? = null
)

sealed class BooksEvent {
    data class ShowError(val message: String) : BooksEvent()
    data class BookSelected(val book: Book) : BooksEvent()
    object ShowLoading : BooksEvent()
    object HideLoading : BooksEvent()
}

enum class ViewMode {
    GRID, LIST
}