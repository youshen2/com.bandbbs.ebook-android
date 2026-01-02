package com.bandbbs.ebook.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.bandbbs.ebook.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

object DataBackupManager {
    private const val TAG = "DataBackupManager"
    private const val READING_TIME_PREFS = "reading_time_prefs"
    private const val CHAPTER_READER_PREFS = "chapter_reader_prefs"

    suspend fun backupData(context: Context, uri: Uri, db: AppDatabase): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val backupJson = JSONObject()
                backupJson.put("version", 1)
                backupJson.put("timestamp", System.currentTimeMillis())

                val timePrefs = context.getSharedPreferences(READING_TIME_PREFS, Context.MODE_PRIVATE)
                val timeData = JSONObject()
                timePrefs.all.forEach { (key, value) ->
                    timeData.put(key, value)
                }
                backupJson.put("reading_time", timeData)

                val progressPrefs = context.getSharedPreferences(CHAPTER_READER_PREFS, Context.MODE_PRIVATE)
                val progressData = JSONArray()

                val books = db.bookDao().getAllBooks()
                books.forEach { book ->
                    val lastReadChapterId = progressPrefs.getInt("last_read_chapter_${book.id}", -1)
                    if (lastReadChapterId != -1) {
                        val chapterInfo = db.chapterDao().getChapterInfoByIndex(book.id, -1)
                        val chapters = db.chapterDao().getChapterInfoForBook(book.id)
                        val lastReadChapter = chapters.find { it.id == lastReadChapterId }

                        if (lastReadChapter != null) {
                            val bookProgress = JSONObject()
                            bookProgress.put("bookName", book.name)
                            bookProgress.put("lastReadChapterIndex", lastReadChapter.index)
                            bookProgress.put("lastReadTimestamp", progressPrefs.getLong("last_read_timestamp_${book.id}", 0L))

                            val chapterProgressArray = JSONArray()
                            chapters.forEach { chapter ->
                                val indexKey = "reading_position_index_${chapter.id}"
                                if (progressPrefs.contains(indexKey)) {
                                    val index = progressPrefs.getInt(indexKey, 0)
                                    val offset = progressPrefs.getInt("reading_position_offset_${chapter.id}", 0)
                                    val cp = JSONObject()
                                    cp.put("chapterIndex", chapter.index)
                                    cp.put("pageIndex", index)
                                    cp.put("offset", offset)
                                    chapterProgressArray.put(cp)
                                }
                            }
                            bookProgress.put("chapters", chapterProgressArray)
                            progressData.put(bookProgress)
                        }
                    }
                }
                backupJson.put("reading_progress", progressData)

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(backupJson.toString(2).toByteArray())
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Backup failed", e)
                Result.failure(e)
            }
        }
    }

    suspend fun restoreData(context: Context, uri: Uri, db: AppDatabase): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val stringBuilder = StringBuilder()
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        var line: String? = reader.readLine()
                        while (line != null) {
                            stringBuilder.append(line)
                            line = reader.readLine()
                        }
                    }
                }

                val backupJson = JSONObject(stringBuilder.toString())

                if (backupJson.has("reading_time")) {
                    val timeData = backupJson.getJSONObject("reading_time")
                    val timePrefs = context.getSharedPreferences(READING_TIME_PREFS, Context.MODE_PRIVATE)
                    val editor = timePrefs.edit()
                    val keys = timeData.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val value = timeData.get(key)
                        when (value) {
                            is Long -> editor.putLong(key, value)
                            is String -> editor.putString(key, value)
                            is Int -> editor.putLong(key, value.toLong())
                            else -> editor.putString(key, value.toString())
                        }
                    }
                    editor.apply()
                }

                if (backupJson.has("reading_progress")) {
                    val progressArray = backupJson.getJSONArray("reading_progress")
                    val progressPrefs = context.getSharedPreferences(CHAPTER_READER_PREFS, Context.MODE_PRIVATE)
                    val editor = progressPrefs.edit()

                    for (i in 0 until progressArray.length()) {
                        val bookProgress = progressArray.getJSONObject(i)
                        val bookName = bookProgress.getString("bookName")

                        val book = db.bookDao().getBookByName(bookName)
                        if (book != null) {
                            val chapters = db.chapterDao().getChapterInfoForBook(book.id)

                            if (bookProgress.has("lastReadChapterIndex")) {
                                val lastIndex = bookProgress.getInt("lastReadChapterIndex")
                                val lastChapter = chapters.find { it.index == lastIndex }
                                if (lastChapter != null) {
                                    editor.putInt("last_read_chapter_${book.id}", lastChapter.id)
                                    if (bookProgress.has("lastReadTimestamp")) {
                                        editor.putLong("last_read_timestamp_${book.id}", bookProgress.getLong("lastReadTimestamp"))
                                    }
                                }
                            }

                            if (bookProgress.has("chapters")) {
                                val chaptersArray = bookProgress.getJSONArray("chapters")
                                for (j in 0 until chaptersArray.length()) {
                                    val cp = chaptersArray.getJSONObject(j)
                                    val chapIndex = cp.getInt("chapterIndex")
                                    val targetChapter = chapters.find { it.index == chapIndex }
                                    if (targetChapter != null) {
                                        editor.putInt("reading_position_index_${targetChapter.id}", cp.getInt("pageIndex"))
                                        editor.putInt("reading_position_offset_${targetChapter.id}", cp.getInt("offset"))
                                    }
                                }
                            }
                        }
                    }
                    editor.apply()
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Restore failed", e)
                Result.failure(e)
            }
        }
    }
}
