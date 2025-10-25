package com.bandbbs.ebook.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val path: String,
    val size: Long,
    val format: String = "txt", // txt, nvb, epub
    val coverImagePath: String? = null // NVB 封面图片路径
)
