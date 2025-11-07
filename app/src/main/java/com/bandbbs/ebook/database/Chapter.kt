package com.bandbbs.ebook.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(
    tableName = "chapters",
    foreignKeys = [ForeignKey(
        entity = BookEntity::class,
        parentColumns = ["id"],
        childColumns = ["bookId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["bookId"])]
)
@Serializable
data class Chapter(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookId: Int,
    val index: Int,
    var name: String,
    val contentFilePath: String,
    val wordCount: Int = 0
)
