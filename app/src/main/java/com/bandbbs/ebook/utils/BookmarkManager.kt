package com.bandbbs.ebook.utils

import android.content.Context
import com.bandbbs.ebook.database.AppDatabase
import com.bandbbs.ebook.database.BookmarkEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object BookmarkManager {
    suspend fun getBookmarks(context: Context, bookId: Int): List<BookmarkEntity> {
        return withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            db.bookmarkDao().getBookmarksByBookId(bookId)
        }
    }

    suspend fun addBookmark(
        context: Context,
        bookId: Int,
        name: String,
        chapterIndex: Int,
        chapterName: String,
        offsetInChapter: Int = 0,
        scrollOffset: Int = 0
    ): Long {
        return withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            val bookmark = BookmarkEntity(
                bookId = bookId,
                name = name,
                chapterIndex = chapterIndex,
                chapterName = chapterName,
                offsetInChapter = offsetInChapter,
                scrollOffset = scrollOffset,
                time = System.currentTimeMillis()
            )
            db.bookmarkDao().insert(bookmark)
        }
    }

    suspend fun deleteBookmark(context: Context, bookmarkId: Int) {
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            db.bookmarkDao().deleteById(bookmarkId)
        }
    }

    suspend fun updateBookmark(context: Context, bookmark: BookmarkEntity) {
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            db.bookmarkDao().update(bookmark)
        }
    }

    suspend fun deleteAllBookmarks(context: Context, bookId: Int) {
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            db.bookmarkDao().deleteAllByBookId(bookId)
        }
    }

    suspend fun syncBookmarksFromBand(
        context: Context,
        bookId: Int,
        bookmarks: List<BookmarkEntity>
    ) {
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            db.bookmarkDao().deleteAllByBookId(bookId)
            if (bookmarks.isNotEmpty()) {
                val bookmarksWithBookId = bookmarks.map { it.copy(bookId = bookId) }
                db.bookmarkDao().insertAll(bookmarksWithBookId)
            }
        }
    }

    suspend fun getBookmarksForSync(context: Context, bookId: Int): List<BookmarkEntity> {
        return withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            db.bookmarkDao().getBookmarksByBookId(bookId)
        }
    }
}

