package com.bandbbs.ebook.ui.viewmodel.handlers

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import com.bandbbs.ebook.database.AppDatabase
import com.bandbbs.ebook.database.Chapter
import com.bandbbs.ebook.ui.model.Book
import com.bandbbs.ebook.ui.model.ChapterEditContent
import com.bandbbs.ebook.ui.model.ChapterSegment
import com.bandbbs.ebook.utils.ChapterContentManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val PREFS_NAME = "chapter_reader_prefs"
private const val KEY_LAST_READ_CHAPTER = "last_read_chapter_"

class LibraryHandler(
    private val application: Application,
    private val db: AppDatabase,
    private val booksDir: File,
    private val scope: CoroutineScope,
    private val bookToDelete: MutableStateFlow<Book?>,
    private val selectedBookForChapters: MutableStateFlow<Book?>,
    private val chaptersForSelectedBook: MutableStateFlow<List<com.bandbbs.ebook.database.ChapterInfo>>,
    private val chapterToPreview: MutableStateFlow<com.bandbbs.ebook.ui.model.ChapterWithContent?>,
    private val chaptersForPreview: MutableStateFlow<List<com.bandbbs.ebook.database.ChapterInfo>>,
    private val bookForCoverImport: MutableStateFlow<Book?>,
    private val chapterEditorContent: MutableStateFlow<ChapterEditContent?>,
    private val onBooksChanged: () -> Unit,
) {

    fun requestDeleteBook(book: Book) {
        bookToDelete.value = book
    }

    fun cancelDeleteBook() {
        bookToDelete.value = null
    }

    fun confirmDeleteBook() {
        bookToDelete.value?.let { book ->
            scope.launch(Dispatchers.IO) {
                File(book.path).delete()
                val bookEntity = db.bookDao().getBookByPath(book.path)
                if (bookEntity != null) {
                    val context = application.applicationContext
                    ChapterContentManager.deleteBookChapters(context, bookEntity.id)
                    db.chapterDao().deleteChaptersByBookId(bookEntity.id)
                    db.bookDao().delete(bookEntity)
                }
                withContext(Dispatchers.Main) {
                    onBooksChanged()
                }
            }
        }
        bookToDelete.value = null
    }

    fun showChapterList(book: Book) {
        scope.launch(Dispatchers.IO) {
            val bookEntity = db.bookDao().getBookByPath(book.path)
            if (bookEntity != null) {
                val chapters = db.chapterDao().getChapterInfoForBook(bookEntity.id)
                withContext(Dispatchers.Main) {
                    chaptersForSelectedBook.value = chapters
                    selectedBookForChapters.value = book
                }
            }
        }
    }

    fun closeChapterList() {
        selectedBookForChapters.value = null
        chaptersForSelectedBook.value = emptyList()
    }

    fun showChapterPreview(chapterId: Int) {
        scope.launch(Dispatchers.IO) {
            val chapter = db.chapterDao().getChapterById(chapterId)
            if (chapter != null) {
                val content = ChapterContentManager.readChapterContent(chapter.contentFilePath)
                val chapterWithContent = com.bandbbs.ebook.ui.model.ChapterWithContent(
                    id = chapter.id,
                    name = chapter.name,
                    content = content,
                    wordCount = chapter.wordCount
                )
                
                val allChapters = db.chapterDao().getChapterInfoForBook(chapter.bookId)
                withContext(Dispatchers.Main) {
                    chapterToPreview.value = chapterWithContent
                    chaptersForPreview.value = allChapters
                }
            } else {
                withContext(Dispatchers.Main) {
                    chapterToPreview.value = null
                    chaptersForPreview.value = emptyList()
                }
            }
        }
    }

    fun continueReading(book: Book) {
        scope.launch(Dispatchers.IO) {
            val bookEntity = db.bookDao().getBookByPath(book.path) ?: return@launch
            val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastReadChapterId = prefs.getInt("$KEY_LAST_READ_CHAPTER${bookEntity.id}", -1)

            val chapterIdToOpen = if (lastReadChapterId != -1) {
                
                if (db.chapterDao().getChapterById(lastReadChapterId) != null) {
                    lastReadChapterId
                } else {
                    
                    val chapters = db.chapterDao().getChapterInfoForBook(bookEntity.id)
                    chapters.firstOrNull()?.id
                }
            } else {
                
                val chapters = db.chapterDao().getChapterInfoForBook(bookEntity.id)
                chapters.firstOrNull()?.id
            }

            if (chapterIdToOpen != null) {
                showChapterPreview(chapterIdToOpen)
            }
        }
    }

    fun closeChapterPreview() {
        chapterToPreview.value = null
        chaptersForPreview.value = emptyList()
    }

    fun requestImportCover(book: Book) {
        bookForCoverImport.value = book
    }

    fun cancelImportCover() {
        bookForCoverImport.value = null
    }

    fun importCoverForBook(uri: Uri) {
        val book = bookForCoverImport.value ?: return
        bookForCoverImport.value = null

        scope.launch(Dispatchers.IO) {
            try {
                val context = application.applicationContext
                val bookEntity = db.bookDao().getBookByPath(book.path) ?: return@launch

                val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                val imageBytes = inputStream.readBytes()
                inputStream.close()

                val coverFile = File(booksDir, "${bookEntity.name}_cover.jpg")
                coverFile.writeBytes(imageBytes)

                db.bookDao().update(bookEntity.copy(coverImagePath = coverFile.absolutePath))

                withContext(Dispatchers.Main) {
                    onBooksChanged()
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to import cover", e)
            }
        }
    }

    fun renameChapter(chapterId: Int, newTitle: String) {
        val title = newTitle.trim()
        if (title.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            val chapter = db.chapterDao().getChapterById(chapterId) ?: return@launch
            db.chapterDao().updateChapter(chapter.copy(name = title))
            refreshAfterMutation(chapter.bookId)
        }
    }

    fun moveChapter(chapterId: Int, direction: Int) {
        if (direction == 0) return
        scope.launch(Dispatchers.IO) {
            val chapter = db.chapterDao().getChapterById(chapterId) ?: return@launch
            val targetIndex = chapter.index + direction
            if (targetIndex < 0) return@launch
            val neighbor = db.chapterDao().getChapterByBookAndIndex(chapter.bookId, targetIndex)
                ?: return@launch
            db.chapterDao().updateChapter(neighbor.copy(index = chapter.index))
            db.chapterDao().updateChapter(chapter.copy(index = targetIndex))
            refreshAfterMutation(chapter.bookId)
        }
    }

    fun reorderChapter(chapterId: Int, targetIndex: Int) {
        scope.launch(Dispatchers.IO) {
            val chapter = db.chapterDao().getChapterById(chapterId) ?: return@launch
            val bookId = chapter.bookId
            val chapters =
                db.chapterDao().getChaptersForBook(bookId).sortedBy { it.index }.toMutableList()
            if (chapters.isEmpty()) return@launch
            val currentIdx = chapters.indexOfFirst { it.id == chapterId }
            if (currentIdx == -1) return@launch
            val boundedTarget = targetIndex.coerceIn(0, chapters.lastIndex)
            if (currentIdx == boundedTarget) return@launch
            val moving = chapters.removeAt(currentIdx)
            chapters.add(boundedTarget, moving)
            chapters.forEachIndexed { idx, item ->
                if (item.index != idx) {
                    db.chapterDao().updateChapter(item.copy(index = idx))
                }
            }
            refreshAfterMutation(bookId)
        }
    }

    fun openChapterEditor(chapterId: Int) {
        scope.launch(Dispatchers.IO) {
            val chapter = db.chapterDao().getChapterById(chapterId) ?: return@launch
            val content = ChapterContentManager.readChapterContent(chapter.contentFilePath)
            val state = ChapterEditContent(
                id = chapter.id,
                bookId = chapter.bookId,
                index = chapter.index,
                title = chapter.name,
                content = content
            )
            withContext(Dispatchers.Main) {
                chapterEditorContent.value = state
            }
        }
    }

    fun closeChapterEditor() {
        chapterEditorContent.value = null
    }

    fun saveChapterContent(chapterId: Int, title: String, content: String) {
        scope.launch(Dispatchers.IO) {
            val chapter = db.chapterDao().getChapterById(chapterId) ?: return@launch
            val file = File(chapter.contentFilePath)
            file.writeText(content)
            val updated = chapter.copy(
                name = if (title.isBlank()) chapter.name else title.trim(),
                wordCount = content.length
            )
            db.chapterDao().updateChapter(updated)
            refreshAfterMutation(chapter.bookId)
            withContext(Dispatchers.Main) {
                chapterEditorContent.value = null
            }
        }
    }

    fun addChapter(insertIndex: Int, title: String, content: String) {
        val book = selectedBookForChapters.value ?: return
        if (content.isBlank()) return
        scope.launch(Dispatchers.IO) {
            val bookEntity = db.bookDao().getBookByPath(book.path) ?: return@launch
            val chapterCount = db.chapterDao().getChapterCountForBook(bookEntity.id)
            val targetIndex = insertIndex.coerceIn(0, chapterCount)
            shiftIndices(bookEntity.id, targetIndex, 1)
            val filePath = ChapterContentManager.saveChapterContent(
                application.applicationContext,
                bookEntity.id,
                targetIndex,
                content
            )
            val chapter = Chapter(
                bookId = bookEntity.id,
                index = targetIndex,
                name = title.ifBlank { "新章节 ${chapterCount + 1}" },
                contentFilePath = filePath,
                wordCount = content.length
            )
            db.chapterDao().insertChapter(chapter)
            refreshAfterMutation(bookEntity.id)
        }
    }

    fun batchRenameChapters(
        chapterIds: List<Int>,
        prefix: String,
        suffix: String,
        startNumber: Int,
        padding: Int
    ) {
        if (chapterIds.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            val chapters = db.chapterDao().getChaptersByIds(chapterIds).sortedBy { it.index }
            if (chapters.isEmpty()) return@launch
            val padSize = padding.coerceAtLeast(0)
            chapters.forEachIndexed { idx, chapter ->
                val number = (startNumber + idx).coerceAtLeast(0)
                val formattedNumber = number.toString().let { value ->
                    if (padSize > 0) value.padStart(padSize, '0') else value
                }
                val newName = buildString {
                    if (prefix.isNotBlank()) append(prefix.trim())
                    append(formattedNumber)
                    if (suffix.isNotBlank()) append(suffix.trim())
                }.ifBlank { chapter.name }
                db.chapterDao().updateChapter(chapter.copy(name = newName))
            }
            refreshAfterMutation(chapters.first().bookId)
        }
    }

    fun mergeChapters(
        chapterIds: List<Int>,
        mergedTitle: String,
        insertBlankLine: Boolean
    ) {
        if (chapterIds.size < 2) return
        scope.launch(Dispatchers.IO) {
            val chapters = db.chapterDao().getChaptersByIds(chapterIds).sortedBy { it.index }
            if (chapters.isEmpty()) return@launch
            val bookId = chapters.first().bookId
            val separator = if (insertBlankLine) "\n\n" else ""
            val mergedContent = chapters.joinToString(separator) { chapter ->
                ChapterContentManager.readChapterContent(chapter.contentFilePath)
            }
            val first = chapters.first()
            File(first.contentFilePath).writeText(mergedContent)
            val renamed = first.copy(
                name = if (mergedTitle.isBlank()) first.name else mergedTitle.trim(),
                wordCount = mergedContent.length
            )
            db.chapterDao().updateChapter(renamed)
            val others = chapters.drop(1)
            others.forEach { ChapterContentManager.deleteChapterContent(it.contentFilePath) }
            if (others.isNotEmpty()) {
                db.chapterDao().deleteChapters(others)
            }
            normalizeChapterIndices(bookId)
            refreshAfterMutation(bookId)
        }
    }

    fun splitChapter(chapterId: Int, segments: List<ChapterSegment>) {
        val validSegments = segments.filter { it.content.isNotBlank() }
        if (validSegments.size < 2) return
        scope.launch(Dispatchers.IO) {
            val chapter = db.chapterDao().getChapterById(chapterId) ?: return@launch
            val bookId = chapter.bookId
            val additional = validSegments.size - 1
            shiftIndices(bookId, chapter.index + 1, additional)
            val firstSegment = validSegments.first()
            File(chapter.contentFilePath).writeText(firstSegment.content)
            val updatedFirst = chapter.copy(
                name = firstSegment.title.ifBlank { chapter.name },
                wordCount = firstSegment.content.length
            )
            db.chapterDao().updateChapter(updatedFirst)

            validSegments.drop(1).forEachIndexed { idx, segment ->
                val index = chapter.index + 1 + idx
                val path = ChapterContentManager.saveChapterContent(
                    application.applicationContext,
                    bookId,
                    index,
                    segment.content
                )
                val newChapter = Chapter(
                    bookId = bookId,
                    index = index,
                    name = segment.title.ifBlank { "${chapter.name}-${idx + 2}" },
                    contentFilePath = path,
                    wordCount = segment.content.length
                )
                db.chapterDao().insertChapter(newChapter)
            }
            normalizeChapterIndices(bookId)
            refreshAfterMutation(bookId)
            withContext(Dispatchers.Main) {
                chapterEditorContent.value = null
            }
        }
    }

    private suspend fun refreshAfterMutation(bookId: Int) {
        refreshSelectedBookChapters()
        withContext(Dispatchers.Main) {
            onBooksChanged()
        }
    }

    private suspend fun refreshSelectedBookChapters() {
        val book = selectedBookForChapters.value ?: return
        val bookEntity = db.bookDao().getBookByPath(book.path) ?: return
        val chapters = db.chapterDao().getChapterInfoForBook(bookEntity.id)
        withContext(Dispatchers.Main) {
            chaptersForSelectedBook.value = chapters
        }
    }

    private suspend fun shiftIndices(bookId: Int, startIndex: Int, delta: Int) {
        if (delta <= 0) return
        val chapters = db.chapterDao().getChaptersForBook(bookId)
            .filter { it.index >= startIndex }
            .sortedByDescending { it.index }
        chapters.forEach { chapter ->
            db.chapterDao().updateChapter(chapter.copy(index = chapter.index + delta))
        }
    }

    private suspend fun normalizeChapterIndices(bookId: Int) {
        val chapters = db.chapterDao().getChaptersForBook(bookId).sortedBy { it.index }
        chapters.forEachIndexed { newIndex, chapter ->
            if (chapter.index != newIndex) {
                db.chapterDao().updateChapter(chapter.copy(index = newIndex))
            }
        }
    }
}
