package com.example.featureBook.ui.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.featureBook.fake.FakeBookDao
import com.example.featureBook.fake.FakeBooksRemoteRepository
import com.example.featureBook.model.domain.BookUiModel
import com.example.featureBook.module.local.BooksCacheRepository
import com.example.featureBook.usecase.GetBookDetailUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.yield
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(bookId: String, useCase: GetBookDetailUseCase): BookDetailViewModel =
        BookDetailViewModel(useCase, SavedStateHandle(mapOf("bookId" to bookId)))

    @Test
    fun `state transitions from Loading to Success when book is found`() = runTest {
        val fakeUseCase = FakeGetBookDetailUseCase(
            Result.success(makeUiBook("book_1", "Test Book"))
        )
        val viewModel = buildViewModel("book_1", fakeUseCase)

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is BookDetailUiState.Success)
            assertEquals("Test Book", (state as BookDetailUiState.Success).book.title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `state transitions to Error when use case returns failure`() = runTest {
        val fakeUseCase = FakeGetBookDetailUseCase(
            Result.failure(Exception("Book not found"))
        )
        val viewModel = buildViewModel("book_1", fakeUseCase)

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is BookDetailUiState.Error)
            assertTrue((state as BookDetailUiState.Error).message.isNotEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retry via loadBookDetail emits Loading then resolves to Success`() = runTest {
        val fakeUseCase = FakeGetBookDetailUseCase(
            Result.success(makeUiBook("book_1", "Test Book"))
        )
        val viewModel = buildViewModel("book_1", fakeUseCase)

        viewModel.uiState.test {
            awaitItem() // Success from init
            viewModel.loadBookDetail()
            // StateFlow emits Loading (different from previous Success)
            val loading = awaitItem()
            assertTrue(loading is BookDetailUiState.Loading)
            // StateFlow won't re-emit the same Success value, so check value directly
            cancelAndIgnoreRemainingEvents()
        }
        // Verify final state is Success after retry
        assertTrue(viewModel.uiState.value is BookDetailUiState.Success)
    }

    private fun makeUiBook(id: String, title: String) = BookUiModel(
        id = id, title = title, author = "Author", coverUrl = "",
        publishedYear = 2020, rating = 4.0, description = "", genres = emptyList()
    )
}

class FakeGetBookDetailUseCase(
    private val result: Result<BookUiModel>
) : GetBookDetailUseCase(
    FakeBooksRemoteRepository(),
    BooksCacheRepository(FakeBookDao())
) {
    override suspend fun invoke(bookId: String): Result<BookUiModel> {
        yield()
        return result
    }
}
