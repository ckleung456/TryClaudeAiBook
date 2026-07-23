package com.example.core.presentation

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: UiText, val errorData: Any? = null) : UiState<Nothing>
}
