package com.example.featureBook.model.domain.list

sealed interface BooksListEvent {
    data class NavigateToDetail(val bookId: String) : BooksListEvent
}