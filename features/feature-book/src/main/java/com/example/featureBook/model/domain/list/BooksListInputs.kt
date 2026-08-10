package com.example.featureBook.model.domain.list

import com.example.core.domain.DataError
import com.example.core.presentation.UseCaseOutputWithStatus
import com.example.featureBook.model.domain.LoadBooksResult
import com.example.featureBook.model.domain.SortOrder
import com.example.featureBook.model.domain.ViewMode

data class BooksListInputs(
    val output: UseCaseOutputWithStatus<LoadBooksResult, DataError>,
    val sortOrder: SortOrder,
    val viewMode: ViewMode,
    val searchQuery: String,
    val isSearchActive: Boolean
)