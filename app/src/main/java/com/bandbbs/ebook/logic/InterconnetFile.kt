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

data class BookStatusResult(val chapterCount: Int, val hasCover: Boolean)

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
        chapters = emptyList()
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
                        
                        if (!hasPendingCoverTransfer) {
                            sendNextChapter(0)
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
                        sendNextChapter(nextSlicedListIndex)
                    }

                    "next_chunk" -> {
                        if (!busy) return@listener
                        sendCurrentChunk()
                    }
                    
                    "cover_chunk_received" -> {
                        if (!busy) return@listener
                        sendNextCoverChunk()
                    }

                    "cancel" -> {
                        onSuccess("取消传输", 0)
                        resetTransferState()
                    }

                    "book_status" -> {
                        val jsonMessage = json.decodeFromString<FileMessagesFromDevice.BookStatus>(it)
                        bookStatusCompleter?.complete(BookStatusResult(jsonMessage.chapterCount, jsonMessage.hasCover))
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
        chapters: List<com.bandbbs.ebook.database.Chapter>,
        totalChaptersInBook: Int,
        startFromIndex: Int,
        coverImagePath: String?,
        onError: (message: String, count: Int) -> Unit,
        onSuccess: (message: String, count: Int) -> Unit,
        onProgress: (progress: Double, String, status: String) -> Unit,
        onCoverProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ) {
        conn.setOnDisconnected {
            onError("连接断开", 0)
            resetTransferState()
        }
        this.chapters = chapters
        this.totalChaptersInBook = totalChaptersInBook
        this.transferStartChapterIndex = startFromIndex
        this.onError = onError
        this.onSuccess = onSuccess
        this.onProgress = onProgress
        this.onCoverProgress = onCoverProgress

        this.chapterIndexMap = chapters.mapIndexed { listIndex, chapter -> 
            chapter.index to listIndex 
        }.toMap()

        this.currentChapterChunks = emptyList()
        this.currentChunkIndex = 0
        this.currentChapterForTransfer = null
        this.lastChunkTime = 0L

        busy = true
        isCoverOnlyTransfer = false
        onProgress(0.0, chapters.firstOrNull()?.name ?: "", " --")
        delay(200L)

        val chapterIndices = chapters.map { it.index }
        
        
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
                    hasCover = hasCoverImage
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

    private fun sendNextChapter(chapterIndexInSlicedList: Int) {
        if (chapterIndexInSlicedList < 0 || chapterIndexInSlicedList >= chapters.size) {
            if (chapterIndexInSlicedList >= chapters.size) {
                onProgress(1.0, "", " --")
                onSuccess("传输完成", chapters.size)
            } else {
                onError("无效的章节索引: $chapterIndexInSlicedList", currentChapterIndexInBook)
            }
            resetTransferState()
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
                resetTransferState()
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
                
                Log.d("File", "Cover image chunks: ${coverImageChunks.size}")
                
                
                sendNextCoverChunk()
            } catch (e: Exception) {
                Log.e("File", "Failed to send cover image", e)
                onError("封面发送失败: ${e.message}", 0)
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
                
                Log.d("File", "Cover image transfer completed, starting chapter transfer")
                coverImageChunks = emptyList()
                currentCoverChunkIndex = 0
                hasPendingCoverTransfer = false
                onCoverProgress(0, 0) 
                
                if (isCoverOnlyTransfer) {
                    
                } else {
                    
                    sendNextChapter(0)
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
            val chapterCount: Int,
            val hasCover: Boolean = false
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
            val hasCover: Boolean = false
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
