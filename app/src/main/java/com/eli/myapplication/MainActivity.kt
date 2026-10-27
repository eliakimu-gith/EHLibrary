package com.eli.myapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import com.eli.myapplication.data.Book
import com.eli.myapplication.ui.screens.HomeDashboardScreen
import com.eli.myapplication.ui.screens.MainViewModel
import com.eli.myapplication.ui.screens.ReaderScreen
import com.eli.myapplication.ui.theme.VerdeReadTheme

import androidx.core.content.IntentCompat

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var pendingIntentUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            VerdeReadTheme {
                var selectedBookForReader by remember { mutableStateOf<Book?>(null) }

                LaunchedEffect(pendingIntentUri) {
                    val uri = pendingIntentUri
                    if (uri != null) {
                        viewModel.importBookFromUri(this@MainActivity, uri) { importedBook ->
                            selectedBookForReader = importedBook
                        }
                        pendingIntentUri = null
                    }
                }

                val activeBook = selectedBookForReader
                if (activeBook != null) {
                    ReaderScreen(
                        book = activeBook,
                        viewModel = viewModel,
                        onBack = { selectedBookForReader = null }
                    )
                } else {
                    HomeDashboardScreen(
                        viewModel = viewModel,
                        onOpenReader = { book -> selectedBookForReader = book },
                        onOpenPlanner = { }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        if (Intent.ACTION_VIEW == action || Intent.ACTION_SEND == action) {
            val uri: Uri? = intent.data ?: IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            if (uri != null) {
                pendingIntentUri = uri
            }
        }
    }
}
