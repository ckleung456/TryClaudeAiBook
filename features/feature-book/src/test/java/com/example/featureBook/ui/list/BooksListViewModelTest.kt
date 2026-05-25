package com.example.featureBook.ui.list

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.featureBook.fake.FakeBookDao
import com.example.featureBook.fake.FakeBooksRemoteRepository
import com.example.featureBook.model.domain.BookUiModel
import com.example.featureBook.model.domain.SortOrder
import com.example.featureBook.model.domain.ViewMode
import com.example.featureBook.module.local.BooksCacheRepository
import com.example.featureBook.usecase.LoadBooksUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BooksListViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeUseCase: FakeLoadBooksUseCase
    private lateinit var viewModel: BooksListViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeUseCase = FakeLoadBooksUseCase()
        viewModel = BooksListViewModel(fakeUseCase, SavedStateHandle())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Helper: skips the initial loading value from WhileSubscribed's StateFlow replay
    private suspend fun app.cash.turbine.TurbineTestContext<BooksListUiState>.skipLoadingIfPresent(): BooksListUiState {
        val first = awaitItem()
        return if (first.isLoading) awaitItem() else first
    }

    @Test
    fun `initial state has loading true`() {
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `state transitions to success with books after use case emits`() = runTest {
        fakeUseCase.books = listOf(makeUiBook("1", "Alpha"), makeUiBook("2", "Beta"))

        viewModel.uiState.test {
            val state = skipLoadingIfPresent()
            assertFalse(state.isLoading)
            assertEquals(2, state.books.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `state shows error when use case emits failure`() = runTest {
        fakeUseCase.error = RuntimeException("Load failed")

        viewModel.uiState.test {
            val state = skipLoadingIfPresent()
            assertFalse(state.isLoading)
            assertEquals("Load failed", state.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleViewMode switches from LIST to GRID`() = runTest {
        fakeUseCase.books = emptyList()

        viewModel.uiState.test {
            skipLoadingIfPresent()
            viewModel.toggleViewMode()
            val updated = awaitItem()
            assertEquals(ViewMode.GRID, updated.viewMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleViewMode switches from GRID back to LIST`() = runTest {
        fakeUseCase.books = emptyList()

        viewModel.uiState.test {
            skipLoadingIfPresent()
            viewModel.toggleViewMode()
            awaitItem() // GRID
            viewModel.toggleViewMode()
            val updated = awaitItem()
            assertEquals(ViewMode.LIST, updated.viewMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleSortOrder cycles ASCENDING to DESCENDING`() = runTest {
        fakeUseCase.books = emptyList()

        viewModel.uiState.test {
            skipLoadingIfPresent() // initial ASCENDING
            viewModel.toggleSortOrder()
            val descState = skipLoadingIfPresent()
            assertEquals(SortOrder.DESCENDING, descState.sortOrder)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateSearchQuery filters displayed books by title`() = runTest {
        fakeUseCase.books = listOf(
            makeUiBook("1", "Kotlin Guide"),
            makeUiBook("2", "Java Basics")
        )

        viewModel.uiState.test {
            skipLoadingIfPresent()
            viewModel.updateSearchQuery("Kotlin")
            val filtered = awaitItem()
            assertEquals(1, filtered.displayedBooks.size)
            assertEquals("Kotlin Guide", filtered.displayedBooks.first().title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateSearchQuery filters displayed books by author`() = runTest {
        fakeUseCase.books = listOf(
            makeUiBook("1", "Book A", author = "Martin Fowler"),
            makeUiBook("2", "Book B", author = "Jane Doe")
        )

        viewModel.uiState.test {
            skipLoadingIfPresent()
            viewModel.updateSearchQuery("Martin")
            val filtered = awaitItem()
            assertEquals(1, filtered.displayedBooks.size)
            assertEquals("Martin Fowler", filtered.displayedBooks.first().author)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setSearchActive false clears query and shows all books`() = runTest {
        fakeUseCase.books = listOf(makeUiBook("1", "Kotlin"), makeUiBook("2", "Java"))

        viewModel.uiState.test {
            skipLoadingIfPresent()
            viewModel.updateSearchQuery("Kotlin")
            awaitItem() // filtered to 1
            viewModel.setSearchActive(false)
            val restored = awaitItem()
            assertEquals(2, restored.displayedBooks.size)
            assertFalse(restored.isSearchActive)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `saveScrollPosition persists index in SavedStateHandle`() {
        val handle = SavedStateHandle()
        val vm = BooksListViewModel(fakeUseCase, handle)
        vm.saveScrollPosition(7)
        assertEquals(7, handle.get<Int>("scroll_index"))
    }

    @Test
    fun `savedScrollIndex is restored from SavedStateHandle on creation`() = runTest {
        val handle = SavedStateHandle(mapOf("scroll_index" to 5))
        val vm = BooksListViewModel(fakeUseCase, handle)

        vm.uiState.test {
            val state = skipLoadingIfPresent()
            assertEquals(5, state.savedScrollIndex)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun makeUiBook(id: String, title: String, author: String = "Author") = BookUiModel(
        id = id, title = title, author = author, coverUrl = "",
        publishedYear = 2020, rating = 4.0, description = "", genres = emptyList()
    )
}

class FakeLoadBooksUseCase : LoadBooksUseCase(
    FakeBooksRemoteRepository(),
    BooksCacheRepository(FakeBookDao())
) {
    var books: List<BookUiModel> = emptyList()
    var error: Throwable? = null

    override operator fun invoke(sortOrder: SortOrder): Flow<Result<List<BookUiModel>>> =
        error?.let { flowOf(Result.failure(it)) }
            ?: flowOf(Result.success(books))
}
