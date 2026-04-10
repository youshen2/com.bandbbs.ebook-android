package com.bandbbs.ebook.utils.manager

import android.content.Context
import android.util.Log
import java.io.File
import java.io.IOException
import java.util.UUID

object ChapterContentManager {
    private const val TAG = "ChapterContentManager"

    fun getChaptersDir(context: Context): File {
        return File(context.filesDir, "chapters").apply { mkdirs() }
    }

    fun saveChapterContent(
        context: Context,
        bookId: Int,
        chapterIndex: Int,
        content: String
    ): String {
        try {
            val chaptersDir = getChaptersDir(context)
            val bookDir = File(chaptersDir, "book_$bookId").apply {
                if (!exists() && !mkdirs()) {
                    throw IOException("Failed to create book directory: $absolutePath")
                }
            }
            val chapterFile = generateChapterFile(bookDir, chapterIndex)
            chapterFile.writeText(content)
            Log.d(TAG, "Saved chapter $chapterIndex for book $bookId (${content.length} chars)")
            return chapterFile.absolutePath
        } catch (e: IOException) {
            Log.e(TAG, "Failed to save chapter $chapterIndex for book $bookId", e)
            throw e
        }
    }

    private fun generateChapterFile(bookDir: File, chapterIndex: Int): File {
        var suffix = ""
        var attempt = 0
        var candidate: File
        do {
            val uniqueSegment = if (attempt == 0) "" else "_${UUID.randomUUID()}"
            candidate = File(bookDir, "chapter_${chapterIndex}${suffix}${uniqueSegment}.txt")
            attempt++
            suffix = "_$attempt"
        } while (candidate.exists())
        return candidate
    }

    fun readChapterContent(filePath: String): String {
        val file = File(filePath)
        return try {
            if (file.exists()) {
                file.readText()
            } else {
                Log.w(TAG, "Chapter file not found: $filePath")
                ""
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to read chapter content from: $filePath", e)
            ""
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Out of memory reading chapter: $filePath", e)
            ""
        }
    }

    fun deleteBookChapters(context: Context, bookId: Int): Boolean {
        return try {
            val chaptersDir = getChaptersDir(context)
            val bookDir = File(chaptersDir, "book_$bookId")
            if (bookDir.exists()) {
                val success = bookDir.deleteRecursively()
                if (success) {
                    Log.d(TAG, "Deleted all chapters for book $bookId")
                } else {
                    Log.w(TAG, "Failed to delete some chapters for book $bookId")
                }
                success
            } else {
                Log.d(TAG, "No chapters directory found for book $bookId")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting chapters for book $bookId", e)
            false
        }
    }

    fun deleteChapterContent(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                val success = file.delete()
                if (success) {
                    Log.d(TAG, "Deleted chapter file: $filePath")
                } else {
                    Log.w(TAG, "Failed to delete chapter file: $filePath")
                }
                success
            } else {
                Log.d(TAG, "Chapter file not found: $filePath")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting chapter file: $filePath", e)
            false
        }
    }
}

