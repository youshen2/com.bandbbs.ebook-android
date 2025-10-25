package com.bandbbs.ebook.utils

import android.content.Context
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.GZIPInputStream
import org.json.JSONObject

/**
 * NVB 格式解析器
 * NVB (Novel Book) 是专为小说设计的自定义二进制书籍格式
 */
object NvbParser {

    private const val MAGIC_NUMBER = 0x114514AAu
    private const val VERSION = 1u

    
    private val OBFUSCATION_KEY = byteArrayOf(
        0x11, 0x45, 0x14, 0xAA.toByte(), 0x5E, 0x7B, 0x9C.toByte(), 0x3F,
        0xE2.toByte(), 0x8D.toByte(), 0x6A, 0xF1.toByte(), 0x42, 0xB8.toByte(), 0x2D, 0xC9.toByte()
    )

    data class NvbBook(
        val metadata: BookMetadata,
        val coverImage: ByteArray?,
        val chapters: List<NvbChapter>
    )

    data class BookMetadata(
        val id: String,
        val title: String,
        val author: String,
        val summary: String,
        val coverUrl: String,
        val sourceUrl: String,
        val wordCount: String,
        val category: String,
        val bookStatus: String
    )

    data class NvbChapter(
        val title: String,
        val id: String,
        val content: String,
        val wordCount: Int
    )

    /**
     * 从 Uri 解析 NVB 文件
     */
    fun parse(context: Context, uri: Uri): NvbBook {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            return parseFromInputStream(inputStream)
        } ?: throw IllegalArgumentException("无法打开文件")
    }

    /**
     * 从 InputStream 解析 NVB 文件
     */
    private fun parseFromInputStream(inputStream: InputStream): NvbBook {
        val allBytes = inputStream.readBytes()
        var offset = 0

        
        val magic = readUInt32(allBytes, offset)
        offset += 4
        if (magic != MAGIC_NUMBER) {
            throw IllegalArgumentException("无效的NVB文件：魔数不匹配 (期望: ${MAGIC_NUMBER.toString(16)}, 实际: ${magic.toString(16)})")
        }

        
        val version = readUInt32(allBytes, offset)
        offset += 4
        if (version != VERSION) {
            throw IllegalArgumentException("不支持的版本：$version")
        }

        
        val metadataOriginalLen = readUInt32(allBytes, offset).toInt()
        offset += 4
        val metadataCompressedLen = readUInt32(allBytes, offset).toInt()
        offset += 4
        val metadataBytes = allBytes.copyOfRange(offset, offset + metadataCompressedLen)
        offset += metadataCompressedLen

        val deobfuscatedMetadata = deobfuscate(metadataBytes)
        val decompressedMetadata = decompress(deobfuscatedMetadata)
        val metadataJson = JSONObject(String(decompressedMetadata, Charsets.UTF_8))
        val metadata = parseMetadata(metadataJson)

        
        val coverOriginalLen = readUInt32(allBytes, offset).toInt()
        offset += 4
        val coverCompressedLen = readUInt32(allBytes, offset).toInt()
        offset += 4
        val coverImage = if (coverCompressedLen > 0) {
            val coverBytes = allBytes.copyOfRange(offset, offset + coverCompressedLen)
            offset += coverCompressedLen
            val deobfuscatedCover = deobfuscate(coverBytes)
            decompress(deobfuscatedCover)
        } else {
            null
        }

        
        val chapterCount = readUInt32(allBytes, offset).toInt()
        offset += 4

        
        val chapters = mutableListOf<NvbChapter>()
        for (i in 0 until chapterCount) {
            
            val titleLen = readUInt32(allBytes, offset).toInt()
            offset += 4
            val titleBytes = allBytes.copyOfRange(offset, offset + titleLen)
            offset += titleLen
            val title = String(titleBytes, Charsets.UTF_8)

            
            val idLen = readUInt16(allBytes, offset).toInt()
            offset += 2
            val idBytes = allBytes.copyOfRange(offset, offset + idLen)
            offset += idLen
            val chapterId = String(idBytes, Charsets.UTF_8)

            
            val originalLen = readUInt32(allBytes, offset).toInt()
            offset += 4
            val compressedLen = readUInt32(allBytes, offset).toInt()
            offset += 4
            val wordCount = readUInt32(allBytes, offset).toInt()
            offset += 4

            
            val compressedBytes = allBytes.copyOfRange(offset, offset + compressedLen)
            offset += compressedLen
            val deobfuscatedContent = deobfuscate(compressedBytes)
            val decompressedContent = decompress(deobfuscatedContent)
            val content = String(decompressedContent, Charsets.UTF_8)

            chapters.add(NvbChapter(title, chapterId, content, wordCount))
        }

        return NvbBook(metadata, coverImage, chapters)
    }

    /**
     * 解析元数据 JSON
     */
    private fun parseMetadata(json: JSONObject): BookMetadata {
        return BookMetadata(
            id = json.optString("id", ""),
            title = json.optString("title", "未知书名"),
            author = json.optString("author", "未知作者"),
            summary = json.optString("summary", ""),
            coverUrl = json.optString("coverUrl", ""),
            sourceUrl = json.optString("sourceUrl", ""),
            wordCount = json.optString("wordCount", ""),
            category = json.optString("category", ""),
            bookStatus = json.optString("bookStatus", "")
        )
    }

    /**
     * XOR 解混淆
     */
    private fun deobfuscate(data: ByteArray): ByteArray {
        val result = data.copyOf()
        for (i in result.indices) {
            result[i] = (result[i].toInt() xor OBFUSCATION_KEY[i % OBFUSCATION_KEY.size].toInt()).toByte()
        }
        return result
    }

    /**
     * GZip 解压缩
     */
    private fun decompress(data: ByteArray): ByteArray {
        val outputStream = ByteArrayOutputStream()
        GZIPInputStream(data.inputStream()).use { gzipStream ->
            gzipStream.copyTo(outputStream)
        }
        return outputStream.toByteArray()
    }

    /**
     * 读取 32 位无符号整数（小端序）
     */
    private fun readUInt32(bytes: ByteArray, offset: Int): UInt {
        val buffer = ByteBuffer.wrap(bytes, offset, 4)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        return buffer.int.toUInt()
    }

    /**
     * 读取 16 位无符号整数（小端序）
     */
    private fun readUInt16(bytes: ByteArray, offset: Int): UInt {
        val buffer = ByteBuffer.wrap(bytes, offset, 2)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        return buffer.short.toUInt()
    }

    /**
     * 验证文件是否为 NVB 格式
     */
    fun isNvbFile(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val header = ByteArray(8)
                val read = inputStream.read(header)
                if (read < 8) return false
                val magic = readUInt32(header, 0)
                magic == MAGIC_NUMBER
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
}

