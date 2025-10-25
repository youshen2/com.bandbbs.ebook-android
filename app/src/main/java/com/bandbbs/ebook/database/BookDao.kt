package com.bandbbs.ebook.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY id DESC")
    suspend fun getAllBooks(): List<BookEntity>

    @Query("SELECT * FROM books WHERE path = :path LIMIT 1")
    suspend fun getBookByPath(path: String): BookEntity?
    
    @Query("SELECT * FROM books WHERE name = :name LIMIT 1")
    suspend fun getBookByName(name: String): BookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(book: BookEntity): Long

    @Update
    suspend fun update(book: BookEntity)

    @Delete
    suspend fun delete(book: BookEntity)
}
