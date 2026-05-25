package com.example.featureBook.ui.list

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.featureBook.fake.FakeBookDao
import com.example.featureBook.fake.FakeBooksRemoteRepository
import com.example.featureBook.model.domain.BookUiModel
import com.example.featureBook.model.domain.SortOrder
import com.example.featureBook.model.domain.ViewMode
import com.example.featureBook.module.local.BooksCacheRepository
import com.example.featureBook.ui.UiState
import com.example.featureBook.ui.UiText
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

    private suspend fun app.cash.turbine.TurbineTestContext<UiState<BooksListState>>.skipLoadingIfPresent(): UiState<BooksListState> {
        val first = awaitItem()
        return if (first is UiState.Loading) awaitItem() else first
    }

    @Test
    fun `initial state has loading true`() {
        assertTrue(viewModel.state.value is UiState.Loading)
    }

    @Test
    fun `state transitions to success with books after use case emits`() = runTest {
        fakeUseCase.books = listOf(makeUiBook("1", "Alpha"), makeUiBook("2", "Beta"))

        viewModel.state.test {
            val state = skipLoadingIfPresent()
            assertTrue(state is UiState.Success)
            assertEquals(2, (state as UiState.Success).data.books.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `state shows error when use case emits failure`() = runTest {
        fakeUseCase.error = RuntimeException("Load failed")

        viewModel.state.test {
            val state = skipLoadingIfPresent()
            assertTrue(state is UiState.Error)
            assertEquals("Load failed", ((state as UiState.Error).message as UiText.DynamicString).value)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleViewMode switches from LIST to GRID`() = runTest {
        fakeUseCase.books = emptyList()

        viewModel.state.test {
            skipLoadingIfPresent()
            viewModel.onAction(BooksListAction.OnToggleViewMode)
            val updated = awaitItem()
            assertEquals(ViewMode.GRID, (updated as UiState.Success).data.viewMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleViewMode switches from GRID back to LIST`() = runTest {
        fakeUseCase.books = emptyList()

        viewModel.state.test {
            skipLoadingIfPresent()
            viewModel.onAction(BooksListAction.OnToggleViewMode)
            awaitItem() // GRID
            viewModel.onAction(BooksListAction.OnToggleViewMode)
            val updated = awaitItem()
            assertEquals(ViewMode.LIST, (updated as UiState.Success).data.viewMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleSortOrder cycles ASCENDING to DESCENDING`() = runTest {
        fakeUseCase.books = emptyList()

        viewModel.state.test {
            skipLoadingIfPresent() // initial ASCENDING
            viewModel.onAction(BooksListAction.OnToggleSortOrder)
            val descState = skipLoadingIfPresent()
            assertEquals(SortOrder.DESCENDING, (descState as UiState.Success).data.sortOrder)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateSearchQuery filters displayed books by title`() = runTest {
        fakeUseCase.books = listOf(
            makeUiBook("1", "Kotlin Guide"),
            makeUiBook("2", "Java Basics")
        )

        viewModel.state.test {
            skipLoadingIfPresent()
            viewModel.onAction(BooksListAction.OnUpdateSearchQuery("Kotlin"))
            val filtered = awaitItem()
            val data = (filtered as UiState.Success).data
            assertEquals(1, data.displayedBooks.size)
            assertEquals("Kotlin Guide", data.displayedBooks.first().title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `updateSearchQuery filters displayed books by author`() = runTest {
        fakeUseCase.books = listOf(
            makeUiBook("1", "Book A", author = "Martin Fowler"),
            makeUiBook("2", "Book B", author = "Jane Doe")
        )

        viewModel.state.test {
            skipLoadingIfPresent()
            viewModel.onAction(BooksListAction.OnUpdateSearchQuery("Martin"))
            val filtered = awaitItem()
            val data = (filtered as UiState.Success).data
            assertEquals(1, data.displayedBooks.size)
            assertEquals("Martin Fowler", data.displayedBooks.first().author)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setSearchActive false clears query and shows all books`() = runTest {
        fakeUseCase.books = listOf(makeUiBook("1", "Kotlin"), makeUiBook("2", "Java"))

        viewModel.state.test {
            skipLoadingIfPresent()
            viewModel.onAction(BooksListAction.OnUpdateSearchQuery("Kotlin"))
            awaitItem() // filtered to 1
            viewModel.onAction(BooksListAction.OnSetSearchActive(false))
            val restored = awaitItem()
            val data = (restored as UiState.Success).data
            assertEquals(2, data.displayedBooks.size)
            assertFalse(data.isSearchActive)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `saveScrollPosition persists index in SavedStateHandle`() {
        val handle = SavedStateHandle()
        val vm = BooksListViewModel(fakeUseCase, handle)
        vm.onAction(BooksListAction.OnSaveScrollPosition(7))
        assertEquals(7, handle.get<Int>("scroll_index"))
    }

    @Test
    fun `savedScrollIndex is restored from SavedStateHandle on creation`() = runTest {
        val handle = SavedStateHandle(mapOf("scroll_index" to 5))
        val vm = BooksListViewModel(fakeUseCase, handle)

        vm.state.test {
            val state = skipLoadingIfPresent()
            assertEquals(5, (state as UiState.Success).data.savedScrollIndex)
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
