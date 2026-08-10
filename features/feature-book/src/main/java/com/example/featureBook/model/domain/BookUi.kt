package com.example.featureBook.model.domain

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class BookUi(
    val id: String,
    val title: String,
    val author: String,
    val coverUrl: String,
    val publishedYear: Int,
    val rating: Double,
    val description: String,
    val genres: ImmutableList<String>
)
