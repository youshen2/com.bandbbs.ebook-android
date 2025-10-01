package com.bandbbs.ebook.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bandbbs.ebook.logic.InterHandshake
import com.bandbbs.ebook.logic.InterconnetFile
import com.bandbbs.ebook.ui.model.Book
import com.bandbbs.ebook.utils.UritoFile
import com.bandbbs.ebook.utils.bytesToReadable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private lateinit var conn: InterHandshake
    private lateinit var fileConn: InterconnetFile

    private val booksDir = File(application.filesDir, "books").apply { mkdirs() }

    private val _connectionState = MutableStateFlow(ConnectionState())
    val connectionState = _connectionState.asStateFlow()

    private val _books = MutableStateFlow<List<Book>>(emptyList())
    val books = _books.asStateFlow()

    private val _pushState = MutableStateFlow(PushState())
    val pushState = _pushState.asStateFlow()

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
                                statusText = "喵喵电子书未安装",
                                descriptionText = "请在手环上安装喵喵电子书",
                                isConnected = false
                            )
                        }
                        return@launch
                    }
                } catch (e: Exception) {
                    _connectionState.update {
                        it.copy(
                            statusText = "喵喵电子书未安装",
                            descriptionText = "请在手环上安装喵喵电子书",
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
        val bookFiles = booksDir.listFiles() ?: return
        _books.value = bookFiles.map { file ->
            Book(name = file.name, path = file.absolutePath, size = file.length())
        }.sortedByDescending { it.name }
    }

    fun importBook(uri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            UritoFile(uri, context)?.let { sourceFile ->
                val destFile = File(booksDir, sourceFile.name)
                sourceFile.copyTo(destFile, overwrite = true)
                sourceFile.delete() // Delete temp file from cache
                loadBooks()
            }
        }
    }

    fun deleteBook(book: Book) {
        viewModelScope.launch {
            File(book.path).delete()
            loadBooks()
        }
    }

    fun startPush(book: Book) {
        if (fileConn.busy) return

        _pushState.value = PushState(book = book, preview = "准备开始传输...")

        viewModelScope.launch {
            val file = File(book.path)
            fileConn.sentFile(
                file = file,
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

    fun cancelPush() {
        if (fileConn.busy) {
            fileConn.cancel()
        }
        resetPushState()
    }

    fun resetPushState() {
        _pushState.value = PushState()
    }
}
