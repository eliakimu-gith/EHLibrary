package com.eli.myapplication.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eli.myapplication.data.Book
import com.eli.myapplication.data.Note
import com.eli.myapplication.data.ReadingPlan
import com.eli.myapplication.data.ReadingSession
import com.eli.myapplication.ui.theme.*
import com.eli.myapplication.util.PdfSummaryExporter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDashboardScreen(
    viewModel: MainViewModel,
    onOpenReader: (Book) -> Unit,
    onOpenPlanner: () -> Unit = {}
) {
    val context = LocalContext.current
    val books by viewModel.books.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val allNotes by viewModel.allNotes.collectAsState()
    val plans by viewModel.plans.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showPlannerSheet by remember { mutableStateOf(false) }
    var showHabitsSheet by remember { mutableStateOf(false) }
    var bookToDelete by remember { mutableStateOf<Book?>(null) }

    var titleInput by remember { mutableStateOf("") }
    var authorInput by remember { mutableStateOf("") }
    var pagesInput by remember { mutableStateOf("") }

    val totalReadingMinutes = sessions.sumOf { it.durationSeconds } / 60
    val totalBooksRead = books.count { it.isCompleted }

    // File picker launcher for importing books (PDF, EPUB, TXT, etc.)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importBookFromUri(context, it) { importedBook ->
                onOpenReader(importedBook)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search title or author...", color = TextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ForestPrimary,
                                unfocusedBorderColor = DarkGreenSurfaceVariant,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            modifier = Modifier.fillMaxWidth(0.9f)
                        )
                    } else {
                        Text("EH library", fontWeight = FontWeight.Bold, color = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkGreenSurface),
                actions = {
                    // Reading Habits & Analytics Icon
                    IconButton(onClick = { showHabitsSheet = true }) {
                        Icon(Icons.Default.Insights, contentDescription = "Reading Habits & Analytics", tint = GoldAccent)
                    }

                    IconButton(onClick = {
                        isSearchActive = !isSearchActive
                        if (!isSearchActive) viewModel.setSearchQuery("")
                    }) {
                        Icon(
                            if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search Books",
                            tint = GoldAccent
                        )
                    }

                    IconButton(onClick = {
                        filePickerLauncher.launch(
                            arrayOf(
                                "application/pdf",
                                "text/plain",
                                "application/epub+zip",
                                "application/octet-stream",
                                "*/*"
                            )
                        )
                    }) {
                        Icon(
                            Icons.Default.FileOpen,
                            contentDescription = "Import Book File",
                            tint = GoldAccent
                        )
                    }

                    BadgedBox(
                        badge = {
                            if (allNotes.isNotEmpty()) {
                                Badge(
                                    containerColor = ForestPrimary,
                                    contentColor = TextWhite
                                ) {
                                    Text(allNotes.size.toString())
                                }
                            }
                        }
                    ) {
                        IconButton(onClick = { showPlannerSheet = true }) {
                            Icon(Icons.Default.EditNote, contentDescription = "Notes & Planner", tint = GoldAccent)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = ForestPrimary,
                contentColor = TextWhite
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Book")
            }
        },
        containerColor = DarkGreenBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Glassmorphic Statistics Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(16),
                colors = CardDefaults.cardColors(containerColor = DarkGreenSurface.copy(alpha = 0.9f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatItem("Books", books.size.toString())
                    StatItem("Completed", totalBooksRead.toString())
                    StatItem("Read Time", "${totalReadingMinutes}m")
                    StatItem("Notes", allNotes.size.toString())
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (searchQuery.isBlank()) "Your Books" else "Search Results",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold
                )

                TextButton(onClick = {
                    filePickerLauncher.launch(
                        arrayOf(
                            "application/pdf",
                            "text/plain",
                            "application/epub+zip",
                            "application/octet-stream",
                            "*/*"
                        )
                    )
                }) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Import File", color = GoldAccent)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (books.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (searchQuery.isBlank()) "No books added yet." else "No books match \"$searchQuery\".",
                            color = TextMuted,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                filePickerLauncher.launch(
                                    arrayOf(
                                        "application/pdf",
                                        "text/plain",
                                        "application/epub+zip",
                                        "application/octet-stream",
                                        "*/*"
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ForestPrimary)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Browse Device Files")
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(books, key = { it.id }) { book ->
                        val notesForBook = allNotes.filter { it.bookId == book.id }
                        BookCard(
                            book = book,
                            notesCount = notesForBook.size,
                            onClick = { onOpenReader(book) },
                            onExportPdf = {
                                val uri = PdfSummaryExporter.exportBookSummaryPdf(context, book, notesForBook)
                                if (uri != null) {
                                    Toast.makeText(context, "Exported PDF Summary to Downloads!", Toast.LENGTH_SHORT).show()
                                    PdfSummaryExporter.openOrSharePdf(context, uri)
                                } else {
                                    Toast.makeText(context, "Failed to create PDF summary.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDelete = { bookToDelete = book }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add Manual Book") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = titleInput,
                            onValueChange = { titleInput = it },
                            label = { Text("Book Title") }
                        )
                        OutlinedTextField(
                            value = authorInput,
                            onValueChange = { authorInput = it },
                            label = { Text("Author") }
                        )
                        OutlinedTextField(
                            value = pagesInput,
                            onValueChange = { pagesInput = it },
                            label = { Text("Total Pages") }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val pages = pagesInput.toIntOrNull() ?: 100
                        if (titleInput.isNotBlank()) {
                            viewModel.addBook(titleInput, authorInput, pages)
                            titleInput = ""
                            authorInput = ""
                            pagesInput = ""
                            showAddDialog = false
                        }
                    }) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (bookToDelete != null) {
            AlertDialog(
                onDismissRequest = { bookToDelete = null },
                title = { Text("Delete Book") },
                text = { Text("Are you sure you want to remove \"${bookToDelete?.title}\" from your library?") },
                confirmButton = {
                    TextButton(onClick = {
                        bookToDelete?.let { viewModel.deleteBook(it) }
                        bookToDelete = null
                    }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { bookToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showPlannerSheet) {
            AllNotesAndPlannerSheet(
                allNotes = allNotes,
                books = books,
                plans = plans,
                onDeleteNote = { viewModel.deleteNote(it) },
                onAddPlan = { bookId, planTitle, pages ->
                    viewModel.addPlan(bookId, planTitle, System.currentTimeMillis() + 86400000 * 7, pages)
                },
                onDeletePlan = { viewModel.deletePlan(it) },
                onOpenBook = { book ->
                    showPlannerSheet = false
                    onOpenReader(book)
                },
                onDismiss = { showPlannerSheet = false }
            )
        }

        if (showHabitsSheet) {
            ReadingHabitsSheet(
                books = books,
                sessions = sessions,
                notesCount = allNotes.size,
                onDismiss = { showHabitsSheet = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingHabitsSheet(
    books: List<Book>,
    sessions: List<ReadingSession>,
    notesCount: Int,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val totalSeconds = sessions.sumOf { it.durationSeconds }
    val totalMinutes = totalSeconds / 60
    val totalHours = totalMinutes / 60
    val remMinutes = totalMinutes % 60
    val totalPagesRead = sessions.sumOf { it.pagesRead }
    val completedBooks = books.count { it.isCompleted }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkGreenSurface,
        contentColor = TextWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Insights, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Reading Habits & Analytics",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = GoldAccent
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Summary Statistics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HabitStatCard(
                    title = "Total Time",
                    value = if (totalHours > 0) "${totalHours}h ${remMinutes}m" else "${totalMinutes}m",
                    modifier = Modifier.weight(1f)
                )
                HabitStatCard(
                    title = "Pages Read",
                    value = totalPagesRead.toString(),
                    modifier = Modifier.weight(1f)
                )
                HabitStatCard(
                    title = "Sessions",
                    value = sessions.size.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HabitStatCard(
                    title = "Completed",
                    value = "$completedBooks / ${books.size}",
                    modifier = Modifier.weight(1f)
                )
                HabitStatCard(
                    title = "Total Notes",
                    value = notesCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                HabitStatCard(
                    title = "Avg Pace",
                    value = if (sessions.isNotEmpty()) "${(totalPagesRead / sessions.size).coerceAtLeast(1)} p/s" else "0",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Recent Reading Sessions (${sessions.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextWhite
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (sessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No reading sessions recorded yet.\nOpen a book and read for a couple of minutes to track your habits!",
                        color = TextMuted,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    items(sessions.sortedByDescending { it.timestamp }, key = { it.id }) { session ->
                        val sessionBook = books.find { it.id == session.bookId }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkGreenSurfaceVariant),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sessionBook?.title ?: "Book #${session.bookId}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = GoldAccent,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = dateFormat.format(Date(session.timestamp)),
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${session.durationSeconds / 60} mins",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = ForestPrimary
                                    )
                                    Text(
                                        text = "+${session.pagesRead} pages",
                                        fontSize = 11.sp,
                                        color = TextWhite
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HabitStatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkGreenSurfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = title, fontSize = 11.sp, color = TextMuted)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllNotesAndPlannerSheet(
    allNotes: List<Note>,
    books: List<Book>,
    plans: List<ReadingPlan>,
    onDeleteNote: (Note) -> Unit,
    onAddPlan: (Long, String, Int) -> Unit,
    onDeletePlan: (ReadingPlan) -> Unit,
    onOpenBook: (Book) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Saved Notes, 1 = Reading Plans

    var showAddPlanDialog by remember { mutableStateOf(false) }
    var newPlanTitle by remember { mutableStateOf("") }
    var newPlanPages by remember { mutableStateOf("20") }
    var selectedBookIdForPlan by remember { mutableLongStateOf(books.firstOrNull()?.id ?: 0L) }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkGreenSurface,
        contentColor = TextWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Notes & Reading Planner",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = GoldAccent
                )

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkGreenSurfaceVariant,
                contentColor = GoldAccent
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("All Saved Notes (${allNotes.size})", color = if (selectedTab == 0) GoldAccent else TextMuted) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Reading Plans (${plans.size})", color = if (selectedTab == 1) GoldAccent else TextMuted) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                // Saved Notes View
                if (allNotes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No notes written yet.\nOpen any book and tap the note icon (📝) at the top to write notes while reading!",
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                    ) {
                        items(allNotes, key = { it.id }) { note ->
                            val associatedBook = books.find { it.id == note.bookId }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkGreenSurfaceVariant),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = associatedBook?.title ?: "Book #${note.bookId}",
                                                fontWeight = FontWeight.Bold,
                                                color = GoldAccent,
                                                fontSize = 14.sp,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "Page ${note.pageNumber}",
                                                color = TextMuted,
                                                fontSize = 12.sp
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (associatedBook != null) {
                                                TextButton(onClick = { onOpenBook(associatedBook) }) {
                                                    Text("Read", color = ForestPrimary)
                                                }
                                            }
                                            IconButton(onClick = { onDeleteNote(note) }) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    if (note.title.isNotBlank()) {
                                        Text(
                                            text = note.title,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextWhite,
                                            fontSize = 15.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                    }

                                    if (note.content.isNotBlank()) {
                                        Text(
                                            text = note.content,
                                            color = TextWhite.copy(alpha = 0.9f),
                                            fontSize = 14.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = dateFormat.format(Date(note.timestamp)),
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Reading Plans View
                Column(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { showAddPlanDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Set Reading Goal / Plan")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (plans.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No active reading plans.\nCreate a reading goal to track your daily progress!",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 350.dp)
                        ) {
                            items(plans, key = { it.id }) { plan ->
                                val targetBook = books.find { it.id == plan.bookId }
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = DarkGreenSurfaceVariant),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = plan.title,
                                                fontWeight = FontWeight.Bold,
                                                color = TextWhite,
                                                fontSize = 15.sp
                                            )
                                            Text(
                                                text = "Book: ${targetBook?.title ?: "All Books"} • Target: ${plan.targetPages} pages/day",
                                                color = TextMuted,
                                                fontSize = 13.sp
                                            )
                                        }

                                        IconButton(onClick = { onDeletePlan(plan) }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete Plan",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddPlanDialog) {
            AlertDialog(
                onDismissRequest = { showAddPlanDialog = false },
                title = { Text("Set Reading Goal") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = newPlanTitle,
                            onValueChange = { newPlanTitle = it },
                            label = { Text("Goal Title (e.g. Daily 20 Pages)") }
                        )
                        OutlinedTextField(
                            value = newPlanPages,
                            onValueChange = { newPlanPages = it },
                            label = { Text("Target Pages per Day") }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val pages = newPlanPages.toIntOrNull() ?: 20
                        if (newPlanTitle.isNotBlank()) {
                            onAddPlan(selectedBookIdForPlan, newPlanTitle, pages)
                            newPlanTitle = ""
                            newPlanPages = "20"
                            showAddPlanDialog = false
                        }
                    }) {
                        Text("Save Goal")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddPlanDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
        Text(text = label, fontSize = 12.sp, color = TextMuted)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookCard(
    book: Book,
    notesCount: Int,
    onClick: () -> Unit,
    onExportPdf: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(12)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onDelete
            ),
        colors = CardDefaults.cardColors(containerColor = DarkGreenSurfaceVariant.copy(alpha = 0.8f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (book.fileUri.isNotBlank()) Icons.AutoMirrored.Filled.Article else Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = ForestPrimary,
                modifier = Modifier.size(38.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = book.title, fontWeight = FontWeight.Bold, color = TextWhite, fontSize = 15.sp, maxLines = 1)
                Text(
                    text = "Page ${book.currentPage + 1} of ${book.totalPages} • $notesCount notes",
                    color = TextMuted,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                val progressFloat = if (book.totalPages > 0) book.currentPage.toFloat() / book.totalPages else 0f
                LinearProgressIndicator(
                    progress = { progressFloat.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp),
                    color = ForestPrimary,
                    trackColor = DarkGreenBackground
                )
            }

            IconButton(onClick = onExportPdf) {
                Icon(
                    Icons.Default.PictureAsPdf,
                    contentDescription = "Export Summary PDF",
                    tint = GoldAccent,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = TextMuted.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
