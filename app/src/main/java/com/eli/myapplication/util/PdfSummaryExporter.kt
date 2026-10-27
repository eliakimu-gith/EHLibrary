package com.eli.myapplication.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import com.eli.myapplication.data.Book
import com.eli.myapplication.data.Note
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfSummaryExporter {

    fun exportBookSummaryPdf(
        context: Context,
        book: Book,
        notes: List<Note>
    ): Uri? {
        val pdfDocument = PdfDocument()
        val pageWidth = 595 // Standard A4 width in points
        val pageHeight = 842 // Standard A4 height in points

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val primaryGreen = Color.rgb(20, 80, 45)

        val titlePaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subtitlePaint = TextPaint().apply {
            color = Color.DKGRAY
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            isAntiAlias = true
        }

        val headerPaint = TextPaint().apply {
            color = primaryGreen
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val noteTitlePaint = TextPaint().apply {
            color = Color.rgb(30, 30, 30)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val bodyPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 11f
            isAntiAlias = true
        }

        val metaPaint = TextPaint().apply {
            color = Color.GRAY
            textSize = 10f
            isAntiAlias = true
        }

        var yPos = 40f
        val margin = 40f
        val contentWidth = pageWidth - (margin * 2)

        // Draw Top Header Banner
        val bannerPaint = Paint().apply { color = primaryGreen }
        canvas.drawRect(margin, yPos, pageWidth - margin, yPos + 6f, bannerPaint)
        yPos += 20f

        val titleText = "Book Summary & Notes Report: ${book.title}"
        val titleLayout = StaticLayout.Builder.obtain(titleText, 0, titleText.length, titlePaint, contentWidth.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()
        canvas.save()
        canvas.translate(margin, yPos)
        titleLayout.draw(canvas)
        canvas.restore()
        yPos += titleLayout.height + 10f

        val authorText = "Author: ${book.author.ifBlank { "Unknown Author" }}"
        val authorLayout = StaticLayout.Builder.obtain(authorText, 0, authorText.length, subtitlePaint, contentWidth.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()
        canvas.save()
        canvas.translate(margin, yPos)
        authorLayout.draw(canvas)
        canvas.restore()
        yPos += authorLayout.height + 12f

        val dateFormat = SimpleDateFormat("MMMM dd, yyyy • HH:mm", Locale.getDefault())
        val metaText = "Total Pages: ${book.totalPages}  |  Read: ${book.currentPage} pages (${if (book.totalPages > 0) (book.currentPage * 100 / book.totalPages) else 0}%)  |  Status: ${if (book.isCompleted) "Completed ✅" else "In Progress 📖"}\nExported on: ${dateFormat.format(Date())}"
        val metaLayout = StaticLayout.Builder.obtain(metaText, 0, metaText.length, metaPaint, contentWidth.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()
        canvas.save()
        canvas.translate(margin, yPos)
        metaLayout.draw(canvas)
        canvas.restore()
        yPos += metaLayout.height + 18f

        // Horizontal Line
        canvas.drawLine(margin, yPos, pageWidth - margin, yPos, Paint().apply { color = Color.LTGRAY; strokeWidth = 1f })
        yPos += 20f

        // Notes Section
        val sectionTitle = "Compiled Notes & Annotations (${notes.size} entries)"
        val sectionLayout = StaticLayout.Builder.obtain(sectionTitle, 0, sectionTitle.length, headerPaint, contentWidth.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()
        canvas.save()
        canvas.translate(margin, yPos)
        sectionLayout.draw(canvas)
        canvas.restore()
        yPos += sectionLayout.height + 15f

        if (notes.isEmpty()) {
            val emptyText = "No specific page notes were added for this book. Total reading progress recorded is ${book.currentPage} of ${book.totalPages} pages."
            val emptyLayout = StaticLayout.Builder.obtain(emptyText, 0, emptyText.length, bodyPaint, contentWidth.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build()
            canvas.save()
            canvas.translate(margin, yPos)
            emptyLayout.draw(canvas)
            canvas.restore()
            yPos += emptyLayout.height + 20f
        } else {
            val noteDateFmt = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            notes.forEachIndexed { index, note ->
                // Check page height overflow
                if (yPos > pageHeight - 90) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    yPos = 40f
                }

                val noteHeader = "${index + 1}. [Page ${note.pageNumber}] ${note.title.ifBlank { "Note" }}"
                val noteHeaderLayout = StaticLayout.Builder.obtain(noteHeader, 0, noteHeader.length, noteTitlePaint, contentWidth.toInt())
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .build()
                canvas.save()
                canvas.translate(margin, yPos)
                noteHeaderLayout.draw(canvas)
                canvas.restore()
                yPos += noteHeaderLayout.height + 4f

                if (note.content.isNotBlank()) {
                    val contentLayout = StaticLayout.Builder.obtain(note.content, 0, note.content.length, bodyPaint, (contentWidth - 15f).toInt())
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .build()
                    canvas.save()
                    canvas.translate(margin + 15f, yPos)
                    contentLayout.draw(canvas)
                    canvas.restore()
                    yPos += contentLayout.height + 4f
                }

                val timestampText = "Saved: ${noteDateFmt.format(Date(note.timestamp))}"
                val tsLayout = StaticLayout.Builder.obtain(timestampText, 0, timestampText.length, metaPaint, contentWidth.toInt())
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .build()
                canvas.save()
                canvas.translate(margin + 15f, yPos)
                tsLayout.draw(canvas)
                canvas.restore()
                yPos += tsLayout.height + 14f
            }
        }

        pdfDocument.finishPage(page)

        val sanitizeTitle = book.title.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val fileName = "${sanitizeTitle}_Summary.pdf"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        pdfDocument.writeTo(outputStream)
                    }
                    pdfDocument.close()
                    return uri
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val file = File(downloadsDir, fileName)
                FileOutputStream(file).use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }
                pdfDocument.close()
                return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        pdfDocument.close()
        return null
    }

    fun openOrSharePdf(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(Intent.createChooser(intent, "Open Summary PDF"))
        } catch (_: Exception) {
            Toast.makeText(context, "Summary PDF saved to Downloads folder!", Toast.LENGTH_LONG).show()
        }
    }
}
