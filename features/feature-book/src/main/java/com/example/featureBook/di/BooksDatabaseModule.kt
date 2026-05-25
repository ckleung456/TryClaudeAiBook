package com.example.featureBook.di

import android.content.Context
import androidx.room.Room
import com.example.featureBook.module.local.BookDao
import com.example.featureBook.module.local.BooksDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BooksDatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BooksDatabase =
        Room.databaseBuilder(context, BooksDatabase::class.java, "books.db").build()

    @Provides
    fun provideBookDao(database: BooksDatabase): BookDao = database.bookDao()
}
