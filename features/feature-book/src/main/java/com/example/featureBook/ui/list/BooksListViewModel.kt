package com.example.featureBook.ui.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.featureBook.model.domain.SortOrder
import com.example.featureBook.model.domain.ViewMode
import com.example.featureBook.usecase.LoadBooksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private const val KEY_SCROLL_INDEX = "scroll_index"

@HiltViewModel
class BooksListViewModel @Inject constructor(
    private val loadBooksUseCase: LoadBooksUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _sortOrder = MutableStateFlow(SortOrder.ASCENDING)
    private val _viewMode = MutableStateFlow(ViewMode.LIST)
    private val _searchQuery = MutableStateFlow("")
    private val _isSearchActive = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<BooksListUiState> = _sortOrder
        .flatMapLatest { sortOrder -> loadBooksUseCase(sortOrder) }
        .combine(_viewMode) { booksResult, viewMode -> booksResult to viewMode }
        .combine(_searchQuery) { (booksResult, viewMode), query -> Triple(booksResult, viewMode, query) }
        .combine(_isSearchActive) { (booksResult, viewMode, query), isSearchActive ->
            val savedScrollIndex = savedStateHandle.get<Int>(KEY_SCROLL_INDEX) ?: 0
            when {
                booksResult.isSuccess -> BooksListUiState(
                    isLoading = false,
                    books = booksResult.getOrElse { emptyList() },
                    viewMode = viewMode,
                    sortOrder = _sortOrder.value,
                    searchQuery = query,
                    isSearchActive = isSearchActive,
                    savedScrollIndex = savedScrollIndex
                )
                else -> BooksListUiState(
                    isLoading = false,
                    error = booksResult.exceptionOrNull()?.message ?: "Unknown error",
                    viewMode = viewMode,
                    sortOrder = _sortOrder.value,
                    searchQuery = query,
                    isSearchActive = isSearchActive,
                    savedScrollIndex = savedScrollIndex
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BooksListUiState()
        )

    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
    }

    fun toggleSortOrder() {
        _sortOrder.value = if (_sortOrder.value == SortOrder.ASCENDING) SortOrder.DESCENDING else SortOrder.ASCENDING
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) _searchQuery.value = ""
    }

    fun saveScrollPosition(index: Int) {
        savedStateHandle[KEY_SCROLL_INDEX] = index
    }
}
