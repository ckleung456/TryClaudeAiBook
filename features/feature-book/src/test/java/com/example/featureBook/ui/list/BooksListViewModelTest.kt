package com.example.featureBook.ui.list

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.featureBook.fake.FakeBookDao
import com.example.featureBook.fake.FakeBooksRemoteRepository
import com.example.featureBook.model.domain.SortOrder
import com.example.featureBook.model.domain.ViewMode
import com.example.featureBook.model.local.BookEntity
import com.example.featureBook.module.local.BooksCacheRepository
import com.example.core.presentation.UiState
import com.example.core.presentation.UiText
import com.example.featureBook.usecase.LoadBooksUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private lateinit var fakeRemote: FakeBooksRemoteRepository
    private lateinit var fakeDao: FakeBookDao
    private lateinit var loadBooksUseCase: LoadBooksUseCase
    private lateinit var viewModel: BooksListViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRemote = FakeBooksRemoteRepository()
        fakeDao = FakeBookDao()
        loadBooksUseCase = LoadBooksUseCase(fakeRemote, BooksCacheRepository(fakeDao))
        viewModel = BooksListViewModel(loadBooksUseCase, SavedStateHandle())
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
        fakeDao.seed(listOf(makeEntity("1", "Alpha"), makeEntity("2", "Beta")))

        viewModel.state.test {
            val state = skipLoadingIfPresent()
            assertTrue(state is UiState.Success)
            assertEquals(2, (state as UiState.Success).data.books.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `state shows error when use case emits failure`() = runTest {
        fakeRemote.shouldThrow = true

        viewModel.state.test {
            // Offline-first emits Loading, then the (empty) cached success, before the remote
            // failure resolves to Error. With synchronous fakes and no real suspension between
            // these stages, StateFlow conflation may deliver only a subset of them to the
            // collector, so skip past whichever transient states do arrive.
            var state = awaitItem()
            while (state is UiState.Loading || (state is UiState.Success && state.data.books.isEmpty())) {
                state = awaitItem()
            }
            assertTrue(state is UiState.Error)
            assertTrue((state as UiState.Error).message is UiText.StringResource)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleViewMode switches from LIST to GRID`() = runTest {
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
        fakeDao.seed(listOf(makeEntity("1", "Kotlin Guide"), makeEntity("2", "Java Basics")))

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
        fakeDao.seed(
            listOf(
                makeEntity("1", "Book A", author = "Martin Fowler"),
                makeEntity("2", "Book B", author = "Jane Doe")
            )
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
        fakeDao.seed(listOf(makeEntity("1", "Kotlin"), makeEntity("2", "Java")))

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
    fun `refresh keeps existing books visible while re-fetching`() = runTest {
        fakeDao.seed(listOf(makeEntity("1", "Alpha"), makeEntity("2", "Beta")))

        viewModel.state.test {
            val initial = skipLoadingIfPresent()
            assertTrue(initial is UiState.Success)
            assertFalse((initial as UiState.Success).data.isRefreshing)

            viewModel.onAction(BooksListAction.OnRefresh)

            // StateFlow conflation under UnconfinedTestDispatcher may or may not surface a
            // distinct isRefreshing=true emission before the refreshed data lands - tolerate
            // either, but if it does appear, the previous books must still be showing.
            var state = awaitItem()
            if (state is UiState.Success && state.data.isRefreshing) {
                assertEquals(initial.data.books, state.data.books)
                state = awaitItem()
            }
            assertTrue(state is UiState.Success)
            assertFalse((state as UiState.Success).data.isRefreshing)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `saveScrollPosition persists index in SavedStateHandle`() {
        val handle = SavedStateHandle()
        val vm = BooksListViewModel(loadBooksUseCase, handle)
        vm.onAction(BooksListAction.OnSaveScrollPosition(7))
        assertEquals(7, handle.get<Int>("scroll_index"))
    }

    @Test
    fun `savedScrollIndex is restored from SavedStateHandle on creation`() = runTest {
        val handle = SavedStateHandle(mapOf("scroll_index" to 5))
        val vm = BooksListViewModel(loadBooksUseCase, handle)

        vm.state.test {
            val state = skipLoadingIfPresent()
            assertEquals(5, (state as UiState.Success).data.savedScrollIndex)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun makeEntity(id: String, title: String, author: String = "Author") = BookEntity(
        id = id, title = title, author = author, coverUrl = "",
        publishedYear = 2020, rating = 4.0, description = "", genres = "", createdAt = 0L
    )
}
