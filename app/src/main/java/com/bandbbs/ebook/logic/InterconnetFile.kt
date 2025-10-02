package com.bandbbs.ebook.logic

import android.util.Log
import com.bandbbs.ebook.ui.model.Book
import com.bandbbs.ebook.utils.bytesToReadable
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class InterconnetFile(private val conn: InterHandshake) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }
    private var chapters: List<com.bandbbs.ebook.database.Chapter> = emptyList()
    private var totalChaptersInBook: Int = 0
    private var lastChunkTime: Long = 0
    private lateinit var onError: (message: String, count: Int) -> Unit
    private lateinit var onSuccess: (message: String, count: Int) -> Unit
    private lateinit var onProgress: (progress: Double, chunkPreview: String, status: String) -> Unit
    var busy = false
    private val CHUNK_SIZE = 10 * 1024

    private var currentChapterChunks: List<String> = emptyList()
    private var currentChunkIndex: Int = 0
    private var currentChapterForTransfer: com.bandbbs.ebook.database.Chapter? = null
    private var currentChapterIndexInBook: Int = 0
    private var currentChapterIndexInSlicedList: Int = 0
    private var bookStatusCompleter: CompletableDeferred<Int>? = null
    private var transferStartChapterIndex: Int = 0

    init {
        conn.addListener("file") listener@{
            try {
                val type = json.decodeFromString<FileMessagesFromDevice.Header>(it).type
                when (type) {
                    "ready" -> {
                        val jsonMessage = json.decodeFromString<FileMessagesFromDevice.Ready>(it)
                        if (jsonMessage.usage > 25 * 1024 * 1024) {
                            onError("存储空间不足", 0)
                            busy = false
                            return@listener
                        }
                        sendNextChapter(0)
                    }

                    "error" -> {
                        val jsonMessage = json.decodeFromString<FileMessagesFromDevice.Error>(it)
                        onError(jsonMessage.message, jsonMessage.count)
                        busy = false
                        conn.setOnDisconnected { }
                    }

                    "success" -> {
                        busy = false
                        val jsonMessage = json.decodeFromString<FileMessagesFromDevice.Success>(it)
                        onSuccess(jsonMessage.message, jsonMessage.count)
                        conn.setOnDisconnected { }
                    }

                    "next" -> {
                        if (!busy) return@listener
                        val jsonMessage = json.decodeFromString<FileMessagesFromDevice.Next>(it)
                        val nextAbsoluteIndex = jsonMessage.count
                        val nextSlicedListIndex = nextAbsoluteIndex - transferStartChapterIndex
                        sendNextChapter(nextSlicedListIndex)
                    }

                    "next_chunk" -> {
                        if (!busy) return@listener
                        sendCurrentChunk()
                    }

                    "cancel" -> {
                        busy = false
                        onSuccess("取消传输", 0)
                        conn.setOnDisconnected { }
                    }

                    "book_status" -> {
                        val jsonMessage = json.decodeFromString<FileMessagesFromDevice.BookStatus>(it)
                        bookStatusCompleter?.complete(jsonMessage.chapterCount)
                        bookStatusCompleter = null
                    }

                    "usuage" -> TODO()
                }
            } catch (e: Exception) {
                Log.e("file", "Error parsing JSON message: $it", e)
            }
        }
    }

    suspend fun getBookStatus(bookName: String): Int {
        conn.init()
        delay(500L) // Add delay for watch app to start
        bookStatusCompleter = CompletableDeferred()
        conn.sendMessage(json.encodeToString(FileMessagesToSend.GetBookStatus(filename = bookName))).await()
        val result = bookStatusCompleter!!.await()
        bookStatusCompleter = null
        return result
    }

    suspend fun sentChapters(
        book: Book,
        chapters: List<com.bandbbs.ebook.database.Chapter>,
        totalChaptersInBook: Int,
        startFromIndex: Int,
        onError: (message: String, count: Int) -> Unit,
        onSuccess: (message: String, count: Int) -> Unit,
        onProgress: (progress: Double, String, status: String) -> Unit,
    ) {
        conn.setOnDisconnected {
            onError("连接断开", 0)
        }
        this.chapters = chapters
        this.totalChaptersInBook = totalChaptersInBook
        this.transferStartChapterIndex = startFromIndex
        busy = true
        onProgress(0.0, chapters.firstOrNull()?.name ?: "", " --")
        delay(200L)

        conn.sendMessage(
            json.encodeToString(
                FileMessagesToSend.StartTransfer(
                    filename = book.name,
                    total = totalChaptersInBook,
                    wordCount = book.wordCount,
                    startFrom = startFromIndex
                )
            )
        ).await()
        this.onError = onError
        this.onSuccess = onSuccess
        this.onProgress = onProgress
        Log.d("File", "sentChapters")
    }

    private fun sendNextChapter(chapterIndexInSlicedList: Int) {
        if (chapterIndexInSlicedList >= chapters.size) {
            busy = false
            onProgress(1.0, "", " --")
            onSuccess("传输完成", chapters.size)
            conn.setOnDisconnected { }
            return
        }

        this.currentChapterIndexInSlicedList = chapterIndexInSlicedList
        val chapter = chapters[chapterIndexInSlicedList]
        currentChapterForTransfer = chapter
        currentChapterIndexInBook = chapter.index
        currentChapterChunks = chapter.content.chunked(CHUNK_SIZE)
        currentChunkIndex = 0
        sendCurrentChunk()
    }

    private fun sendCurrentChunk() {
        conn.scope.launch {
            if (currentChapterForTransfer == null || currentChunkIndex >= currentChapterChunks.size) {
                Log.w("File", "sendCurrentChunk called in invalid state.")
                return@launch
            }

            val chapter = currentChapterForTransfer!!
            val chunkContent = currentChapterChunks[currentChunkIndex]
            val totalChunks = currentChapterChunks.size

            val chapterForTransfer = ChapterForTransfer(
                index = chapter.index,
                name = chapter.name,
                content = chunkContent,
                wordCount = chapter.wordCount,
                chunkNum = currentChunkIndex,
                totalChunks = totalChunks
            )

            val dataString = json.encodeToString(chapterForTransfer)
            val message = FileMessagesToSend.DataChunk(
                count = currentChapterIndexInBook,
                data = dataString
            )

            val currentTime = System.currentTimeMillis()
            try {
                conn.sendMessage(json.encodeToString(message)).await()
            } catch (e: Exception) {
                onError("发送失败: ${e.message ?: "未知错误"}", currentChapterIndexInBook)
                busy = false
                conn.setOnDisconnected { }
                return@launch
            }

            val totalChaptersToSend = chapters.size.coerceAtLeast(1)
            val progress = (currentChapterIndexInSlicedList.toDouble() + (currentChunkIndex + 1.0) / totalChunks) / totalChaptersToSend

            if (lastChunkTime != 0L) {
                val timeTaken = currentTime - lastChunkTime
                if (timeTaken > 0) {
                    val speed = bytesToReadable(chunkContent.toByteArray().size / (timeTaken / 1000.0))
                    onProgress(
                        progress,
                        chapter.name + " (${currentChunkIndex + 1}/$totalChunks)",
                        " $speed/s"
                    )
                } else {
                    onProgress(
                        progress,
                        chapter.name + " (${currentChunkIndex + 1}/$totalChunks)",
                        " --"
                    )
                }
            } else {
                onProgress(
                    progress,
                    chapter.name + " (${currentChunkIndex + 1}/$totalChunks)",
                    " --"
                )
            }
            lastChunkTime = currentTime

            currentChunkIndex++
        }
    }


    fun cancel() {
        busy = false
        conn.scope.launch {
            try {
                conn.sendMessage(json.encodeToString(FileMessagesToSend.Cancel())).await()
            } catch (e: Exception) {
                // It might fail if connection is already bad, which is fine.
            }
        }
    }

    @Serializable
    private sealed class FileMessagesFromDevice {
        @Serializable
        data class Header(
            val tag: String = "file",
            val type: String
        ) : FileMessagesFromDevice()

        @Serializable
        data class Ready(
            val type: String = "ready",
            val usage: Long,
            val count: Int
        ) : FileMessagesFromDevice()

        @Serializable
        data class Error(
            val type: String = "error",
            val message: String,
            val count: Int
        ) : FileMessagesFromDevice()

        @Serializable
        data class Success(
            val type: String = "success",
            val message: String,
            val count: Int
        ) : FileMessagesFromDevice()

        @Serializable
        data class Next(
            val type: String = "next",
            val message: String,
            val count: Int
        ) : FileMessagesFromDevice()

        @Serializable
        data class NextChunk(
            val type: String = "next_chunk"
        ) : FileMessagesFromDevice()

        @Serializable
        data class Cancel(
            val type: String = "cancel"
        ) : FileMessagesFromDevice()

        @Serializable
        data class Usage(
            val type: String = "usage",
            val usage: Long
        ) : FileMessagesFromDevice()

        @Serializable
        data class BookStatus(
            val type: String = "book_status",
            val chapterCount: Int
        ) : FileMessagesFromDevice()
    }

    @Serializable
    private sealed class FileMessagesToSend {
        @Serializable
        data class GetUsage(
            val tag: String = "file",
            val stat: String = "getUsage"
        ) : FileMessagesToSend()

        @Serializable
        data class StartTransfer(
            val tag: String = "file",
            val stat: String = "startTransfer",
            val filename: String,
            val total: Int,
            val wordCount: Long,
            val startFrom: Int
        ) : FileMessagesToSend()

        @Serializable
        data class DataChunk(
            val tag: String = "file",
            val stat: String = "d",
            val count: Int,
            val data: String
        ) : FileMessagesToSend()

        @Serializable
        data class Cancel(
            val tag: String = "file",
            val stat: String = "cancel"
        ) : FileMessagesToSend()

        @Serializable
        data class GetBookStatus(
            val tag: String = "file",
            val stat: String = "get_book_status",
            val filename: String
        ) : FileMessagesToSend()
    }
}

@Serializable
private data class ChapterForTransfer(
    val index: Int,
    val name: String,
    val content: String,
    val wordCount: Int,
    val chunkNum: Int = 0,
    val totalChunks: Int = 1
)
