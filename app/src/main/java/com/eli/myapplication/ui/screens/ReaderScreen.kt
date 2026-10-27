package com.eli.myapplication.ui.screens

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eli.myapplication.data.Book
import com.eli.myapplication.data.Note
import com.eli.myapplication.ui.theme.*
import com.eli.myapplication.util.PdfSummaryExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    book: Book,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var currentPage by remember(book.id) { mutableIntStateOf(book.currentPage.coerceAtLeast(0)) }
    var totalPages by remember(book.id) { mutableIntStateOf(book.totalPages.coerceAtLeast(1)) }

    var pdfPageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var textPageContent by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Jump to page dialog state
    var showJumpToPageDialog by remember { mutableStateOf(false) }
    var jumpPageInput by remember { mutableStateOf("") }

    // Concurrent Split Reading & Note Taking Mode
    var isConcurrentNoteMode by remember { mutableStateOf(false) }
    var showFullNotesSheet by remember { mutableStateOf(false) }

    val bookNotes by viewModel.getNotesForBook(book.id).collectAsState(initial = emptyList())

    val startTime = remember { System.currentTimeMillis() }

    // Session tracking on exit
    DisposableEffect(book.id) {
        onDispose {
            val durationSeconds = (System.currentTimeMillis() - startTime) / 1000
            if (durationSeconds > 2) {
                val pagesRead = (currentPage - book.currentPage).coerceAtLeast(1)
                viewModel.recordSession(book.id, durationSeconds, pagesRead)
            }
            viewModel.updateBookProgress(book, currentPage, totalPages)
        }
    }

    // Save reading progress instantly on page change
    LaunchedEffect(currentPage) {
        if (currentPage >= 0) {
            viewModel.updateBookProgress(book, currentPage, totalPages)
        }
    }

    // Load page content
    LaunchedEffect(book.fileUri, currentPage) {
        isLoading = true
        errorMessage = null
        pdfPageBitmap = null
        textPageContent = null

        val fileUriString = book.fileUri
        if (fileUriString.isBlank()) {
            textPageContent = "Sample Reading Content for \"${book.title}\"\n\nPage ${currentPage + 1} of $totalPages\n\nThis is a sample text preview for manual library entries. Import a PDF or TXT file using the '+' or 'Import Book' option in EH library to read full documents!"
            isLoading = false
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(fileUriString)
                val pfd: ParcelFileDescriptor? = context.contentResolver.openFileDescriptor(uri, "r")

                if (pfd != null) {
                    var isPdf = false
                    try {
                        val renderer = PdfRenderer(pfd)
                        totalPages = renderer.pageCount.coerceAtLeast(1)
                        val validPage = currentPage.coerceIn(0, totalPages - 1)
                        val page = renderer.openPage(validPage)

                        // High resolution bitmap rendering
                        val width = (page.width * 2).coerceAtLeast(100)
                        val height = (page.height * 2).coerceAtLeast(100)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                        pdfPageBitmap = bitmap
                        page.close()
                        renderer.close()
                        isPdf = true
                    } catch (_: Exception) {
                        isPdf = false
                    } finally {
                        pfd.close()
                    }

                    if (!isPdf) {
                        // Attempt to read as text file
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val fullText = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                        if (fullText.isNotBlank()) {
                            val lines = fullText.lines()
                            val linesPerPage = 35
                            totalPages = (lines.size / linesPerPage).coerceAtLeast(1)
                            val validPage = currentPage.coerceIn(0, totalPages - 1)
                            val startLine = validPage * linesPerPage
                            val endLine = (startLine + linesPerPage).coerceAtMost(lines.size)
                            textPageContent = if (startLine < lines.size) {
                                lines.subList(startLine, endLine).joinToString("\n")
                            } else {
                                fullText
                            }
                        } else {
                            errorMessage = "Unable to read file content."
                        }
                    }
                } else {
                    errorMessage = "Could not open file descriptor for URI."
                }
            } catch (e: Exception) {
                errorMessage = "Error reading file: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier.clickable {
                            jumpPageInput = (currentPage + 1).toString()
                            showJumpToPageDialog = true
                        }
                    ) {
                        Text(
                            text = book.title,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            fontSize = 17.sp,
                            maxLines = 1
                        )
                        Text(
                            text = "Page ${currentPage + 1} of $totalPages (Tap to Jump)",
                            color = GoldAccent,
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Library",
                            tint = GoldAccent
                        )
                    }
                },
                actions = {
                    // Export PDF Summary Button
                    IconButton(
                        onClick = {
                            val uri = PdfSummaryExporter.exportBookSummaryPdf(context, book, bookNotes)
                            if (uri != null) {
                                Toast.makeText(context, "Exported PDF Summary to Downloads!", Toast.LENGTH_SHORT).show()
                                PdfSummaryExporter.openOrSharePdf(context, uri)
                            } else {
                                Toast.makeText(context, "Failed to create PDF summary.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Export Summary PDF",
                            tint = GoldAccent
                        )
                    }

                    // Concurrent Split Reading & Note Taking Mode Toggle
                    IconButton(
                        onClick = { isConcurrentNoteMode = !isConcurrentNoteMode }
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerticalSplit,
                            contentDescription = "Toggle Split Read & Write Mode",
                            tint = if (isConcurrentNoteMode) ForestPrimary else GoldAccent
                        )
                    }

                    // Notes Sheet Launcher Button
                    BadgedBox(
                        badge = {
                            if (bookNotes.isNotEmpty()) {
                                Badge(
                                    containerColor = ForestPrimary,
                                    contentColor = TextWhite
                                ) {
                                    Text(bookNotes.size.toString())
                                }
                            }
                        }
                    ) {
                        IconButton(onClick = { showFullNotesSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = "Notes & Annotations",
                                tint = GoldAccent
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkGreenSurface)
            )
        },
        containerColor = DarkGreenBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // PROMINENT TOP NAVIGATION BAR (Always visible right at the top, never covered by keyboard)
            Surface(
                color = DarkGreenSurfaceVariant,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (currentPage > 0) currentPage--
                        },
                        enabled = currentPage > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkGreenSurface,
                            contentColor = GoldAccent,
                            disabledContainerColor = DarkGreenSurface.copy(alpha = 0.4f),
                            disabledContentColor = TextMuted
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Prev", fontSize = 13.sp)
                    }

                    Surface(
                        color = ForestPrimary.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable {
                            jumpPageInput = (currentPage + 1).toString()
                            showJumpToPageDialog = true
                        }
                    ) {
                        Text(
                            text = "Page ${currentPage + 1} / $totalPages",
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    Button(
                        onClick = {
                            if (currentPage < totalPages - 1) currentPage++
                        },
                        enabled = currentPage < totalPages - 1,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkGreenSurface,
                            contentColor = GoldAccent,
                            disabledContainerColor = DarkGreenSurface.copy(alpha = 0.4f),
                            disabledContentColor = TextMuted
                        )
                    ) {
                        Text("Next", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Top Section: Book Reader View (Takes 100% when full screen, 45% when split mode is active)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (isConcurrentNoteMode) 0.45f else 1f)
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(color = ForestPrimary)
                    }
                    errorMessage != null -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = errorMessage ?: "Error loading document.",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 16.sp
                            )
                        }
                    }
                    pdfPageBitmap != null -> {
                        val bitmap = pdfPageBitmap!!
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "PDF Page ${currentPage + 1}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                        )
                    }
                    textPageContent != null -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkGreenSurface.copy(alpha = 0.9f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = textPageContent!!,
                                    color = TextWhite,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Section: Concurrent Note Taking Panel (Scrollable so typed text is always visible above keyboard)
            AnimatedVisibility(
                visible = isConcurrentNoteMode,
                enter = expandVertically(),
                exit = shrinkVertically(),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.55f)
            ) {
                ConcurrentSplitNotePanel(
                    currentPageNumber = currentPage + 1,
                    notesOnThisPage = bookNotes.filter { it.pageNumber == currentPage + 1 },
                    onAddNote = { title, content ->
                        viewModel.addNote(book.id, currentPage + 1, title, content)
                    },
                    onDeleteNote = { note ->
                        viewModel.deleteNote(note)
                    },
                    onClose = { isConcurrentNoteMode = false }
                )
            }
        }

        // Direct Jump-to-Page Dialog
        if (showJumpToPageDialog) {
            AlertDialog(
                onDismissRequest = { showJumpToPageDialog = false },
                title = { Text("Jump to Page Number") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Enter a page number between 1 and $totalPages:",
                            fontSize = 14.sp,
                            color = TextMuted
                        )
                        OutlinedTextField(
                            value = jumpPageInput,
                            onValueChange = { jumpPageInput = it },
                            placeholder = { Text("e.g. 42") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val targetPage = jumpPageInput.toIntOrNull()
                            if (targetPage != null) {
                                currentPage = (targetPage - 1).coerceIn(0, totalPages - 1)
                            }
                            showJumpToPageDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestPrimary)
                    ) {
                        Text("Jump")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showJumpToPageDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showFullNotesSheet) {
            NotesBottomSheet(
                bookTitle = book.title,
                currentPageNumber = currentPage + 1,
                notes = bookNotes,
                onAddNote = { title, content ->
                    viewModel.addNote(book.id, currentPage + 1, title, content)
                },
                onDeleteNote = { note ->
                    viewModel.deleteNote(note)
                },
                onExportPdf = {
                    val uri = PdfSummaryExporter.exportBookSummaryPdf(context, book, bookNotes)
                    if (uri != null) {
                        Toast.makeText(context, "Exported PDF Summary to Downloads!", Toast.LENGTH_SHORT).show()
                        PdfSummaryExporter.openOrSharePdf(context, uri)
                    }
                },
                onSelectPage = { pageNum ->
                    currentPage = (pageNum - 1).coerceIn(0, totalPages - 1)
                    showFullNotesSheet = false
                },
                onDismiss = { showFullNotesSheet = false }
            )
        }
    }
}

