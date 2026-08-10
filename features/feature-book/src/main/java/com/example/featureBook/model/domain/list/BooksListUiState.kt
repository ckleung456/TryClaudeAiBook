package com.example.featureBook.model.domain.list

import androidx.compose.runtime.Immutable
import com.example.featureBook.model.domain.BookUi
import com.example.featureBook.model.domain.SortOrder
import com.example.featureBook.model.domain.ViewMode
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class BooksListState(
    val books: ImmutableList<BookUi> = persistentListOf(),
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