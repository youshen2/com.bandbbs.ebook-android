package com.bandbbs.ebook.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ChapterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chapters: List<Chapter>)

    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY `index` ASC")
    suspend fun getChaptersForBook(bookId: Int): List<Chapter>

    @Query("SELECT COUNT(id) FROM chapters WHERE bookId = :bookId")
    suspend fun getChapterCountForBook(bookId: Int): Int

    @Query("SELECT SUM(wordCount) FROM chapters WHERE bookId = :bookId")
    suspend fun getTotalWordCountForBook(bookId: Int): Long?

    @Query("DELETE FROM chapters WHERE bookId = :bookId")
    suspend fun deleteChaptersByBookId(bookId: Int)
}
