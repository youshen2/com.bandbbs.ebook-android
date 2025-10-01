package com.bandbbs.ebook.logic
import org.mozilla.universalchardet.UniversalDetector
import java.nio.charset.Charset
import android.util.Log
import com.bandbbs.ebook.utils.bytesToReadable
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class InterconnetFile(private val conn: InterHandshake) {
    private val FILE_SIZE = 1024 *20 // 20kb
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }
    private lateinit var chunks: List<String>
    private lateinit var file: File
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
                        if (jsonMessage.found&& jsonMessage.length?.toInt()!! >0) {
                            val currentChunk = jsonMessage.length / FILE_SIZE
                            if(currentChunk.toInt()>chunks.size)sendNextChunk(0, true)
                            else sendNextChunk(currentChunk.toInt(), true)
                        }else{
                            sendNextChunk(0)
                        }
                    }
                    "error" -> {
                        val jsonMessage = json.decodeFromString<FileMessagesFromDevice.Error>(it)
                        sendNextChunk(jsonMessage.count)
                    }
                    "success" -> {
                        busy = false
                        val jsonMessage = json.decodeFromString<FileMessagesFromDevice.Success>(it)
                        onSuccess(jsonMessage.message, jsonMessage.count)
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
    suspend fun sentFile(
        file: File,
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
        this.file = file
        //将文本文档分割成多个分块并发送
        val chunkSize = FILE_SIZE
        // Read raw bytes and detect charset automatically
        val fileBytes = file.readBytes()
        val detected = detectCharset(fileBytes)
        val content = fileBytes.toString(Charset.forName(detected))
        // Chunk the decoded content by UTF-8 bytes
        chunks = content.chunkedByBytes(chunkSize, charset = Charsets.UTF_16LE)
        busy = true
        onProgress(0.0,chunks[0]," --")
        delay(1000L) //等待应用打开
        conn.sendMessage(
            json.encodeToString(
                FileMessagesToSend.StartTransfer(
                    filename = file.name, total = chunks.size - 1,
                    chunkSize = FILE_SIZE
                )
            )
        )
        this.onError = onError
        this.onSuccess = onSuccess
        this.onProgress = onProgress
        Log.d("File", "sentFile")
    }
    private fun detectCharset(bytes: ByteArray): String {
        val detector = UniversalDetector(null)
        detector.handleData(bytes, 0, bytes.size)
        detector.dataEnd()
        val charset = detector.detectedCharset ?: "UTF-16LE"
        detector.reset()
        return charset
    }
    private fun sendNextChunk(
        currentChunk: Int,
        isReSend:Boolean=false
    ){
            val chunk = chunks[currentChunk]
            val message = FileMessagesToSend.DataChunk(
                count = currentChunk,
                data = chunk,
                setCount = if (isReSend) currentChunk else null
            )
            // Calculate speed
            val currentTime = System.currentTimeMillis()
            if (lastChunkTime != 0L) {
                val timeTaken = currentTime - lastChunkTime
                val speed = bytesToReadable(FILE_SIZE / (timeTaken / 1000.0))
                val remainingTime = (chunks.size - currentChunk) * (currentTime - lastChunkTime) / 1000.0
                onProgress(currentChunk.toDouble()/chunks.size, chunks[currentChunk], " $speed/s ${remainingTime.toInt()}s")
            } else {
                onProgress(currentChunk.toDouble()/chunks.size, chunks[currentChunk], " --")
            }
            lastChunkTime = currentTime
            conn.sendMessage(json.encodeToString(message)).invokeOnCompletion {
                if (it != null) {
                    onError("发送失败", currentChunk)
                }
            }
        if (currentChunk >= chunks.size-1){
            busy=false
            onProgress(1.0,chunks[0]," --")
            onSuccess("传输完成", chunks.size)
            conn.setOnDisconnected {  }
            conn.destroy()
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
        // 准备状态（新文件/续传）
        @Serializable
        data class Ready(
            val type: String = "ready",  // 固定为 "ready"
            val found: Boolean,            // 文件是否已存在
            val usage: Long,               // 当前存储占用
            val length: Long? = null       // 仅当 found=true 时有效（已存在文件的长度）
        ) : FileMessagesFromDevice()

        // 分块错误（序号不匹配）
        @Serializable
        data class Error(
            val type: String = "error",  // 固定为 "error"
            val message: String,           // 错误描述（如 "package count error"）
            val count: Int                 // 当前期望的分块序号
        ) : FileMessagesFromDevice()

        // 传输成功（所有分块接收完成）
        @Serializable
        data class Success(
            val type: String = "success",// 固定为 "success"
            val message: String,           // 成功描述（如 "transfer success"）
            val count: Int                 // 最终分块序号
        ) : FileMessagesFromDevice()

        // 请求下一分块（当前分块接收成功）
        @Serializable
        data class Next(
            val type: String = "next",   // 固定为 "next"
            val message: String,           // 提示信息（如 "X success"）
            val count: Int                 // 已接收的分块序号
        ) : FileMessagesFromDevice()

        // 传输取消
        @Serializable
        data class Cancel(
            val type: String = "cancel"  // 固定为 "cancel"
        ) : FileMessagesFromDevice()

        // 存储占用查询响应
        @Serializable
        data class Usage(
            val type: String = "usage",   // 固定为 "usage"
            val usage: Long                // 当前存储总占用（字节）
        ) : FileMessagesFromDevice()
    }
    @Serializable
    private sealed class FileMessagesToSend {
        // 查询存储占用（发送方请求当前存储使用情况）
        @Serializable
        data class GetUsage(
            val tag:String="file",
            val stat: String = "getUsage"  // 固定为 "getUsage"
        ) : FileMessagesToSend()

        // 开始传输（发送方通知启动文件传输）
        @Serializable
        data class StartTransfer(
            val tag:String="file",
            val stat: String = "startTransfer",  // 固定为 "startTransfer"
            val filename: String,                 // 待传输的文件名
            val total: Int,                       // 总分块数
            val chunkSize: Int       // 每个分块的大小（字节）
        ) : FileMessagesToSend()

        // 数据分块（发送方发送文件分块数据）
        @Serializable
        data class DataChunk(
            val tag:String="file",
            val stat: String = "d",               // 固定为 "d"
            val setCount: Int?,            // 可选：重置当前分块序号（用于续传）
            val count: Int,                       // 当前分块序号
            val data: String                      // 分块数据内容（文本格式）
        ) : FileMessagesToSend()

        // 取消传输（发送方请求中断传输）
        @Serializable
        data class Cancel(
            val tag:String="file",
            val stat: String = "cancel"           // 固定为 "cancel"
        ) : FileMessagesToSend()
    }
    private fun String.chunkedByBytes(byteSize: Int, charset: Charset = Charsets.UTF_16LE): List<String> {
        val byteArray = this.toByteArray(charset)
        val chunks = mutableListOf<String>()
        var index = 0

        while (index < byteArray.size) {
            val end = (index + byteSize).coerceAtMost(byteArray.size)
            chunks.add(String(byteArray.copyOfRange(index, end), charset))
            index = end
        }

        return chunks
    }
}

