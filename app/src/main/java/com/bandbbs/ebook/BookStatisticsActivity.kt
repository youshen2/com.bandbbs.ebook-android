package com.bandbbs.ebook

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bandbbs.ebook.ui.screens.BookStatisticsScreen
import com.bandbbs.ebook.ui.theme.EbookTheme
import com.bandbbs.ebook.ui.viewmodel.MainViewModel

class BookStatisticsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val bookName = intent.getStringExtra(EXTRA_BOOK_NAME) ?: return finish()
        
        setContent {
            val viewModel: MainViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                MainViewModel.ThemeMode.LIGHT -> false
                MainViewModel.ThemeMode.DARK -> true
                MainViewModel.ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            EbookTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BookStatisticsScreen(
                        bookName = bookName,
                        onBackClick = { finish() }
                    )
                }
            }
        }
    }
    
    companion object {
        private const val EXTRA_BOOK_NAME = "extra_book_name"
        
        fun start(context: Context, bookName: String) {
            val intent = Intent(context, BookStatisticsActivity::class.java).apply {
                putExtra(EXTRA_BOOK_NAME, bookName)
            }
            context.startActivity(intent)
        }
    }
}

