package com.bandbbs.ebook.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bandbbs.ebook.database.AppDatabase
import com.bandbbs.ebook.database.BookEntity
import com.bandbbs.ebook.database.Chapter
import com.bandbbs.ebook.logic.InterHandshake
import com.bandbbs.ebook.logic.InterconnetFile
import com.bandbbs.ebook.ui.model.Book
import com.bandbbs.ebook.utils.ChapterSplitter
import com.bandbbs.ebook.utils.UritoFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min

data class ConnectionState(
    val statusText: String = "手环连接中",
    val descriptionText: String = "请确保小米运动健康后台运行",
    val isConnected: Boolean = false
)

data class PushState(
    val book: Book? = null,
    val progress: Double = 0.0,
    val preview: String = "...",
    val speed: String = "0 B/s",
    val statusText: String = "等待中...",
    val isFinished: Boolean = false,
    val isSuccess: Boolean = false
)

data class ImportState(
    val uri: Uri,
    val bookName: String,
    val splitMethod: String = ChapterSplitter.METHOD_DEFAULT,
    val noSplit: Boolean = false
)

data class ImportingState(
    val bookName: String,
    val statusText: String = "正在准备",
    val progress: Float = 0f
)

data class ImportReportState(
    val bookName: String,
    val mergedChaptersInfo: String
)

