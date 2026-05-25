package com.example.featureBook.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.featureBook.ui.detail.BookDetailRoot
import com.example.featureBook.ui.list.BooksListRoot

fun NavGraphBuilder.booksGraph(navController: NavController) {
    composable<BooksListRoute> {
        BooksListRoot(
            onNavigateToDetail = { bookId -> navController.navigate(BookDetailRoute(bookId)) }
        )
    }
    composable<BookDetailRoute> {
        BookDetailRoot(
            onBack = { navController.popBackStack() }
        )
    }
}
