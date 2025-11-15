package com.bandbbs.ebook.ui.model

data class Book(
    val name: String,
    val path: String,
    val size: Long,
    val chapterCount: Int = 0,
    val syncedChapterCount: Int = 0,
    val wordCount: Long = 0,
    val coverImagePath: String? = null,
    val localCategory: String? = null
)
