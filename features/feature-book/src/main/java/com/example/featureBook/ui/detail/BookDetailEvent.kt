package com.example.featureBook.ui.detail

sealed interface BookDetailEvent {
    data object NavigateBack : BookDetailEvent
}