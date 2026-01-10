package com.bandbbs.ebook.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(
    tableName = "bookmarks",
    foreignKeys = [ForeignKey(
        entity = BookEntity::class,
        parentColumns = ["id"],
        childColumns = ["bookId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["bookId"])]
)
@Serializable
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookId: Int,
    val name: String,
    val chapterIndex: Int,
    val chapterName: String,
    val offsetInChapter: Int = 0,
    val scrollOffset: Int = 0,
    val time: Long = System.currentTimeMillis()
)

