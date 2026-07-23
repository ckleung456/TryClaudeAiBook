package com.example.core.presentation

import androidx.compose.runtime.Composable

@Composable
fun <T> UiStatefulContent(
    state: UiState<T>,
    loadingContent: @Composable () -> Unit,
    errorContent: @Composable (UiText, Any?) -> Unit,
    successContent: @Composable (T) -> Unit
) {
    when (state) {
        is UiState.Loading -> loadingContent()
        is UiState.Error -> errorContent(state.message, state.errorData)
        is UiState.Success -> successContent(state.data)
    }
}
