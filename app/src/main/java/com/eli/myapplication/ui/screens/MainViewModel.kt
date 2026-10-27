package com.eli.myapplication.ui.screens

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eli.myapplication.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).appDao()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val books: StateFlow<List<Book>> = dao.getAllBooks()
        .combine(_searchQuery) { bookList, query ->
            if (query.isBlank()) {
                bookList
            } else {
                bookList.filter {
                    it.title.contains(query, ignoreCase = true) ||
                            it.author.contains(query, ignoreCase = true)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val sessions: StateFlow<List<ReadingSession>> = dao.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val plans: StateFlow<List<ReadingPlan>> = dao.getAllPlans()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allNotes: StateFlow<List<Note>> = dao.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun getNotesForBook(bookId: Long): Flow<List<Note>> {
        return dao.getNotesForBook(bookId)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addBook(title: String, author: String, totalPages: Int, fileUri: String = "") {
        viewModelScope.launch {
            dao.insertBook(
                Book(
                    title = title,
                    author = author,
                    totalPages = totalPages,
                    fileUri = fileUri
                )
            )
        }
    }

    fun importBookFromUri(context: Context, uri: Uri, onImported: (Book) -> Unit = {}) {
        viewModelScope.launch {
            try {
                if (uri.scheme == "content") {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (_: Exception) {
                    }
                }

                val currentBooks = books.value
                val existing = currentBooks.find { it.fileUri == uri.toString() }
                if (existing != null) {
                    onImported(existing)
                    return@launch
                }

                var title = ""

                if (uri.scheme == "content") {
                    context.contentResolver.query(
                        uri,
                        arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                        null,
                        null,
                        null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (nameIndex != -1) {
                                title = cursor.getString(nameIndex) ?: ""
                            }
                        }
                    }
                }

                if (title.isBlank()) {
                    title = uri.lastPathSegment ?: "Imported Book"
                }

                var totalPages = 1
                withContext(Dispatchers.IO) {
                    try {
                        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                        if (pfd != null) {
                            try {
                                val pdfRenderer = PdfRenderer(pfd)
                                totalPages = pdfRenderer.pageCount.coerceAtLeast(1)
                                pdfRenderer.close()
                            } catch (_: Exception) {
                                val inputStream = context.contentResolver.openInputStream(uri)
                                val lineCount = inputStream?.bufferedReader()?.useLines { it.count() } ?: 0
                                totalPages = (lineCount / 40).coerceAtLeast(1)
                            } finally {
                                pfd.close()
                            }
                        }
                    } catch (_: Exception) {
                        totalPages = 100
                    }
                }

                val newBook = Book(
                    title = title,
                    author = "Local Document",
                    totalPages = totalPages,
                    currentPage = 0,
                    fileUri = uri.toString()
                )

                val id = dao.insertBook(newBook)
                val insertedBook = newBook.copy(id = id)
                onImported(insertedBook)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateBookProgress(book: Book, currentPage: Int, totalPages: Int) {
        viewModelScope.launch {
            val isCompleted = currentPage >= totalPages && totalPages > 0
            dao.updateBook(
                book.copy(
                    currentPage = currentPage,
                    totalPages = totalPages.coerceAtLeast(1),
                    isCompleted = isCompleted
                )
            )
        }
    }

    fun deleteBook(book: Book) {
        viewModelScope.launch {
            dao.deleteBook(book)
        }
    }

    fun addNote(bookId: Long, pageNumber: Int, title: String, content: String) {
        viewModelScope.launch {
            dao.insertNote(Note(bookId = bookId, pageNumber = pageNumber, title = title, content = content))
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            dao.deleteNote(note)
        }
    }

    fun addPlan(bookId: Long, title: String, targetDate: Long, targetPages: Int) {
        viewModelScope.launch {
            dao.insertPlan(ReadingPlan(bookId = bookId, title = title, targetDate = targetDate, targetPages = targetPages))
        }
    }

    fun deletePlan(plan: ReadingPlan) {
        viewModelScope.launch {
            dao.deletePlan(plan)
        }
    }

    fun recordSession(bookId: Long, durationSeconds: Long, pagesRead: Int) {
        viewModelScope.launch {
            dao.insertSession(ReadingSession(bookId = bookId, durationSeconds = durationSeconds, pagesRead = pagesRead))
        }
    }
}
