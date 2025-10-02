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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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

    private val _selectedBookForChapters = MutableStateFlow<Book?>(null)
    val selectedBookForChapters = _selectedBookForChapters.asStateFlow()

    private val _chaptersForSelectedBook = MutableStateFlow<List<Chapter>>(emptyList())
    val chaptersForSelectedBook = _chaptersForSelectedBook.asStateFlow()

    private val _chapterToPreview = MutableStateFlow<Chapter?>(null)
    val chapterToPreview = _chapterToPreview.asStateFlow()


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
                conn.auth()
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
                conn.openApp()
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
        val finalBookName = bookName.trim()
        if (finalBookName.isEmpty()) {
            _importState.value = null
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>().applicationContext
            UritoFile(state.uri, context)?.let { sourceFile ->
                val destFile = File(booksDir, sourceFile.name)
                sourceFile.copyTo(destFile, overwrite = true)

                val bookId = db.bookDao().insert(
                    BookEntity(
                        name = finalBookName,
                        path = destFile.absolutePath,
                        size = destFile.length()
                    )
                )

                val chapters = if (noSplit) {
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
                    ChapterSplitter.split(context, state.uri, bookId.toInt(), splitMethod)
                }
                db.chapterDao().insertAll(chapters)

                sourceFile.delete()
                withContext(Dispatchers.Main) {
                    _importState.value = null
                    loadBooks()
                }
            }
        }
    }


    fun deleteBook(book: Book) {
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

    fun startPush(book: Book) {
        if (fileConn.busy) return

        _pushState.value = PushState(book = book, preview = "准备开始传输...")

        viewModelScope.launch(Dispatchers.IO) {
            val bookEntity = db.bookDao().getBookByPath(book.path) ?: return@launch
            val chapters = db.chapterDao().getChaptersForBook(bookEntity.id)

            withContext(Dispatchers.Main) {
                fileConn.sentChapters(
                    book = book,
                    chapters = chapters,
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
