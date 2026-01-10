package com.bandbbs.ebook.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY chapterIndex ASC, offsetInChapter ASC")
    suspend fun getBookmarksByBookId(bookId: Int): List<BookmarkEntity>

    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId AND chapterIndex = :chapterIndex")
    suspend fun getBookmarksByChapter(bookId: Int, chapterIndex: Int): List<BookmarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bookmarks: List<BookmarkEntity>)

    @Update
    suspend fun update(bookmark: BookmarkEntity)

    @Delete
    suspend fun delete(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE bookId = :bookId")
    suspend fun deleteAllByBookId(bookId: Int)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteById(id: Int)
}

