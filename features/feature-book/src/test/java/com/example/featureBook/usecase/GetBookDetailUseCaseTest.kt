package com.example.featureBook.usecase

import com.example.featureBook.fake.FakeBookDao
import com.example.featureBook.fake.FakeBooksRemoteRepository
import com.example.featureBook.model.local.BookEntity
import com.example.featureBook.model.network.Book
import com.example.featureBook.module.local.BooksCacheRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetBookDetailUseCaseTest {

    private lateinit var fakeRemote: FakeBooksRemoteRepository
    private lateinit var fakeDao: FakeBookDao
    private lateinit var cacheRepo: BooksCacheRepository
    private lateinit var useCase: GetBookDetailUseCase

    @Before
    fun setUp() {
        fakeRemote = FakeBooksRemoteRepository()
        fakeDao = FakeBookDao()
        cacheRepo = BooksCacheRepository(fakeDao)
        useCase = GetBookDetailUseCase(fakeRemote, cacheRepo)
    }

    @Test
    fun `returns cached book when present without calling remote`() = runTest {
        fakeDao.seed(listOf(makeEntity("book_1", "Cached Title")))
        fakeRemote.books = emptyList()

        val result = useCase("book_1")

        assertTrue(result.isSuccess)
        assertEquals("Cached Title", result.getOrNull()?.title)
    }

    @Test
    fun `fetches from remote when not in cache and saves to cache`() = runTest {
        fakeRemote.books = listOf(makeBook("book_2", "Remote Title"))

        val result = useCase("book_2")

        assertTrue(result.isSuccess)
        assertEquals("Remote Title", result.getOrNull()?.title)
        assertTrue(fakeDao.getBookById("book_2") != null)
    }

    @Test
    fun `returns error when book not found in cache or remote`() = runTest {
        fakeRemote.books = emptyList()

        val result = useCase("nonexistent_id")

        assertTrue(result.isFailure)
    }

    @Test
    fun `returns error when remote throws`() = runTest {
        fakeRemote.shouldThrow = true

        val result = useCase("book_1")

        assertTrue(result.isFailure)
    }

    @Test
    fun `prefers cached book over remote`() = runTest {
        fakeDao.seed(listOf(makeEntity("book_1", "Cached Title")))
        fakeRemote.books = listOf(makeBook("book_1", "Remote Title"))

        val result = useCase("book_1")

        assertEquals("Cached Title", result.getOrNull()?.title)
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
