package com.example.featureBook.ui.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.featureBook.fake.FakeBookDao
import com.example.featureBook.fake.FakeBooksRemoteRepository
import com.example.featureBook.model.local.BookEntity
import com.example.featureBook.module.local.BooksCacheRepository
import com.example.featureBook.ui.UiState
import com.example.featureBook.usecase.GetBookDetailUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private lateinit var fakeRemote: FakeBooksRemoteRepository
    private lateinit var fakeDao: FakeBookDao

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRemote = FakeBooksRemoteRepository()
        fakeDao = FakeBookDao()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(bookId: String): BookDetailViewModel =
        BookDetailViewModel(
            GetBookDetailUseCase(fakeRemote, BooksCacheRepository(fakeDao)),
            SavedStateHandle(mapOf("bookId" to bookId))
        )

    @Test
    fun `state transitions from Loading to Success when book is found`() = runTest {
        fakeDao.seed(listOf(makeEntity("book_1", "Test Book")))
        val viewModel = buildViewModel("book_1")

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state is UiState.Success)
            assertEquals("Test Book", (state as UiState.Success<BookDetailState>).data.book.title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `state transitions to Error when use case returns failure`() = runTest {
        fakeRemote.shouldThrow = true
        val viewModel = buildViewModel("book_1")

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state is UiState.Error)
            assertTrue((state as UiState.Error).message.asString().isNotEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retry via OnRetry action emits Loading then resolves to Success`() = runTest {
        fakeDao.seed(listOf(makeEntity("book_1", "Test Book")))
        val viewModel = buildViewModel("book_1")

        viewModel.state.test {
            awaitItem() // Success from init
            viewModel.onAction(BookDetailAction.OnRetry)
            val loading = awaitItem()
            assertTrue(loading is UiState.Loading)
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(viewModel.state.value is UiState.Success)
    }

    private fun makeEntity(id: String, title: String) = BookEntity(
        id = id, title = title, author = "Author", coverUrl = "",
        publishedYear = 2020, rating = 4.0, description = "", genres = "", createdAt = 0L
    )
}

private fun com.example.featureBook.ui.UiText.asString(): String =
    (this as com.example.featureBook.ui.UiText.DynamicString).value