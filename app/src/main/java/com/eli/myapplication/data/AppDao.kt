package com.eli.myapplication.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM books")
    fun getAllBooks(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE id = :bookId")
    suspend fun getBookById(bookId: Long): Book?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: Book): Long

    @Update
    suspend fun updateBook(book: Book)

    @Delete
    suspend fun deleteBook(book: Book)

    @Query("SELECT * FROM reading_sessions")
    fun getAllSessions(): Flow<List<ReadingSession>>

    @Insert
    suspend fun insertSession(session: ReadingSession)

    @Query("SELECT * FROM notes WHERE bookId = :bookId ORDER BY timestamp DESC")
    fun getNotesForBook(bookId: Long): Flow<List<Note>>

    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("SELECT * FROM reading_plans")
    fun getAllPlans(): Flow<List<ReadingPlan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: ReadingPlan)

    @Update
    suspend fun updatePlan(plan: ReadingPlan)

    @Delete
    suspend fun deletePlan(plan: ReadingPlan)
}
