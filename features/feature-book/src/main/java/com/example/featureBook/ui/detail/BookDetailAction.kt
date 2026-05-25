package com.example.featureBook.ui.detail

sealed interface BookDetailAction {
    data object OnRetry : BookDetailAction
    data object OnBackClick : BookDetailAction
}