package com.example.featureBook.model.domain

data class BookUiModel(
    val id: String,
    val title: String,
    val author: String,
    val coverUrl: String,
    val publishedYear: Int,
    val rating: Double,
    val description: String,
    val genres: List<String>
)
