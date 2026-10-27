package com.eli.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_sessions")
data class ReadingSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Long,
    val pagesRead: Int
)
