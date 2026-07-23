package com.example.featureBook.ui.list

import com.example.featureBook.model.domain.BookUi
import com.example.featureBook.model.domain.SortOrder
import com.example.featureBook.model.domain.ViewMode

data class BooksListState(
    val books: List<BookUi> = emptyList(),
    val viewMode: ViewMode = ViewMode.LIST,
    val sortOrder: SortOrder = SortOrder.ASCENDING,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val savedScrollIndex: Int = 0,
    val isRefreshing: Boolean = false
)

val BooksListState.displayedBooks: List<BookUi>
    get() = if (searchQuery.isBlank()) books
            else books.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.author.contains(searchQuery, ignoreCase = true)
            }