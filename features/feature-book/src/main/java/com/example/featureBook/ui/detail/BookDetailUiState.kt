package com.example.featureBook.ui.detail

import com.example.featureBook.model.domain.BookUiModel

sealed interface BookDetailUiState {
    data object Loading : BookDetailUiState
    data class Success(val book: BookUiModel) : BookDetailUiState
    data class Error(val message: String) : BookDetailUiState
}
