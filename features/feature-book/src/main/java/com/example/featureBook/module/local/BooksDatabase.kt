package com.example.featureBook.module.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.featureBook.model.local.BookEntity

@Database(entities = [BookEntity::class], version = 1, exportSchema = false)
abstract class BooksDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
}
