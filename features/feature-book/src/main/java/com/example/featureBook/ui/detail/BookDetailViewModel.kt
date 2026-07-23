package com.example.featureBook.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.presentation.UiState
import com.example.featureBook.ui.toUiText
import com.example.featureBook.usecase.GetBookDetailUseCase
import com.example.core.presentation.UseCaseOutputWithStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
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
            BookDetailAction.OnRetry -> retry()
            BookDetailAction.OnBackClick -> navigateBack()
        }
    }

    private fun retry() {
        loadBookDetail()
    }

    private fun navigateBack() {
        viewModelScope.launch {
            _events.send(BookDetailEvent.NavigateBack)
        }
    }

    private fun loadBookDetail() {
        viewModelScope.launch {
            getBookDetailUseCase.invoke(bookId).collect { output ->
                when (output) {
                    is UseCaseOutputWithStatus.Progress ->
                        _state.update { UiState.Loading }
                    is UseCaseOutputWithStatus.Success ->
                        _state.update { UiState.Success(BookDetailState(output.result)) }
                    is UseCaseOutputWithStatus.Failed ->
                        _state.update { current ->
                            UiState.Error(
                                message = output.error.toUiText(),
                                errorData = (current as? UiState.Success)?.data
                            )
                        }
                }
            }
        }
    }
}
