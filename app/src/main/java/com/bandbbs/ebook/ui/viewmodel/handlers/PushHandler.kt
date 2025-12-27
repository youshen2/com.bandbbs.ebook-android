package com.bandbbs.ebook.ui.viewmodel.handlers

import android.content.SharedPreferences
import android.util.Log
import com.bandbbs.ebook.database.AppDatabase
import com.bandbbs.ebook.ui.model.Book
import com.bandbbs.ebook.ui.viewmodel.PushState
import com.bandbbs.ebook.ui.viewmodel.SyncOptionsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Context
import com.bandbbs.ebook.notifications.ForegroundTransferService
import com.bandbbs.ebook.notifications.LiveNotificationManager

class PushHandler(
    private val db: AppDatabase,
    private val prefs: SharedPreferences,
    private val scope: CoroutineScope,
    private val pushState: MutableStateFlow<PushState>,
    private val syncOptionsState: MutableStateFlow<SyncOptionsState?>,
    private val firstSyncConfirmState: MutableStateFlow<Book?>,
    private val connectionHandler: ConnectionHandler,
    private val firstSyncConfirmedKey: String,
    private val appContext: Context
) {

    private var pendingPushBook: Book? = null
    private var pendingPushChapters: Set<Int>? = null
    private var pendingSyncCover: Boolean = false

    fun startPush(book: Book) {
        val fileConn = runCatching { connectionHandler.getFileConnection() }.getOrElse { return }
        if (fileConn.busy || syncOptionsState.value != null) return

        syncOptionsState.value = SyncOptionsState(book, 0, emptySet(), emptyList(), false)

        scope.launch {
            try {
                connectionHandler.reconnect()

                delay(500L)

                val conn = connectionHandler.getHandshake()
                conn.init()
                delay(500L)
                val bookStatus = withContext(Dispatchers.IO) {
                    fileConn.getBookStatus(book.name)
                }
                val (totalChapters, chapters, hasCover) = withContext(Dispatchers.IO) {
                    val bookEntity = db.bookDao().getBookByPath(book.path)
                    if (bookEntity != null) {
                        val count = db.chapterDao().getChapterCountForBook(bookEntity.id)
                        val chapterList = db.chapterDao().getChapterInfoForBook(bookEntity.id)
                        val hasCoverImage = bookEntity.coverImagePath != null
                        Triple(count, chapterList, hasCoverImage)
                    } else {
                        Triple(0, emptyList(), false)
                    }
                }
                syncOptionsState.value = SyncOptionsState(
                    book = book,
                    totalChapters = totalChapters,
                    syncedChapterIndices = bookStatus.syncedChapters.toSet(),
                    chapters = chapters,
                    hasCover = hasCover,
                    isCoverSynced = bookStatus.hasCover
                )
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to get book status", e)
                pushState.update {
                    it.copy(
                        statusText = "获取手环状态失败: ${e.message}",
                        isFinished = true,
                        isSuccess = false,
                        book = book
                    )
                }
                syncOptionsState.value = null
            }
        }
    }

    fun refreshBookStatus(book: Book) {
        scope.launch {
            try {
                connectionHandler.reconnect()
                delay(500L)
                val conn = connectionHandler.getHandshake()
                conn.init()
                delay(500L)
                val fileConn = connectionHandler.getFileConnection()
                val bookStatus = withContext(Dispatchers.IO) {
                    fileConn.getBookStatus(book.name)
                }
                val (totalChapters, chapters, hasCover) = withContext(Dispatchers.IO) {
                    val bookEntity = db.bookDao().getBookByPath(book.path)
                    if (bookEntity != null) {
                        val count = db.chapterDao().getChapterCountForBook(bookEntity.id)
                        val chapterList = db.chapterDao().getChapterInfoForBook(bookEntity.id)
                        val hasCoverImage = bookEntity.coverImagePath != null
                        Triple(count, chapterList, hasCoverImage)
                    } else {
                        Triple(0, emptyList(), false)
                    }
                }
                syncOptionsState.value?.let { currentState ->
                    syncOptionsState.value = currentState.copy(
                        syncedChapterIndices = bookStatus.syncedChapters.toSet(),
                        chapters = chapters,
                        hasCover = hasCover,
                        isCoverSynced = bookStatus.hasCover
                    )
                }
            } catch (e: Exception) {
                Log.e("PushHandler", "Failed to refresh book status", e)
            }
        }
    }

    private fun addTransferLog(message: String) {
        pushState.update { state ->
            val newLog = (state.transferLog + message).takeLast(100)
            state.copy(transferLog = newLog)
        }
    }

    fun syncCoverOnly(book: Book) {
        val fileConn = runCatching { connectionHandler.getFileConnection() }.getOrElse { return }
        if (fileConn.busy || book.coverImagePath == null) return

        val initialLog = listOf("准备传输封面...")
        pushState.value =
            PushState(book = book, preview = "准备传输封面...", transferLog = initialLog)

        scope.launch {
            fileConn.sendCoverOnly(
                book = book,
                coverImagePath = book.coverImagePath,
                onError = { error, _ ->
                    addTransferLog("[错误] 封面同步失败: $error")
                    pushState.update {
                        it.copy(
                            statusText = "封面同步失败: $error",
                            isFinished = true,
                            isSuccess = false
                        )
                    }
                },
                onSuccess = { _, _ ->
                    addTransferLog("[成功] 封面同步完成")
                    pushState.update {
                        it.copy(
                            statusText = "封面同步成功",
                            progress = 1.0,
                            isFinished = true,
                            isSuccess = true
                        )
                    }
                },
                onCoverProgress = { current, total ->
                    if (total > 0) {
                        val logMessage = "传输封面分块: $current/$total"
                        addTransferLog(logMessage)
                        pushState.update {
                            it.copy(
                                isSendingCover = true,
                                coverProgress = "封面: $current/$total",
                                statusText = "正在同步封面..."
                            )
                        }
                    } else {
                        pushState.update {
                            it.copy(isSendingCover = false, coverProgress = "")
                        }
                    }
                }
            )
        }
    }

    fun confirmPush(book: Book, selectedChapterIndices: Set<Int>, syncCover: Boolean = false) {
        if (selectedChapterIndices.isEmpty()) {
            return
        }

        val hasConfirmedFirstSync = prefs.getBoolean(firstSyncConfirmedKey, false)
        if (!hasConfirmedFirstSync) {
            pendingPushBook = book
            pendingPushChapters = selectedChapterIndices
            pendingSyncCover = syncCover
            firstSyncConfirmState.value = book
            return
        }

        scope.launch {
            delay(500L)

            val isCoverAlreadySynced = syncOptionsState.value?.isCoverSynced ?: false
            syncOptionsState.value = null
            performPush(book, selectedChapterIndices, syncCover, isCoverAlreadySynced)
        }
    }

    fun confirmFirstSync() {
        prefs.edit().putBoolean(firstSyncConfirmedKey, true).apply()
        firstSyncConfirmState.value = null

        val book = pendingPushBook
        val chapters = pendingPushChapters
        val syncCover = pendingSyncCover

        pendingPushBook = null
        pendingPushChapters = null
        pendingSyncCover = false

        if (book != null && chapters != null && chapters.isNotEmpty()) {
            scope.launch {
                val isCoverAlreadySynced = syncOptionsState.value?.isCoverSynced ?: false
                syncOptionsState.value = null
                performPush(book, chapters, syncCover, isCoverAlreadySynced)
            }
        }
    }

    fun cancelFirstSyncConfirm() {
        firstSyncConfirmState.value = null
        pendingPushBook = null
        pendingPushChapters = null
        pendingSyncCover = false
    }

    private fun performPush(
        book: Book,
        selectedChapterIndices: Set<Int>,
        syncCover: Boolean,
        isCoverAlreadySynced: Boolean
    ) {
        val fileConn = runCatching { connectionHandler.getFileConnection() }.getOrElse { return }

        val initialMessage =
            if (syncCover && !isCoverAlreadySynced) "准备传输封面..." else "准备开始传输..."
        val initialLog = listOf(initialMessage)
        pushState.value = PushState(
            book = book,
            preview = initialMessage,
            transferLog = initialLog,
            isTransferring = true
        )
        ForegroundTransferService.startService(
            appContext,
            "传输中",
            initialMessage,
            null
        )
        LiveNotificationManager.showTransferNotification("传输中", initialMessage, null)

        scope.launch(Dispatchers.IO) {
            val bookEntity = db.bookDao().getBookByPath(book.path) ?: return@launch

            val sortedIndices = selectedChapterIndices.sorted()
            if (sortedIndices.isEmpty()) {
                withContext(Dispatchers.Main) {
                    addTransferLog("[完成] 没有需要同步的章节")
                    pushState.update {
                        it.copy(
                            statusText = "没有需要同步的章节",
                            isFinished = true,
                            isSuccess = true,
                            isTransferring = false
                        )
                    }
                }
                return@launch
            }

            val firstChapterName = db.chapterDao().getChapterInfoForBook(bookEntity.id)
                .find { it.index == sortedIndices.first() }?.name ?: ""

            val startFromIndex = sortedIndices.first()
            val totalChaptersInBook = db.chapterDao().getChapterCountForBook(bookEntity.id)

            val coverImagePath =
                if (syncCover && !isCoverAlreadySynced) bookEntity.coverImagePath else null

            withContext(Dispatchers.Main) {
                addTransferLog("开始传输，共 ${sortedIndices.size} 章")
                if (coverImagePath != null) {
                    addTransferLog("包含封面图片")
                }

                fileConn.sentChapters(
                    book = book,
                    bookId = bookEntity.id,
                    chaptersIndicesToSend = sortedIndices,
                    chapterDao = db.chapterDao(),
                    totalChaptersInBook = totalChaptersInBook,
                    startFromIndex = startFromIndex,
                    firstChapterName = firstChapterName,
                    coverImagePath = coverImagePath,
                    bookEntity = bookEntity,
                    onError = { error, count ->
                        addTransferLog("[错误] 传输失败: $error (章节索引: $count)")
                        pushState.update {
                            it.copy(
                                statusText = "传输失败: $error",
                                isFinished = true,
                                isSuccess = false,
                                isTransferring = false
                            )
                        }
                        ForegroundTransferService.stopService(appContext)
                    },
                    onSuccess = { message, count ->
                        addTransferLog("[成功] $message，共传输 $count 章")
                        pushState.update {
                            it.copy(
                                statusText = "传输成功",
                                progress = 1.0,
                                isFinished = true,
                                isSuccess = true,
                                isTransferring = false
                            )
                        }
                        ForegroundTransferService.stopService(appContext)
                    },
                    onProgress = { p, preview, speed ->
                        val progressPercent = (p * 100).toInt()
                        val logMessage = if (preview.isNotEmpty()) {
                            "[$progressPercent%] $preview"
                        } else {
                            "[$progressPercent%] 传输中"
                        }
                        addTransferLog(logMessage)
                        pushState.update {
                            it.copy(
                                progress = p,
                                preview = preview,
                                speed = speed,
                                statusText = "正在推送 $progressPercent%",
                                isTransferring = true
                            )
                        }
                        val title = "$progressPercent%"
                        ForegroundTransferService.startService(appContext, title, preview, progressPercent)
                        LiveNotificationManager.showTransferNotification(title, preview, progressPercent)
                    },
                    onCoverProgress = { current, total ->
                        if (total > 0) {
                            val logMessage = "传输封面分块: $current/$total"
                            addTransferLog(logMessage)
                            pushState.update {
                                it.copy(
                                    isSendingCover = true,
                                    coverProgress = "封面: $current/$total",
                                    isTransferring = true
                                )
                            }
                        } else {
                            pushState.update {
                                it.copy(isSendingCover = false, coverProgress = "")
                            }
                        }
                    }
                )
            }
        }
    }

    fun cancelPush() {
        val fileConn = runCatching { connectionHandler.getFileConnection() }.getOrElse { return }
        if (fileConn.busy) {
            fileConn.cancel()
        }
        runCatching { connectionHandler.getHandshake() }.getOrNull()?.setOnDisconnected { }
        syncOptionsState.value = null
        resetPushState()
    }

    fun resetPushState() {
        pushState.value = PushState()
    }
}

