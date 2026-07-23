package com.example.featureBook.ui.list

import com.example.featureBook.model.domain.BookUiModel
import com.example.featureBook.model.domain.SortOrder
import com.example.featureBook.model.domain.ViewMode

data class BooksListState(
    val books: List<BookUiModel> = emptyList(),
    val viewMode: ViewMode = ViewMode.LIST,
    val sortOrder: SortOrder = SortOrder.ASCENDING,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val savedScrollIndex: Int = 0,
    val isRefreshing: Boolean = false
)

val BooksListState.displayedBooks: List<BookUiModel>
    get() = if (searchQuery.isBlank()) books
            else books.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.author.contains(searchQuery, ignoreCase = true)
            }