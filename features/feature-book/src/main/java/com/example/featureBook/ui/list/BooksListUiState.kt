package com.example.featureBook.ui.list

import com.example.featureBook.model.domain.BookUiModel
import com.example.featureBook.model.domain.SortOrder
import com.example.featureBook.model.domain.ViewMode

data class BooksListUiState(
    val isLoading: Boolean = true,
    val books: List<BookUiModel> = emptyList(),
    val error: String? = null,
    val viewMode: ViewMode = ViewMode.LIST,
    val sortOrder: SortOrder = SortOrder.ASCENDING,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val savedScrollIndex: Int = 0
)

val BooksListUiState.displayedBooks: List<BookUiModel>
    get() = if (searchQuery.isBlank()) books
            else books.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.author.contains(searchQuery, ignoreCase = true)
            }
