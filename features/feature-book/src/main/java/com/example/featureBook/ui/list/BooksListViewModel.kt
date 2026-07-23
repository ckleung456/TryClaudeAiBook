package com.example.featureBook.ui.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.featureBook.model.domain.BookUiModel
import com.example.featureBook.model.domain.SortOrder
import com.example.featureBook.model.domain.ViewMode
import com.example.featureBook.ui.UiState
import com.example.featureBook.ui.UiText
import com.example.featureBook.usecase.LoadBooksUseCase
import com.example.featureBook.usecase.base.UseCaseOutputWithStatus
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
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val KEY_SCROLL_INDEX = "scroll_index"

private data class ListInputs(
    val output: UseCaseOutputWithStatus<List<BookUiModel>>,
    val sortOrder: SortOrder,
    val viewMode: ViewMode,
    val searchQuery: String,
    val isSearchActive: Boolean
)

@HiltViewModel
class BooksListViewModel @Inject constructor(
    private val loadBooksUseCase: LoadBooksUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _sortOrder = MutableStateFlow(SortOrder.ASCENDING)
    private val _viewMode = MutableStateFlow(ViewMode.LIST)
    private val _searchQuery = MutableStateFlow("")
    private val _isSearchActive = MutableStateFlow(false)
    private val _refreshSignal = MutableStateFlow(0)

    private val _events = Channel<BooksListEvent>()
    val events: Flow<BooksListEvent> = _events.receiveAsFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val outputWithSortOrder: Flow<Pair<UseCaseOutputWithStatus<List<BookUiModel>>, SortOrder>> =
        combine(_sortOrder, _refreshSignal) { sortOrder, _ -> sortOrder }
            .flatMapLatest { sortOrder ->
                loadBooksUseCase.invoke(sortOrder).map { output -> output to sortOrder }
            }

    val state: StateFlow<UiState<BooksListState>> = combine(
        outputWithSortOrder,
        _viewMode,
        _searchQuery,
        _isSearchActive
    ) { (output, sortOrder), viewMode, query, isSearchActive ->
        ListInputs(output, sortOrder, viewMode, query, isSearchActive)
    }
        .scan(UiState.Loading as UiState<BooksListState>) { previous, inputs ->
            val savedScrollIndex = savedStateHandle.get<Int>(KEY_SCROLL_INDEX) ?: 0
            when (val output = inputs.output) {
                is UseCaseOutputWithStatus.Progress -> {
                    val previousData = (previous as? UiState.Success)?.data
                    if (previousData != null) {
                        UiState.Success(
                            previousData.copy(
                                viewMode = inputs.viewMode,
                                sortOrder = inputs.sortOrder,
                                searchQuery = inputs.searchQuery,
                                isSearchActive = inputs.isSearchActive,
                                savedScrollIndex = savedScrollIndex,
                                isRefreshing = true
                            )
                        )
                    } else {
                        UiState.Loading
                    }
                }
                is UseCaseOutputWithStatus.Success -> UiState.Success(
                    BooksListState(
                        books = output.result,
                        viewMode = inputs.viewMode,
                        sortOrder = inputs.sortOrder,
                        searchQuery = inputs.searchQuery,
                        isSearchActive = inputs.isSearchActive,
                        savedScrollIndex = savedScrollIndex,
                        isRefreshing = false
                    )
                )
                is UseCaseOutputWithStatus.Failed -> UiState.Error(
                    UiText.DynamicString(output.error.message ?: "Unknown error")
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
            BooksListAction.OnRefresh -> _refreshSignal.update { it + 1 }
        }
    }
}
