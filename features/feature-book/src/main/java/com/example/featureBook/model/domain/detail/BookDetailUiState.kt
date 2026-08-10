package com.example.featureBook.model.domain.detail

import androidx.compose.runtime.Immutable
import com.example.featureBook.model.domain.BookUi

@Immutable
data class BookDetailState(
    val book: BookUi
)
