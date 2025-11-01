package com.bandbbs.ebook.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val path: String,
    val size: Long,
    val format: String = "txt", 
    val coverImagePath: String? = null,
    val author: String? = null,
    val summary: String? = null,
    val bookStatus: String? = null,
    val category: String? = null
)
