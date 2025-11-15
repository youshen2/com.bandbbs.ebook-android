package com.bandbbs.ebook.logic

import android.util.Log
import com.bandbbs.ebook.database.Chapter
import com.bandbbs.ebook.database.ChapterDao
import com.bandbbs.ebook.ui.model.Book
import com.bandbbs.ebook.utils.bytesToReadable
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class BookStatusResult(val syncedChapters: List<Int>, val hasCover: Boolean)

class InterconnetFile(private val conn: InterHandshake) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }
    private var chapterIndices: List<Int> = emptyList()
    private lateinit var chapterDao: ChapterDao
    private var bookId: Int = 0
    private var totalChaptersInBook: Int = 0
    private var lastChunkTime: Long = 0
    private lateinit var onError: (message: String, count: Int) -> Unit
    private lateinit var onSuccess: (message: String, count: Int) -> Unit
    private lateinit var onProgress: (progress: Double, chunkPreview: String, status: String) -> Unit
    private lateinit var onCoverProgress: (current: Int, total: Int) -> Unit
    var busy = false
    private val CHUNK_SIZE = 10 * 1024  

    private var currentChapterChunks: List<String> = emptyList()
    private var currentChunkIndex: Int = 0
    private var currentChapterForTransfer: com.bandbbs.ebook.database.Chapter? = null
    private var currentChapterIndexInBook: Int = 0
    private var currentChapterIndexInSlicedList: Int = 0
    private var bookStatusCompleter: CompletableDeferred<BookStatusResult>? = null
    private var transferStartChapterIndex: Int = 0
    private var chapterIndexMap: Map<Int, Int> = emptyMap()
    private var coverImageChunks: List<String> = emptyList()
    private var currentCoverChunkIndex: Int = 0
    private val COVER_CHUNK_SIZE = 8 * 1024 
    private var hasPendingCoverTransfer = false
    private var isCoverOnlyTransfer = false

    private fun resetTransferState() {
        busy = false
        chapterIndices = emptyList()
        totalChaptersInBook = 0
        lastChunkTime = 0
        currentChapterChunks = emptyList()
        currentChunkIndex = 0
        currentChapterForTransfer = null
        currentChapterIndexInBook = 0
        currentChapterIndexInSlicedList = 0
        bookStatusCompleter?.cancel()
        bookStatusCompleter = null
        transferStartChapterIndex = 0
        chapterIndexMap = emptyMap()
        coverImageChunks = emptyList()
        currentCoverChunkIndex = 0
        hasPendingCoverTransfer = false
        isCoverOnlyTransfer = false
    }

    init {
        conn.addListener("file") listener@{
            try {
                val type = json.decodeFromString<FileMessagesFromDevice.Header>(it).type
                when (type) {
                    "ready" -> {
                        val jsonMessage = json.decodeFromString<FileMessagesFromDevice.Ready>(it)
                        if (jsonMessage.usage > 25 * 1024 * 1024) {
                            onError("存储空间不足", 0)
                            resetTransferState()
                            return@listener
                        }
                        
                        if (hasPendingCoverTransfer) {
                            
                            Log.d("File", "Received ready signal, starting cover transfer")
                            sendNextCoverChunk()
                        } else {
                            
                            conn.scope.launch { sendNextChapter(0) }
                        }
                    }

                    "error" -> {
                        val jsonMessage = json.decodeFromString<FileMessagesFromDevice.Error>(it)
                        onError(jsonMessage.message, jsonMessage.count)
                        resetTransferState()
                    }

                    "success" -> {
                        val jsonMessage = json.decodeFromString<FileMessagesFromDevice.Success>(it)
                        onSuccess(jsonMessage.message, jsonMessage.count)
                        resetTransferState()
                    }

                    "next" -> {
                        if (!busy) return@listener
                        val jsonMessage = json.decodeFromString<FileMessagesFromDevice.Next>(it)
                        val nextAbsoluteIndex = jsonMessage.count
                        val nextSlicedListIndex = chapterIndexMap[nextAbsoluteIndex]
                        if (nextSlicedListIndex == null) {
                            onError("章节索引映射错误: $nextAbsoluteIndex", nextAbsoluteIndex)
                            resetTransferState()
                            return@listener
                        }
                        conn.scope.launch { sendNextChapter(nextSlicedListIndex) }
                    }

                    "next_chunk" -> {
                        if (!busy) return@listener
                        currentChunkIndex++
                        sendCurrentChunk()
                    }
                    
                    "chapter_chunk_complete" -> {
                        if (!busy) return@listener
                        
                        sendChapterComplete()
                    }
                    
                    "chapter_saved" -> {
                        if (!busy) return@listener
                        
                        val jsonMessage = json.decodeFromString<FileMessagesFromDevice.ChapterSaved>(it)
                        Log.d("File", "Chapter saved: ${jsonMessage.syncedCount}/${jsonMessage.totalCount}")
                        val nextSlicedListIndex = currentChapterIndexInSlicedList + 1
                        if (nextSlicedListIndex >= chapterIndices.size) {
                            
                            sendTransferComplete()
                        } else {
                            conn.scope.launch { sendNextChapter(nextSlicedListIndex) }
                        }
                    }
                    
                    "transfer_finished" -> {
                        if (!busy) return@listener
                        
                        Log.d("File", "Transfer finished confirmed by watch")
                        onProgress(1.0, "", " --")
                        onSuccess("传输完成", chapterIndices.size)
                        
                        busy = false
                    }
                    
                    "cover_chunk_received" -> {
                        if (!busy) return@listener
                        sendNextCoverChunk()
                    }
                    
                    "cover_ready" -> {
                        if (!busy) return@listener
                        
                        sendNextCoverChunk()
                    }
                    
                    "cover_saved" -> {
                        if (!busy) return@listener
                        
                        Log.d("File", "Cover saved successfully")
                        if (isCoverOnlyTransfer) {
                            onSuccess("封面同步完成", 1)
                            resetTransferState()
                        }
                    }

                    "cancel" -> {
                        onSuccess("取消传输", 0)
                        resetTransferState()
                    }

                    "book_status" -> {
                        val jsonMessage = json.decodeFromString<FileMessagesFromDevice.BookStatus>(it)
                        bookStatusCompleter?.complete(BookStatusResult(jsonMessage.syncedChapters, jsonMessage.hasCover))
                        bookStatusCompleter = null
                    }

                    "usuage" -> TODO()
                }
            } catch (e: Exception) {
                Log.e("file", "Error parsing JSON message: $it", e)
            }
        }
    }

    suspend fun getBookStatus(bookName: String): BookStatusResult {
        conn.init()
        delay(500L)
        bookStatusCompleter = CompletableDeferred()
        conn.sendMessage(json.encodeToString(FileMessagesToSend.GetBookStatus(filename = bookName))).await()
        val result = bookStatusCompleter!!.await()
        bookStatusCompleter = null
        return result
    }

    suspend fun sendCoverOnly(
        book: Book,
        coverImagePath: String,
        onError: (message: String, count: Int) -> Unit,
        onSuccess: (message: String, count: Int) -> Unit,
        onCoverProgress: (current: Int, total: Int) -> Unit
    ) {
        conn.setOnDisconnected {
            onError("连接断开", 0)
            resetTransferState()
        }
        this.onError = onError
        this.onSuccess = onSuccess
        this.onCoverProgress = onCoverProgress
        
        busy = true
        isCoverOnlyTransfer = true
        
        conn.sendMessage(
            json.encodeToString(
                FileMessagesToSend.StartCoverTransfer(filename = book.name)
            )
        ).await()
        
        hasPendingCoverTransfer = true
        sendCoverImage(coverImagePath)
    }

    suspend fun sentChapters(
        book: Book,
        bookId: Int,
        chaptersIndicesToSend: List<Int>,
        chapterDao: ChapterDao,
        totalChaptersInBook: Int,
        startFromIndex: Int,
        firstChapterName: String,
        coverImagePath: String?,
        bookEntity: com.bandbbs.ebook.database.BookEntity? = null,
        onError: (message: String, count: Int) -> Unit,
        onSuccess: (message: String, count: Int) -> Unit,
        onProgress: (progress: Double, String, status: String) -> Unit,
        onCoverProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ) {
        conn.setOnDisconnected {
            onError("连接断开", 0)
            resetTransferState()
        }
        this.bookId = bookId
        this.chapterDao = chapterDao
        this.chapterIndices = chaptersIndicesToSend
        this.totalChaptersInBook = totalChaptersInBook
        this.transferStartChapterIndex = startFromIndex
        this.onError = onError
        this.onSuccess = onSuccess
        this.onProgress = onProgress
        this.onCoverProgress = onCoverProgress

        this.chapterIndexMap = chaptersIndicesToSend.mapIndexed { listIndex, index ->
            index to listIndex
        }.toMap()

        this.currentChapterChunks = emptyList()
        this.currentChunkIndex = 0
        this.currentChapterForTransfer = null
        this.lastChunkTime = 0L

        busy = true
        isCoverOnlyTransfer = false
        onProgress(0.0, firstChapterName, " --")
        delay(200L)

        val chapterIndices = chaptersIndicesToSend
        
        
        val hasCoverImage = coverImagePath?.let { path ->
            val file = java.io.File(path)
            file.exists()
        } ?: false

        conn.sendMessage(
            json.encodeToString(
                FileMessagesToSend.StartTransfer(
                    filename = book.name,
                    total = totalChaptersInBook,
                    wordCount = book.wordCount,
                    startFrom = startFromIndex,
                    chapterIndices = chapterIndices,
                    hasCover = hasCoverImage,
                    author = bookEntity?.author,
                    summary = bookEntity?.summary,
                    bookStatus = bookEntity?.bookStatus,
                    category = bookEntity?.category,
                    localCategory = bookEntity?.localCategory
                )
            )
        ).await()
        
        
        if (hasCoverImage && coverImagePath != null) {
            hasPendingCoverTransfer = true
            sendCoverImage(coverImagePath)
        } else {
            hasPendingCoverTransfer = false
        }
        
        Log.d("File", "sentChapters")
    }

    private suspend fun sendNextChapter(chapterIndexInSlicedList: Int) {
        if (chapterIndexInSlicedList < 0 || chapterIndexInSlicedList >= chapterIndices.size) {
            if (chapterIndexInSlicedList >= chapterIndices.size) {
                onProgress(1.0, "", " --")
                onSuccess("传输完成", chapterIndices.size)
            } else {
                onError("无效的章节索引: $chapterIndexInSlicedList", currentChapterIndexInBook)
            }
            resetTransferState()
            return
        }

        this.currentChapterIndexInSlicedList = chapterIndexInSlicedList
        val chapterIndex = chapterIndices[chapterIndexInSlicedList]

        val chapterInfo = withContext(Dispatchers.IO) {
            chapterDao.getChapterInfoByIndex(bookId, chapterIndex)
        }

        if (chapterInfo == null) {
            onError("无法加载章节信息: index $chapterIndex", chapterIndex)
            resetTransferState()
            return
        }

        val chapterContent = withContext(Dispatchers.IO) {
            loadChapterContent(chapterInfo.id)
        }

        if (chapterContent == null) {
            onError("无法加载章节内容: index $chapterIndex", chapterIndex)
            resetTransferState()
            return
        }

        val chapter = Chapter(
            id = chapterInfo.id,
            bookId = chapterInfo.bookId,
            index = chapterInfo.index,
            name = chapterInfo.name,
            contentFilePath = "",
            wordCount = chapterInfo.wordCount
        )

        currentChapterForTransfer = chapter
        currentChapterIndexInBook = chapter.index
        currentChapterChunks = chapterContent.chunked(CHUNK_SIZE)
        currentChunkIndex = 0
        sendCurrentChunk()
    }

    private suspend fun loadChapterContent(chapterId: Int): String? {
        return try {
            val chapter = chapterDao.getChapterById(chapterId) ?: return null
            com.bandbbs.ebook.utils.ChapterContentManager.readChapterContent(chapter.contentFilePath)
        } catch (e: Exception) {
            Log.e("File", "Exception while loading chapter content for chapterId $chapterId", e)
            null
        }
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
                resetTransferState()
                return@launch
            }

            val totalChaptersToSend = chapterIndices.size.coerceAtLeast(1)
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
        }
    }


    private fun sendCoverImage(coverImagePath: String) {
        conn.scope.launch {
            try {
                val file = java.io.File(coverImagePath)
                if (!file.exists()) {
                    Log.e("File", "Cover image file not found")
                    return@launch
                }
                
                
                val originalBytes = file.readBytes()
                Log.d("File", "Original cover image size: ${originalBytes.size} bytes")
                
                val compressedBytes = compressCoverImage(originalBytes)
                Log.d("File", "Compressed cover image size: ${compressedBytes.size} bytes")
                
                val coverBase64 = android.util.Base64.encodeToString(compressedBytes, android.util.Base64.NO_WRAP)
                
                
                coverImageChunks = coverBase64.chunked(COVER_CHUNK_SIZE)
                currentCoverChunkIndex = 0
                
                Log.d("File", "Cover image prepared: ${coverImageChunks.size} chunks, waiting for ready signal")
                
                
            } catch (e: Exception) {
                Log.e("File", "Failed to prepare cover image", e)
                onError("封面准备失败: ${e.message}", 0)
                resetTransferState()
            }
        }
    }
    
    /**
     * 压缩封面图片以避免手环端内存溢出
     * 目标：保持图片在合理大小范围内（< 50KB）
     */
    private fun compressCoverImage(imageBytes: ByteArray): ByteArray {
        try {
            
            val originalBitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                ?: return imageBytes
            
            
            val targetWidth = 160
            val targetHeight = 213
            
            
            val scaleWidth = targetWidth.toFloat() / originalBitmap.width
            val scaleHeight = targetHeight.toFloat() / originalBitmap.height
            val scale = minOf(scaleWidth, scaleHeight)
            
            
            val finalWidth = if (scale < 1) (originalBitmap.width * scale).toInt() else originalBitmap.width
            val finalHeight = if (scale < 1) (originalBitmap.height * scale).toInt() else originalBitmap.height
            
            
            val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(
                originalBitmap, 
                finalWidth, 
                finalHeight, 
                true
            )
            
            
            val outputStream = java.io.ByteArrayOutputStream()
            var quality = 85 
            scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, outputStream)
            
            
            while (outputStream.size() > 50 * 1024 && quality > 20) {
                outputStream.reset()
                quality -= 10
                scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, outputStream)
            }
            
            val result = outputStream.toByteArray()
            
            
            originalBitmap.recycle()
            if (scaledBitmap != originalBitmap) {
                scaledBitmap.recycle()
            }
            
            Log.d("File", "Image compressed: ${imageBytes.size} -> ${result.size} bytes (quality: $quality)")
            return result
            
        } catch (e: Exception) {
            Log.e("File", "Failed to compress image, using original", e)
            return imageBytes
        }
    }
    
    private fun sendNextCoverChunk() {
        conn.scope.launch {
            if (currentCoverChunkIndex >= coverImageChunks.size) {
                
                Log.d("File", "All cover chunks sent, sending cover_transfer_complete command")
                
                try {
                    
                    conn.sendMessage(json.encodeToString(FileMessagesToSend.CoverTransferComplete())).await()
                    
                    coverImageChunks = emptyList()
                    currentCoverChunkIndex = 0
                    hasPendingCoverTransfer = false
                    onCoverProgress(0, 0)
                    
                    if (!isCoverOnlyTransfer) {
                        
                        conn.scope.launch { sendNextChapter(0) }
                    }
                } catch (e: Exception) {
                    Log.e("File", "Failed to send cover_transfer_complete", e)
                    onError("封面传输完成命令发送失败: ${e.message}", 0)
                    resetTransferState()
                }
                return@launch
            }
            
            try {
                val chunkData = coverImageChunks[currentCoverChunkIndex]
                val message = FileMessagesToSend.CoverChunk(
                    chunkIndex = currentCoverChunkIndex,
                    totalChunks = coverImageChunks.size,
                    data = chunkData
                )
                
                conn.sendMessage(json.encodeToString(message)).await()
                currentCoverChunkIndex++
                
                
                onCoverProgress(currentCoverChunkIndex, coverImageChunks.size)
                
                Log.d("File", "Sent cover chunk ${currentCoverChunkIndex}/${coverImageChunks.size}")
            } catch (e: Exception) {
                Log.e("File", "Failed to send cover chunk", e)
                onError("封面发送失败: ${e.message}", 0)
                resetTransferState()
            }
        }
    }

    fun cancel() {
        if (!busy) return
        conn.scope.launch {
            try {
                conn.sendMessage(json.encodeToString(FileMessagesToSend.Cancel())).await()
            } catch (e: Exception) {
            }
        }
        resetTransferState()
    }

    private fun sendChapterComplete() {
        conn.scope.launch {
            try {
                val message = FileMessagesToSend.ChapterComplete(count = currentChapterIndexInBook)
                conn.sendMessage(json.encodeToString(message)).await()
                Log.d("File", "Sent chapter_complete for chapter ${currentChapterIndexInBook}")
            } catch (e: Exception) {
                Log.e("File", "Failed to send chapter_complete", e)
                onError("章节完成命令发送失败: ${e.message}", currentChapterIndexInBook)
                resetTransferState()
            }
        }
    }

    private fun sendTransferComplete() {
        conn.scope.launch {
            try {
                val message = FileMessagesToSend.TransferComplete()
                conn.sendMessage(json.encodeToString(message)).await()
                Log.d("File", "Sent transfer_complete command")
            } catch (e: Exception) {
                Log.e("File", "Failed to send transfer_complete", e)
                
                onProgress(1.0, "", " --")
                onSuccess("传输完成（但未能通知手环）", chapterIndices.size)
                busy = false
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
            val syncedChapters: List<Int>,
            val hasCover: Boolean = false
        ) : FileMessagesFromDevice()
        
        @Serializable
        data class ChapterSaved(
            val type: String = "chapter_saved",
            val count: Int,
            val syncedCount: Int,
            val totalCount: Int,
            val progress: Double
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
            val startFrom: Int,
            val chapterIndices: List<Int>,
            val hasCover: Boolean = false,
            val author: String? = null,
            val summary: String? = null,
            val bookStatus: String? = null,
            val category: String? = null,
            val localCategory: String? = null
        ) : FileMessagesToSend()
        
        @Serializable
        data class CoverChunk(
            val tag: String = "file",
            val stat: String = "cover_chunk",
            val chunkIndex: Int,
            val totalChunks: Int,
            val data: String
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

        @Serializable
        data class StartCoverTransfer(
            val tag: String = "file",
            val stat: String = "start_cover_transfer",
            val filename: String
        ) : FileMessagesToSend()
        
        @Serializable
        data class CoverTransferComplete(
            val tag: String = "file",
            val stat: String = "cover_transfer_complete"
        ) : FileMessagesToSend()
        
        @Serializable
        data class ChapterComplete(
            val tag: String = "file",
            val stat: String = "chapter_complete",
            val count: Int
        ) : FileMessagesToSend()
        
        @Serializable
        data class TransferComplete(
            val tag: String = "file",
            val stat: String = "transfer_complete"
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
