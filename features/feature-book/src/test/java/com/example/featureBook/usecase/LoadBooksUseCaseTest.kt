package com.example.featureBook.usecase

import app.cash.turbine.test
import com.example.featureBook.fake.FakeBookDao
import com.example.featureBook.fake.FakeBooksRemoteRepository
import com.example.featureBook.model.domain.SortOrder
import com.example.featureBook.model.local.BookEntity
import com.example.featureBook.model.network.Book
import com.example.featureBook.module.local.BooksCacheRepository
import com.example.core.presentation.UseCaseOutputWithStatus
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

        useCase.invoke(SortOrder.ASCENDING).test {
            assertTrue(awaitItem() is UseCaseOutputWithStatus.Progress)
            val cached = awaitItem() as UseCaseOutputWithStatus.Success
            assertEquals(emptyList<Any>(), cached.result)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits cached books immediately when cache has data`() = runTest {
        fakeDao.seed(listOf(makeEntity("1", "Alpha"), makeEntity("2", "Beta")))

        useCase.invoke(SortOrder.ASCENDING).test {
            awaitItem() // Progress
            val cached = awaitItem() as UseCaseOutputWithStatus.Success
            assertEquals(2, cached.result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `books are sorted ascending by first title character`() = runTest {
        fakeRemote.books = listOf(makeBook("1", "Zeta"), makeBook("2", "Alpha"))

        useCase.invoke(SortOrder.ASCENDING).test {
            awaitItem() // Progress
            awaitItem() // empty cache emission
            val refreshed = awaitItem() as UseCaseOutputWithStatus.Success
            assertEquals("Alpha", refreshed.result.first().title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `books are sorted descending by first title character`() = runTest {
        fakeRemote.books = listOf(makeBook("1", "Alpha"), makeBook("2", "Zeta"))

        useCase.invoke(SortOrder.DESCENDING).test {
            awaitItem() // Progress
            awaitItem() // empty cache emission
            val refreshed = awaitItem() as UseCaseOutputWithStatus.Success
            assertEquals("Zeta", refreshed.result.first().title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits failure when remote throws`() = runTest {
        fakeRemote.shouldThrow = true

        useCase.invoke(SortOrder.ASCENDING).test {
            awaitItem() // Progress
            awaitItem() // empty list from cache
            assertTrue(awaitItem() is UseCaseOutputWithStatus.Failed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `remote books are saved to cache after fetch`() = runTest {
        fakeRemote.books = listOf(makeBook("1", "Alpha"))

        useCase.invoke(SortOrder.ASCENDING).test {
            awaitItem() // Progress
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