package com.example.tryclaudeaibook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.example.featureBook.navigation.BooksListRoute
import com.example.featureBook.navigation.booksGraph
import com.example.tryclaudeaibook.ui.theme.TryClaudeAiBookTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TryClaudeAiBookTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = BooksListRoute
                ) {
                    booksGraph(navController)
                }
            }
        }
    }
}
