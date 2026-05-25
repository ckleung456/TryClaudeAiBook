package com.example.featureBook.ui.list

sealed interface BooksListEvent {
    data class NavigateToDetail(val bookId: String) : BooksListEvent
}