package com.example.featureBook.ui.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.featureBook.model.domain.SortOrder
import com.example.featureBook.model.domain.ViewMode
import com.example.featureBook.ui.UiState
import com.example.featureBook.ui.UiText
import com.example.featureBook.usecase.LoadBooksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

    private val _events = Channel<BooksListEvent>()
    val events: Flow<BooksListEvent> = _events.receiveAsFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<UiState<BooksListState>> = _sortOrder
        .flatMapLatest { sortOrder ->
            loadBooksUseCase(sortOrder).map { result -> result to sortOrder }
        }
        .combine(_viewMode) { (booksResult, sortOrder), viewMode ->
            Triple(booksResult, sortOrder, viewMode)
        }
        .combine(_searchQuery) { (booksResult, sortOrder, viewMode), query ->
            Pair(booksResult to sortOrder, viewMode to query)
        }
        .combine(_isSearchActive) { (resultAndSort, viewAndQuery), isSearchActive ->
            val (booksResult, sortOrder) = resultAndSort
            val (viewMode, query) = viewAndQuery
            val savedScrollIndex = savedStateHandle.get<Int>(KEY_SCROLL_INDEX) ?: 0
            when {
                booksResult.isSuccess -> UiState.Success(
                    BooksListState(
                        books = booksResult.getOrElse { emptyList() },
                        viewMode = viewMode,
                        sortOrder = sortOrder,
                        searchQuery = query,
                        isSearchActive = isSearchActive,
                        savedScrollIndex = savedScrollIndex
                    )
                )
                else -> UiState.Error(
                    UiText.DynamicString(booksResult.exceptionOrNull()?.message ?: "Unknown error")
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Loading
        )

    fun onAction(action: BooksListAction) {
        when (action) {
            BooksListAction.OnToggleViewMode ->
                _viewMode.update { if (it == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST }
            BooksListAction.OnToggleSortOrder ->
                _sortOrder.update { if (it == SortOrder.ASCENDING) SortOrder.DESCENDING else SortOrder.ASCENDING }
            is BooksListAction.OnUpdateSearchQuery -> _searchQuery.value = action.query
            is BooksListAction.OnSetSearchActive -> {
                _isSearchActive.value = action.active
                if (!action.active) _searchQuery.value = ""
            }
            is BooksListAction.OnBookClick -> viewModelScope.launch {
                _events.send(BooksListEvent.NavigateToDetail(action.bookId))
            }
            is BooksListAction.OnSaveScrollPosition ->
                savedStateHandle[KEY_SCROLL_INDEX] = action.index
        }
    }
}
