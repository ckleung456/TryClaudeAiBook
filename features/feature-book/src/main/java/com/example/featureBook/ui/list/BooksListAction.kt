package com.example.featureBook.ui.list

sealed interface BooksListAction {
    data object OnToggleViewMode : BooksListAction
    data object OnToggleSortOrder : BooksListAction
    data class OnUpdateSearchQuery(val query: String) : BooksListAction
    data class OnSetSearchActive(val active: Boolean) : BooksListAction
    data class OnBookClick(val bookId: String) : BooksListAction
    data class OnSaveScrollPosition(val index: Int) : BooksListAction
    data object OnRefresh : BooksListAction
}