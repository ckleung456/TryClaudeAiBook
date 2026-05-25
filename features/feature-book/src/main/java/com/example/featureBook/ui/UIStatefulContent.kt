package com.example.featureBook.ui

import androidx.compose.runtime.Composable

@Composable
fun <T> UIStatefulContent(
    state: UiState<T>,
    loadingContent: @Composable () -> Unit,
    errorContent: @Composable (UiText) -> Unit,
    successContent: @Composable (T) -> Unit
) {
    when (state) {
        is UiState.Loading -> loadingContent()
        is UiState.Error -> errorContent(state.message)
        is UiState.Success -> successContent(state.data)
    }
}