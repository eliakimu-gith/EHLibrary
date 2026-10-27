package com.eli.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_plans")
data class ReadingPlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val title: String,
    val targetDate: Long,
    val targetPages: Int,
    val isCompleted: Boolean = false
)
