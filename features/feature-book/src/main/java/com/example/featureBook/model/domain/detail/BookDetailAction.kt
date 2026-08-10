package com.example.featureBook.model.domain.detail

sealed interface BookDetailAction {
    data object OnRetry : BookDetailAction
    data object OnBackClick : BookDetailAction
}