@Composable
fun ConcurrentSplitNotePanel(
    currentPageNumber: Int,
    notesOnThisPage: List<Note>,
    onAddNote: (String, String) -> Unit,
    onDeleteNote: (Note) -> Unit,
    onClose: () -> Unit
) {
    var noteTitle by remember { mutableStateOf("") }
    var noteContent by remember { mutableStateOf("") }

    Surface(
        color = DarkGreenSurface,
        tonalElevation = 12.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "📝 Split View: Notes (Page $currentPageNumber)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = GoldAccent
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Split View",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Note Input Form
            OutlinedTextField(
                value = noteTitle,
                onValueChange = { noteTitle = it },
                placeholder = { Text("Note Title...", color = TextMuted, fontSize = 13.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ForestPrimary,
                    unfocusedBorderColor = DarkGreenSurfaceVariant,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = noteContent,
                onValueChange = { noteContent = it },
                placeholder = { Text("Write notes while reading...", color = TextMuted, fontSize = 13.sp) },
                minLines = 3,
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ForestPrimary,
                    unfocusedBorderColor = DarkGreenSurfaceVariant,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (noteContent.isNotBlank() || noteTitle.isNotBlank()) {
                        onAddNote(
                            noteTitle.ifBlank { "Note on Page $currentPageNumber" },
                            noteContent
                        )
                        noteTitle = ""
                        noteContent = ""
                    }
                },
                enabled = noteContent.isNotBlank() || noteTitle.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ForestPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Note", fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Saved Page Notes List Section
            Text(
                text = "Page Notes (${notesOnThisPage.size})",
                fontSize = 13.sp,
                color = TextMuted,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (notesOnThisPage.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No notes on this page yet.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    notesOnThisPage.forEach { note ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkGreenSurfaceVariant),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    if (note.title.isNotBlank()) {
                                        Text(
                                            text = note.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = TextWhite
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                    }
                                    if (note.content.isNotBlank()) {
                                        Text(
                                            text = note.content,
                                            fontSize = 11.sp,
                                            color = TextWhite.copy(alpha = 0.9f)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onDeleteNote(note) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesBottomSheet(
    bookTitle: String,
    currentPageNumber: Int,
    notes: List<Note>,
    onAddNote: (String, String) -> Unit,
    onDeleteNote: (Note) -> Unit,
    onExportPdf: () -> Unit,
    onSelectPage: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var noteTitle by remember { mutableStateOf("") }
    var noteContent by remember { mutableStateOf("") }
    var isAddingNote by remember { mutableStateOf(false) }

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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Notes & Annotations",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = GoldAccent
                    )
                    Text(
                        text = bookTitle,
                        fontSize = 13.sp,
                        color = TextMuted,
                        maxLines = 1
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onExportPdf) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = "Export PDF Summary",
                            tint = GoldAccent
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onExportPdf,
                colors = ButtonDefaults.buttonColors(containerColor = DarkGreenSurfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = GoldAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export & Save PDF Summary Book", color = GoldAccent)
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (!isAddingNote) {
                Button(
                    onClick = { isAddingNote = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Note for Page $currentPageNumber")
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkGreenBackground),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "New Note (Page $currentPageNumber)",
                            fontWeight = FontWeight.SemiBold,
                            color = ForestPrimary,
                            fontSize = 15.sp
                        )

                        OutlinedTextField(
                            value = noteTitle,
                            onValueChange = { noteTitle = it },
                            placeholder = { Text("Note Title (Optional)", color = TextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ForestPrimary,
                                unfocusedBorderColor = DarkGreenSurfaceVariant,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = noteContent,
                            onValueChange = { noteContent = it },
                            placeholder = { Text("Write your thoughts, summary, or quotes...", color = TextMuted) },
                            minLines = 3,
                            maxLines = 6,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ForestPrimary,
                                unfocusedBorderColor = DarkGreenSurfaceVariant,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = {
                                isAddingNote = false
                                noteTitle = ""
                                noteContent = ""
                            }) {
                                Text("Cancel", color = TextMuted)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    if (noteContent.isNotBlank() || noteTitle.isNotBlank()) {
                                        onAddNote(
                                            noteTitle.ifBlank { "Note on Page $currentPageNumber" },
                                            noteContent
                                        )
                                        noteTitle = ""
                                        noteContent = ""
                                        isAddingNote = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ForestPrimary),
                                enabled = noteContent.isNotBlank() || noteTitle.isNotBlank()
                            ) {
                                Text("Save Note")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Saved Notes (${notes.size})",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = TextWhite
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No notes written for this book yet.\nTap 'Add Note' above to write down key takeaways!",
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
                    items(notes, key = { it.id }) { note ->
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
                                    Surface(
                                        color = ForestPrimary.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.clickable { onSelectPage(note.pageNumber) }
                                    ) {
                                        Text(
                                            text = "Page ${note.pageNumber}",
                                            color = GoldAccent,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDeleteNote(note) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete Note",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                if (note.title.isNotBlank()) {
                                    Text(
                                        text = note.title,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }

                                if (note.content.isNotBlank()) {
                                    Text(
                                        text = note.content,
                                        color = TextWhite.copy(alpha = 0.9f),
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp
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
        }
    }
}
