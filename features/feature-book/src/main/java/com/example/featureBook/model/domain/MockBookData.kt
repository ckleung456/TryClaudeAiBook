package com.example.featureBook.model.domain

object MockBookData {
    // Mock JSON data
    val booksJson = """
{
  "books": [
    {
      "id": "book_001",
      "title": "The Kotlin Journey",
      "author": "Jane Doe",
      "coverUrl": "https://example.com/covers/kotlin-journey.jpg",
      "publishedYear": 2021,
      "rating": 4.8,
      "description": "A comprehensive guide to mastering Kotlin programming language",
      "genres": ["Programming", "Technology", "Education"],
      "createdAt": 43200000000000
    },
    {
      "id": "book_002",
      "title": "Clean Architecture",
      "author": "Robert Martin",
      "coverUrl": "https://example.com/covers/clean-architecture.jpg",
      "publishedYear": 2017,
      "rating": 4.9,
      "description": "A practical guide to software architecture and design",
      "genres": ["Software Architecture", "Programming", "Design Patterns"],
      "createdAt": 54000000000000
    },
    {
      "id": "book_003",
      "title": "The Pragmatic Programmer",
      "author": "David Thomas",
      "coverUrl": "https://example.com/covers/pragmatic-programmer.jpg",
      "publishedYear": 2019,
      "rating": 4.7,
      "description": "Your journey to mastery in software development",
      "genres": ["Programming", "Best Practices", "Career"],
      "createdAt": 64800000000000
    },
    {
      "id": "book_004",
      "title": "Designing Data-Intensive Applications",
      "author": "Martin Kleppmann",
      "coverUrl": "https://example.com/covers/data-intensive-apps.jpg",
      "publishedYear": 2017,
      "rating": 4.9,
      "description": "The big ideas behind reliable, scalable, and maintainable systems",
      "genres": ["Database", "System Design", "Distributed Systems"],
      "createdAt": 75600000000000
    },
    {
      "id": "book_005",
      "title": "Effective Kotlin",
      "author": "Marcin Moskała",
      "coverUrl": "https://example.com/covers/effective-kotlin.jpg",
      "publishedYear": 2020,
      "rating": 4.6,
      "description": "Best practices for writing better Kotlin code",
      "genres": ["Programming", "Kotlin", "Best Practices"],
      "createdAt": 86400000000000
    }
  ],
  "total": 5,
  "page": 1,
  "limit": 10,
  "totalPages": 1,
  "hasNext": false
}
""".trimIndent()
}
