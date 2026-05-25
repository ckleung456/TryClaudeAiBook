package com.example.featureBook.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val coverUrl: String,
    val publishedYear: Int,
    val rating: Double,
    val description: String?,
    val genres: String,
    val createdAt: Long
)
