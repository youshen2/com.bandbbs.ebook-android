package com.bandbbs.ebook.logic
import android.util.Log
import com.bandbbs.ebook.ui.model.Book
import com.bandbbs.ebook.utils.bytesToReadable
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class InterconnetFile(private val conn: InterHandshake) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }
    private var chapters: List<com.bandbbs.ebook.database.Chapter> = emptyList()
    private var lastChunkTime: Long = 0
    private lateinit var onError: (message: String, count: Int) -> Unit
    private lateinit var onSuccess: (message: String, count: Int) -> Unit
    private lateinit var onProgress: (progress: Double, chunkPreview: String, status: String) -> Unit
    var busy=false
    init {
        conn.addListener("file") listener@{
            try {
                val type = json.decodeFromString<FileMessagesFromDevice.Header>(it).type
                when (type) {
                    "ready"-> {
                        val jsonMessage = json.decodeFromString<FileMessagesFromDevice.Ready>(it)
                        if(jsonMessage.usage>25*1024*1024){
                            onError("存储空间不足", 0)
                            busy = false
                            conn.destroy()
                            return@listener
                        }
                        sendNextChunk(jsonMessage.count)
                    }
                    "error" -> {
                        val jsonMessage = json.decodeFromString<FileMessagesFromDevice.Error>(it)
                        onError(jsonMessage.message, jsonMessage.count)
                        busy = false
                        conn.setOnDisconnected {  }
                        conn.destroy()
                    }
                    "success" -> {
                        busy = false
                        val jsonMessage = json.decodeFromString<FileMessagesFromDevice.Success>(it)
                        onSuccess(jsonMessage.message, jsonMessage.count)
                        conn.setOnDisconnected {  }
                        conn.destroy()
                    }
                    "next" -> {
                        val jsonMessage = json.decodeFromString<FileMessagesFromDevice.Next>(it)
                        sendNextChunk(jsonMessage.count)
                    }
                    "cancel" -> {
                        busy = false
                        conn.destroy()
                        onSuccess("取消传输", 0)
                    }

                    "usuage"-> TODO()
                }
            } catch (e: Exception) {
                Log.e("file", "Error parsing JSON message: $it", e)
            }
        }
    }
    suspend fun sentChapters(
        book: Book,
        chapters: List<com.bandbbs.ebook.database.Chapter>,
        onError: (message: String, count: Int) -> Unit,
        onSuccess: (message: String, count: Int) -> Unit,
        onProgress: (progress: Double, String, status: String) -> Unit,
    ) {
        conn.init()
        conn.destroy().await()
        conn.registerListener().await()
        conn.setOnDisconnected {
            onError("连接断开", 0)
        }
        this.chapters = chapters
        busy = true
        onProgress(0.0, chapters.firstOrNull()?.name ?: ""," --")
        delay(1000L) //等待应用打开

        conn.sendMessage(
            json.encodeToString(
                FileMessagesToSend.StartTransfer(
                    filename = book.name,
                    total = chapters.size - 1,
                    wordCount = book.wordCount
                )
            )
        )
        this.onError = onError
        this.onSuccess = onSuccess
        this.onProgress = onProgress
        Log.d("File", "sentChapters")
    }

    private fun sendNextChunk(
        currentChunk: Int
    ){
        if (currentChunk >= chapters.size){
            busy=false
            onProgress(1.0,""," --")
            onSuccess("传输完成", chapters.size)
            conn.setOnDisconnected {  }
            conn.destroy()
            return
        }

        val chunkObject = chapters[currentChunk]
        val chunk = json.encodeToString(chunkObject)
        val message = FileMessagesToSend.DataChunk(
            count = currentChunk,
            data = chunk,
        )

        val currentTime = System.currentTimeMillis()
        if (lastChunkTime != 0L) {
            val timeTaken = currentTime - lastChunkTime
            val speed = bytesToReadable(chunk.toByteArray().size / (timeTaken / 1000.0))
            val remainingTime = (chapters.size - currentChunk) * (currentTime - lastChunkTime) / 1000.0
            onProgress(currentChunk.toDouble()/chapters.size, chunkObject.name, " $speed/s ${remainingTime.toInt()}s")
        } else {
            onProgress(currentChunk.toDouble()/chapters.size, chunkObject.name, " --")
        }
        lastChunkTime = currentTime

        conn.sendMessage(json.encodeToString(message)).invokeOnCompletion {
            if (it != null) {
                onError("发送失败", currentChunk)
            }
        }

        Log.d("File","sendNextChunk$currentChunk")
    }
    fun cancel(){
        conn.destroy()
        busy=false
        conn.sendMessage(json.encodeToString(FileMessagesToSend.Cancel()))
    }
    @Serializable
    private sealed class FileMessagesFromDevice {
        @Serializable
        data class Header(
            val tag:String="file",
            val type: String
        ):FileMessagesFromDevice()
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
        data class Cancel(
            val type: String = "cancel"
        ) : FileMessagesFromDevice()

        @Serializable
        data class Usage(
            val type: String = "usage",
            val usage: Long
        ) : FileMessagesFromDevice()
    }
    @Serializable
    private sealed class FileMessagesToSend {
        @Serializable
        data class GetUsage(
            val tag:String="file",
            val stat: String = "getUsage"
        ) : FileMessagesToSend()

        @Serializable
        data class StartTransfer(
            val tag:String="file",
            val stat: String = "startTransfer",
            val filename: String,
            val total: Int,
            val wordCount: Long
        ) : FileMessagesToSend()

        @Serializable
        data class DataChunk(
            val tag:String="file",
            val stat: String = "d",
            val count: Int,
            val data: String
        ) : FileMessagesToSend()

        @Serializable
        data class Cancel(
            val tag:String="file",
            val stat: String = "cancel"
        ) : FileMessagesToSend()
    }
}
