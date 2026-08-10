package com.example.featureBook.model.domain.detail

sealed interface BookDetailEvent {
    data object NavigateBack : BookDetailEvent
}