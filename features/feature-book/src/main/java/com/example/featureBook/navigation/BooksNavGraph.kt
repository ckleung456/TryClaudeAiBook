package com.example.featureBook.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.featureBook.ui.detail.BookDetailRoute
import com.example.featureBook.ui.list.BooksListRoute

fun NavGraphBuilder.booksGraph(navController: NavController) {
    composable<BooksListRoute> {
        BooksListRoute(
            onBookClick = { bookId -> navController.navigate(BookDetailRoute(bookId)) }
        )
    }
    composable<BookDetailRoute> {
        BookDetailRoute(
            onBack = { navController.popBackStack() }
        )
    }
}
