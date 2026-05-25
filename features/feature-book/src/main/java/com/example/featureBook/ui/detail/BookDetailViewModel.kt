package com.example.featureBook.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.featureBook.ui.UiState
import com.example.featureBook.ui.UiText
import com.example.featureBook.usecase.GetBookDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookDetailViewModel @Inject constructor(
    private val getBookDetailUseCase: GetBookDetailUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val bookId: String = checkNotNull(savedStateHandle["bookId"])

    private val _state = MutableStateFlow<UiState<BookDetailState>>(UiState.Loading)
    val state: StateFlow<UiState<BookDetailState>> = _state.asStateFlow()

    private val _events = Channel<BookDetailEvent>()
    val events: Flow<BookDetailEvent> = _events.receiveAsFlow()

    init {
        loadBookDetail()
    }

    fun onAction(action: BookDetailAction) {
        when (action) {
            BookDetailAction.OnRetry -> loadBookDetail()
            BookDetailAction.OnBackClick -> viewModelScope.launch {
                _events.send(BookDetailEvent.NavigateBack)
            }
        }
    }

    private fun loadBookDetail() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            getBookDetailUseCase(bookId).fold(
                onSuccess = { _state.value = UiState.Success(BookDetailState(it)) },
                onFailure = { _state.value = UiState.Error(UiText.DynamicString(it.message ?: "Unknown error")) }
            )
        }
    }
}
