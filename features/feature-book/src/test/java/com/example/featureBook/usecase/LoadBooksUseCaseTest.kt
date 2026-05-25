package com.example.featureBook.usecase

import app.cash.turbine.test
import com.example.featureBook.fake.FakeBookDao
import com.example.featureBook.fake.FakeBooksRemoteRepository
import com.example.featureBook.model.domain.BookUiModel
import com.example.featureBook.model.domain.SortOrder
import com.example.featureBook.model.local.BookEntity
import com.example.featureBook.model.network.Book
import com.example.featureBook.module.local.BooksCacheRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LoadBooksUseCaseTest {

    private lateinit var fakeRemote: FakeBooksRemoteRepository
    private lateinit var fakeDao: FakeBookDao
    private lateinit var cacheRepo: BooksCacheRepository
    private lateinit var useCase: LoadBooksUseCase

    @Before
    fun setUp() {
        fakeRemote = FakeBooksRemoteRepository()
        fakeDao = FakeBookDao()
        cacheRepo = BooksCacheRepository(fakeDao)
        useCase = LoadBooksUseCase(fakeRemote, cacheRepo)
    }

    @Test
    fun `emits empty list first when cache is empty`() = runTest {
        fakeRemote.books = listOf(makeBook("1", "Alpha"))

        useCase(SortOrder.ASCENDING).test {
            val first = awaitItem()
            assertTrue(first.isSuccess)
            assertEquals(emptyList<BookUiModel>(), first.getOrNull())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits cached books immediately when cache has data`() = runTest {
        fakeDao.seed(listOf(makeEntity("1", "Alpha"), makeEntity("2", "Beta")))

        useCase(SortOrder.ASCENDING).test {
            val first = awaitItem()
            assertTrue(first.isSuccess)
            assertEquals(2, first.getOrNull()?.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `books are sorted ascending by first title character`() = runTest {
        fakeRemote.books = listOf(makeBook("1", "Zeta"), makeBook("2", "Alpha"))

        useCase(SortOrder.ASCENDING).test {
            awaitItem() // empty cache emission
            val result = awaitItem() // after remote refresh
            val books = result.getOrNull()!!
            assertEquals("Alpha", books.first().title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `books are sorted descending by first title character`() = runTest {
        fakeRemote.books = listOf(makeBook("1", "Alpha"), makeBook("2", "Zeta"))

        useCase(SortOrder.DESCENDING).test {
            awaitItem() // empty cache emission
            val result = awaitItem() // after remote refresh
            val books = result.getOrNull()!!
            assertEquals("Zeta", books.first().title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits failure result when remote throws`() = runTest {
        fakeRemote.shouldThrow = true

        useCase(SortOrder.ASCENDING).test {
            val first = awaitItem() // empty list from cache
            assertTrue(first.isSuccess)
            val errorResult = awaitItem()
            assertTrue(errorResult.isFailure)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `remote books are saved to cache after fetch`() = runTest {
        fakeRemote.books = listOf(makeBook("1", "Alpha"))

        useCase(SortOrder.ASCENDING).test {
            awaitItem() // empty cache
            awaitItem() // after remote save
            cancelAndIgnoreRemainingEvents()
        }

        assertTrue(fakeDao.getBookById("1") != null)
    }

    private fun makeBook(id: String, title: String) = Book(
        id = id, title = title, author = "Author", coverUrl = "",
        publishedYear = 2020, rating = 4.0, description = null,
        genres = emptyList(), createdAt = 0L
    )

    private fun makeEntity(id: String, title: String) = BookEntity(
        id = id, title = title, author = "Author", coverUrl = "",
        publishedYear = 2020, rating = 4.0, description = null,
        genres = "", createdAt = 0L
    )
}
