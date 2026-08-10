package com.example.tryclaudeaibook.di

import android.content.Context
import com.example.tryclaudeaibook.module.local.AppDataBase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModules {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDataBase =
        AppDataBase.getDatabase(context)

    @Provides
    @Singleton
    fun provideBookDao(appDatabase: AppDataBase) =
        appDatabase.bookDao()
}