data class SyncOptionsState(
    val book: Book,
    val totalChapters: Int,
    val syncedChapters: Int
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private lateinit var conn: InterHandshake
    private lateinit var fileConn: InterconnetFile

    private val booksDir = File(application.filesDir, "books").apply { mkdirs() }
    private val db = AppDatabase.getDatabase(application)

    private val _connectionState = MutableStateFlow(ConnectionState())
    val connectionState = _connectionState.asStateFlow()

    private val _books = MutableStateFlow<List<Book>>(emptyList())
    val books = _books.asStateFlow()

    private val _pushState = MutableStateFlow(PushState())
    val pushState = _pushState.asStateFlow()

    private val _importState = MutableStateFlow<ImportState?>(null)
    val importState = _importState.asStateFlow()

    private val _importingState = MutableStateFlow<ImportingState?>(null)
    val importingState = _importingState.asStateFlow()

    private val _importReportState = MutableStateFlow<ImportReportState?>(null)
    val importReportState = _importReportState.asStateFlow()

    private val _selectedBookForChapters = MutableStateFlow<Book?>(null)
    val selectedBookForChapters = _selectedBookForChapters.asStateFlow()

    private val _chaptersForSelectedBook = MutableStateFlow<List<Chapter>>(emptyList())
    val chaptersForSelectedBook = _chaptersForSelectedBook.asStateFlow()

    private val _chapterToPreview = MutableStateFlow<Chapter?>(null)
    val chapterToPreview = _chapterToPreview.asStateFlow()

    private val _bookToDelete = MutableStateFlow<Book?>(null)
    val bookToDelete = _bookToDelete.asStateFlow()

    private val _syncOptionsState = MutableStateFlow<SyncOptionsState?>(null)
    val syncOptionsState = _syncOptionsState.asStateFlow()


    init {
        loadBooks()
    }

    fun setConnection(connection: InterHandshake) {
        this.conn = connection
        this.fileConn = InterconnetFile(conn)
        reconnect()
    }

    fun reconnect() {
        viewModelScope.launch {
            _connectionState.update {
                it.copy(
                    statusText = "手环连接中",
                    descriptionText = "请确保小米运动健康后台运行",
                    isConnected = false
                )
            }
            try {
                conn.destroy().await()
                val deviceName = conn.connect().await().replace(" ", "")
                conn.auth().await()
                try {
                    if (!conn.getAppState().await()) {
                        _connectionState.update {
                            it.copy(
                                statusText = "弦电子书未安装",
                                descriptionText = "请在手环上安装小程序",
                                isConnected = false
                            )
                        }
                        return@launch
                    }
                } catch (e: Exception) {
                    _connectionState.update {
                        it.copy(
                            statusText = "弦电子书未安装",
                            descriptionText = "请在手环上安装小程序",
                            isConnected = false
                        )
                    }
                    return@launch
                }
                conn.openApp().await()
                conn.registerListener().await()
                _connectionState.update {
                    it.copy(
                        statusText = "设备连接成功",
                        descriptionText = "$deviceName 已连接",
                        isConnected = true
                    )
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "connect fail ${e.message}")
                _connectionState.update {
                    it.copy(
                        statusText = "手环连接失败",
                        descriptionText = e.message ?: "未知错误",
                        isConnected = false
                    )
                }
            }
        }
    }

    private fun loadBooks() {
        viewModelScope.launch(Dispatchers.IO) {
            val bookEntities = db.bookDao().getAllBooks()
            val bookUiModels = bookEntities.map { entity ->
                val chapterCount = db.chapterDao().getChapterCountForBook(entity.id)
                val wordCount = db.chapterDao().getTotalWordCountForBook(entity.id) ?: 0
                Book(
                    name = entity.name,
                    path = entity.path,
                    size = entity.size,
                    chapterCount = chapterCount,
                    wordCount = wordCount,
                    syncedChapterCount = 0 // Placeholder, real value from watch
                )
            }
            withContext(Dispatchers.Main) {
                _books.value = bookUiModels.sortedByDescending { it.name }
            }
        }
    }

    fun startImport(uri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            UritoFile(uri, context)?.let { sourceFile ->
                _importState.value =
                    ImportState(uri = uri, bookName = sourceFile.nameWithoutExtension)
            }
        }
    }

    fun cancelImport() {
        _importState.value = null
    }

    fun confirmImport(bookName: String, splitMethod: String, noSplit: Boolean) {
        val state = _importState.value ?: return
        _importState.value = null // Hide the options sheet

        val finalBookName = bookName.trim()
        if (finalBookName.isEmpty()) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _importingState.value = ImportingState(bookName = finalBookName)
            val context = getApplication<Application>().applicationContext
            UritoFile(state.uri, context)?.let { sourceFile ->
                _importingState.update { it?.copy(statusText = "正在复制文件...") }
                val destFile = File(booksDir, sourceFile.name)
                sourceFile.copyTo(destFile, overwrite = true)

                _importingState.update { it?.copy(statusText = "正在写入数据库...") }
                val bookId = db.bookDao().insert(
                    BookEntity(
                        name = finalBookName,
                        path = destFile.absolutePath,
                        size = destFile.length()
                    )
                )

                val initialChapters = if (noSplit) {
                    _importingState.update { it?.copy(statusText = "正在读取全文...", progress = 0.5f) }
                    val content = ChapterSplitter.readTextFromUri(context, state.uri)
                    listOf(
                        Chapter(
                            bookId = bookId.toInt(),
                            index = 0,
                            name = "全文",
                            content = content.trim(),
                            wordCount = content.trim().length
                        )
                    )
                } else {
                    ChapterSplitter.split(context, state.uri, bookId.toInt(), splitMethod) { progress, status ->
                        _importingState.update {
                            it?.copy(
                                statusText = status,
                                progress = progress
                            )
                        }
                    }
                }

                _importingState.update { it?.copy(statusText = "正在后处理章节...", progress = 0.9f) }
                val finalChapters = mutableListOf<Chapter>()
                val mergedChapterTitles = mutableListOf<String>()

                for (chapter in initialChapters) {
                    if (chapter.wordCount == 0 && chapter.content.isBlank()) {
                        if (finalChapters.isNotEmpty()) {
                            val lastChapter = finalChapters.last()
                            val updatedContent = lastChapter.content.trimEnd() + "\n\n" + chapter.name.trim()
                            finalChapters[finalChapters.size - 1] = lastChapter.copy(
                                content = updatedContent,
                                wordCount = updatedContent.length
                            )
                            mergedChapterTitles.add(chapter.name)
                        } else {
                            mergedChapterTitles.add("${chapter.name} (因内容为空已被跳过)")
                        }
                    } else {
                        finalChapters.add(chapter)
                    }
                }

                val reIndexedChapters = finalChapters.mapIndexed { index, chapter ->
                    chapter.copy(index = index)
                }

                _importingState.update { it?.copy(statusText = "正在保存章节...", progress = 1.0f) }
                db.chapterDao().insertAll(reIndexedChapters)

                sourceFile.delete()
                withContext(Dispatchers.Main) {
                    _importingState.value = null
                    loadBooks()
                    if (mergedChapterTitles.isNotEmpty()) {
                        val reportMessage = "有 ${mergedChapterTitles.size} 个章节因内容为空，其标题已被合并到上一章节末尾或被跳过:\n\n" +
                                mergedChapterTitles.joinToString("\n") { "- $it" }
                        _importReportState.value = ImportReportState(
                            bookName = finalBookName,
                            mergedChaptersInfo = reportMessage
                        )
                    }
                }
            } ?: run {
                _importingState.value = null
            }
        }
    }

    fun dismissImportReport() {
        _importReportState.value = null
    }

    fun requestDeleteBook(book: Book) {
        _bookToDelete.value = book
    }

    fun confirmDeleteBook() {
        _bookToDelete.value?.let { book ->
            viewModelScope.launch(Dispatchers.IO) {
                File(book.path).delete()
                val bookEntity = db.bookDao().getBookByPath(book.path)
                if (bookEntity != null) {
                    db.chapterDao().deleteChaptersByBookId(bookEntity.id)
                    db.bookDao().delete(bookEntity)
                }
                withContext(Dispatchers.Main) {
                    loadBooks()
                }
            }
        }
        _bookToDelete.value = null
    }

    fun cancelDeleteBook() {
        _bookToDelete.value = null
    }

    fun startPush(book: Book) {
        if (fileConn.busy || _syncOptionsState.value != null) return

        _syncOptionsState.value = SyncOptionsState(book, 0, 0)

        viewModelScope.launch {
            try {
                conn.init()
                delay(500L) // Give watch time to settle
                val syncedChapters = withContext(Dispatchers.IO) {
                    fileConn.getBookStatus(book.name)
                }
                val totalChapters = withContext(Dispatchers.IO) {
                    val bookEntity = db.bookDao().getBookByPath(book.path)
                    if (bookEntity != null) db.chapterDao().getChapterCountForBook(bookEntity.id) else 0
                }
                _syncOptionsState.value = SyncOptionsState(book, totalChapters, syncedChapters)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to get book status", e)
                _pushState.update { it.copy(statusText = "获取手环状态失败: ${e.message}", isFinished = true, isSuccess = false, book = book) }
                _syncOptionsState.value = null
            }
        }
    }

    fun confirmPush(book: Book, startChapter: Int, chapterCount: Int) {
        _syncOptionsState.value = null
        val finalChapterCount = if (chapterCount > 400) 400 else chapterCount
        if (finalChapterCount <= 0) {
            return
        }

        _pushState.value = PushState(book = book, preview = "准备开始传输...")

        viewModelScope.launch(Dispatchers.IO) {
            val bookEntity = db.bookDao().getBookByPath(book.path) ?: return@launch
            val allChapters = db.chapterDao().getChaptersForBook(bookEntity.id)

            val startFromIndex = (startChapter - 1).coerceIn(0, allChapters.size)
            val endFromIndex = (startFromIndex + finalChapterCount).coerceAtMost(allChapters.size)

            if (startFromIndex >= endFromIndex) {
                withContext(Dispatchers.Main) {
                    _pushState.update { it.copy(statusText = "没有需要同步的章节", isFinished = true, isSuccess = true) }
                }
                return@launch
            }

            val chaptersToSend = allChapters.subList(startFromIndex, endFromIndex)

            withContext(Dispatchers.Main) {
                fileConn.sentChapters(
                    book = book,
                    chapters = chaptersToSend,
                    totalChaptersInBook = allChapters.size,
                    startFromIndex = startFromIndex,
                    onError = { error, _ ->
                        _pushState.update {
                            it.copy(
                                statusText = "传输失败: $error",
                                isFinished = true,
                                isSuccess = false
                            )
                        }
                    },
                    onSuccess = { _, _ ->
                        _pushState.update {
                            it.copy(
                                statusText = "传输成功",
                                progress = 1.0,
                                isFinished = true,
                                isSuccess = true
                            )
                        }
                    },
                    onProgress = { p, preview, speed ->
                        _pushState.update {
                            it.copy(
                                progress = p,
                                preview = preview,
                                speed = speed,
                                statusText = "正在推送 ${(p * 100).toInt()}%"
                            )
                        }
                    },
                )
            }
        }
    }

    fun cancelPush() {
        if (fileConn.busy) {
            fileConn.cancel()
        }
        conn.setOnDisconnected { }
        _syncOptionsState.value = null
        resetPushState()
    }

    fun resetPushState() {
        _pushState.value = PushState()
    }

    fun showChapterList(book: Book) {
        viewModelScope.launch(Dispatchers.IO) {
            val bookEntity = db.bookDao().getBookByPath(book.path)
            if (bookEntity != null) {
                val chapters = db.chapterDao().getChaptersForBook(bookEntity.id)
                withContext(Dispatchers.Main) {
                    _chaptersForSelectedBook.value = chapters
                    _selectedBookForChapters.value = book
                }
            }
        }
    }

    fun closeChapterList() {
        _selectedBookForChapters.value = null
        _chaptersForSelectedBook.value = emptyList()
    }

    fun showChapterPreview(chapter: Chapter) {
        _chapterToPreview.value = chapter
    }

    fun closeChapterPreview() {
        _chapterToPreview.value = null
    }
}
