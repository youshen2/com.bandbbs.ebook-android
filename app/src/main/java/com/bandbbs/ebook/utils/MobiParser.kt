package com.bandbbs.ebook.utils

import android.content.Context
import android.net.Uri
import androidx.core.text.HtmlCompat
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.Charset
import java.util.zip.DataFormatException
import java.util.zip.GZIPInputStream
import java.util.zip.Inflater
import kotlin.math.min
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

object MobiParser {

    data class MobiChapter(
        val title: String,
        val content: String,
        val wordCount: Int
    )

    fun isMobiFile(context: Context, uri: Uri): Boolean {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return false
            isMobiBytes(bytes)
        } catch (_: Exception) {
            false
        }
    }

    fun extractPlainText(
        context: Context,
        uri: Uri,
        onProgress: ((progress: Float, status: String) -> Unit)? = null
    ): String {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("无法打开文件")
        val raw = extractRawString(bytes, onProgress)
        return toPlainText(raw)
    }

    fun extractChapters(
        context: Context,
        uri: Uri,
        onProgress: ((progress: Float, status: String) -> Unit)? = null
    ): List<MobiChapter> {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("无法打开文件")
        val raw = extractRawString(bytes, onProgress)
        onProgress?.invoke(0.92f, "正在解析章节结构...")

        val chaptersFromHtml = splitHtmlByHeadings(raw)
        if (chaptersFromHtml.size >= 2) {
            onProgress?.invoke(0.98f, "章节解析完成, 共 ${chaptersFromHtml.size} 章")
            return chaptersFromHtml
        }

        val text = toPlainText(raw)
        onProgress?.invoke(0.98f, "章节解析完成")
        return listOf(MobiChapter(title = "全文", content = text, wordCount = text.length))
    }

    private fun isMobiBytes(bytes: ByteArray): Boolean {
        if (bytes.size < 78) return false
        val type = String(bytes.copyOfRange(60, 64), Charsets.US_ASCII)
        val creator = String(bytes.copyOfRange(64, 68), Charsets.US_ASCII)
        if (!((creator == "MOBI" || creator == "KIND") && (type == "BOOK" || type == "TEXt"))) {
            return false
        }
        val numRecords = readU16BE(bytes, 76)
        if (numRecords <= 0) return false
        val recordListStart = 78
        val recordEntrySize = 8
        val recordListBytesNeeded = recordListStart + numRecords * recordEntrySize
        if (bytes.size < recordListBytesNeeded) return false
        val record0Start = readU32BE(bytes, recordListStart).toInt()
        if (record0Start <= 0 || record0Start + 20 > bytes.size) return false
        val magic = String(bytes.copyOfRange(record0Start + 16, record0Start + 20), Charsets.US_ASCII)
        return magic == "MOBI"
    }

    private fun extractRawString(
        bytes: ByteArray,
        onProgress: ((progress: Float, status: String) -> Unit)? = null
    ): String {
        if (bytes.size < 78) throw IllegalArgumentException("无效的MOBI文件")
        onProgress?.invoke(0.02f, "正在解析MOBI头信息...")

        val numRecords = readU16BE(bytes, 76)
        if (numRecords <= 1) throw IllegalArgumentException("无效的MOBI文件")

        val recordOffsets = IntArray(numRecords)
        val recordListStart = 78
        val recordEntrySize = 8
        val recordListBytesNeeded = recordListStart + numRecords * recordEntrySize
        if (bytes.size < recordListBytesNeeded) throw IllegalArgumentException("无效的MOBI文件")

        for (i in 0 until numRecords) {
            recordOffsets[i] = readU32BE(bytes, recordListStart + i * recordEntrySize).toInt()
        }
        if (recordOffsets.any { it <= 0 || it >= bytes.size }) throw IllegalArgumentException("无效的MOBI文件")

        val record0Start = recordOffsets[0]
        val record0End = recordEnd(recordOffsets, 0, bytes.size)
        if (record0End - record0Start < 16 + 16) throw IllegalArgumentException("无效的MOBI文件")

        val record0 = bytes.copyOfRange(record0Start, record0End)

        val compression = readU16BE(record0, 0)
        val textLength = readU32BE(record0, 4).toInt().coerceAtLeast(0)
        val textRecordCount = readU16BE(record0, 8).coerceAtLeast(0)

        if (record0.size < 20) throw IllegalArgumentException("无效的MOBI文件")
        val mobiMagic = String(record0.copyOfRange(16, 20), Charsets.US_ASCII)
        if (mobiMagic != "MOBI") throw IllegalArgumentException("无效的MOBI文件")

        val encodingCode = if (record0.size >= 16 + 0x10) readU32BE(record0, 16 + 0x0C).toLong() else 65001L
        val charset = when (encodingCode) {
            65001L -> Charsets.UTF_8
            1252L -> Charset.forName("windows-1252")
            1200L -> Charsets.UTF_16LE
            1201L -> Charsets.UTF_16BE
            else -> Charsets.UTF_8
        }

        if (textRecordCount == 0) return ""

        onProgress?.invoke(0.08f, "正在解压内容数据...")
        val out = ByteArrayOutputStream(min(textLength.coerceAtLeast(1024), 2_000_000))
        val maxRecords = min(textRecordCount, numRecords - 1)
        for (i in 1..maxRecords) {
            if (i == 1 || i % 5 == 0 || i == maxRecords) {
                val p = 0.08f + 0.82f * (i.toFloat() / maxRecords.toFloat())
                onProgress?.invoke(p, "正在解压数据块: $i/$maxRecords")
            }
            val start = recordOffsets[i]
            val end = recordEnd(recordOffsets, i, bytes.size)
            if (end <= start) continue
            val recordBytes = bytes.copyOfRange(start, end)
            val decompressed = when (compression) {
                0, 1 -> recordBytes
                2 -> palmdocDecompress(recordBytes)
                3 -> decompressDeflateOrGzip(recordBytes)
                17480 -> throw IllegalArgumentException("该MOBI使用HUFF/CDIC压缩，暂未支持")
                else -> throw IllegalArgumentException("暂不支持该MOBI压缩方式: $compression")
            }
            out.write(decompressed)
            if (textLength > 0 && out.size() >= textLength) break
        }

        val rawTextBytes = if (textLength > 0) out.toByteArray().copyOfRange(0, min(out.size(), textLength)) else out.toByteArray()
        onProgress?.invoke(0.90f, "正在解码文本...")
        return runCatching { String(rawTextBytes, charset) }.getOrElse { String(rawTextBytes, Charsets.UTF_8) }
    }

    private fun toPlainText(raw: String): String {
        if (raw.isBlank()) return ""
        val normalized = preprocessHtmlForText(raw)
            .replace("\u0000", "")
            .replace("\r\n", "\n")
            .replace("\r", "\n")

        val asText = HtmlCompat.fromHtml(normalized, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
        return asText
            .replace(Regex("[ \\t\\u00A0\\u200B]+"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }

    private fun preprocessHtmlForText(raw: String): String {
        if (raw.isBlank()) return ""
        var s = raw
        s = s.replace(Regex("(?i)<\\s*br\\s*/?\\s*>"), "\n")
        s = s.replace(Regex("(?i)</\\s*p\\s*>"), "\n")
        s = s.replace(Regex("(?i)</\\s*div\\s*>"), "\n")
        s = s.replace(Regex("(?i)</\\s*li\\s*>"), "\n")
        s = s.replace(Regex("(?i)</\\s*h[1-6]\\s*>"), "\n")
        s = s.replace(Regex("(?i)<\\s*mbp:pagebreak\\s*/?\\s*>"), "\n")
        return s
    }

    private fun splitHtmlByHeadings(raw: String): List<MobiChapter> {
        if (!raw.contains('<')) return emptyList()
        val doc = Jsoup.parse(raw)
        var container: Element = doc.body() ?: return emptyList()
        while (container.childrenSize() == 1) {
            val only = container.child(0)
            if (only.tagName().equals("div", ignoreCase = true) ||
                only.tagName().equals("body", ignoreCase = true)
            ) {
                container = only
            } else {
                break
            }
        }

        val blocks = container.children()
        if (blocks.isEmpty()) return emptyList()

        val chapters = mutableListOf<MobiChapter>()
        var currentTitle: String? = null
        val currentHtml = StringBuilder()

        fun flush() {
            val html = currentHtml.toString()
            val text = toPlainText(html)
            if (text.isNotBlank()) {
                val title = (currentTitle ?: "正文").trim().ifBlank { "正文" }
                chapters.add(MobiChapter(title = title, content = text, wordCount = text.length))
            }
            currentHtml.setLength(0)
        }

        for (el in blocks) {
            val tag = el.tagName().lowercase()
            if (tag == "h1" || tag == "h2" || tag == "h3") {
                if (currentTitle != null || currentHtml.isNotEmpty()) {
                    flush()
                }
                val t = el.text().trim()
                currentTitle = if (t.isNotBlank()) t else "正文"
            } else {
                currentHtml.append(el.outerHtml())
            }
        }
        if (currentTitle != null || currentHtml.isNotEmpty()) {
            flush()
        }

        return chapters.filter { it.content.isNotBlank() }
    }

    private fun recordEnd(recordOffsets: IntArray, index: Int, fileSize: Int): Int {
        val next = if (index + 1 < recordOffsets.size) recordOffsets[index + 1] else fileSize
        return next.coerceIn(0, fileSize)
    }

    private fun readU16BE(bytes: ByteArray, offset: Int): Int {
        return ((bytes[offset].toInt() and 0xFF) shl 8) or (bytes[offset + 1].toInt() and 0xFF)
    }

    private fun readU32BE(bytes: ByteArray, offset: Int): Long {
        return ((bytes[offset].toLong() and 0xFF) shl 24) or
            ((bytes[offset + 1].toLong() and 0xFF) shl 16) or
            ((bytes[offset + 2].toLong() and 0xFF) shl 8) or
            (bytes[offset + 3].toLong() and 0xFF)
    }

    private fun palmdocDecompress(input: ByteArray): ByteArray {
        val out = ByteAccumulator(input.size * 2)
        var i = 0
        while (i < input.size) {
            val c = input[i].toInt() and 0xFF
            when {
                c in 0x01..0x08 -> {
                    val count = c
                    i++
                    if (i + count > input.size) break
                    out.write(input, i, count)
                    i += count
                }
                c in 0x09..0x7F -> {
                    out.write(c)
                    i++
                }
                c in 0x80..0xBF -> {
                    if (i + 1 >= input.size) break
                    val c2 = input[i + 1].toInt() and 0xFF
                    val pair = (c shl 8) or c2
                    val distance = (pair and 0x3FFF) shr 3
                    val length = (pair and 0x07) + 3
                    val start = out.size - distance
                    if (start < 0) {
                        i += 2
                        continue
                    }
                    for (k in 0 until length) {
                        out.write(out.get(start + (k % distance)).toInt() and 0xFF)
                    }
                    i += 2
                }
                c in 0xC0..0xFF -> {
                    out.write(0x20)
                    out.write(c xor 0x80)
                    i++
                }
            }
        }
        return out.toByteArray()
    }

    private fun decompressDeflateOrGzip(input: ByteArray): ByteArray {
        if (input.size >= 2) {
            val b0 = input[0].toInt() and 0xFF
            val b1 = input[1].toInt() and 0xFF
            if (b0 == 0x1F && b1 == 0x8B) {
                return gunzip(input)
            }
        }

        inflateFlexible(input)?.let { return it }
        throw IllegalArgumentException("无法解压该MOBI数据块")
    }

    private fun inflateFlexible(input: ByteArray): ByteArray? {
        return try {
            inflateWithInflater(input, nowrap = false)
        } catch (_: Exception) {
            try {
                inflateWithInflater(input, nowrap = true)
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun inflateWithInflater(input: ByteArray, nowrap: Boolean): ByteArray {
        val inflater = Inflater(nowrap)
        try {
            inflater.setInput(input)
            val out = ByteArrayOutputStream(min(input.size * 4, 2_000_000))
            val buffer = ByteArray(8192)
            var total = 0
            while (!inflater.finished() && !inflater.needsInput()) {
                val n = try {
                    inflater.inflate(buffer)
                } catch (e: DataFormatException) {
                    throw e
                }
                if (n <= 0) break
                out.write(buffer, 0, n)
                total += n
                if (total > 10_000_000) {
                    throw IOException("解压输出过大")
                }
            }
            return out.toByteArray()
        } finally {
            inflater.end()
        }
    }

    private fun gunzip(input: ByteArray): ByteArray {
        return GZIPInputStream(input.inputStream()).use { it.readBytes() }
    }

    private class ByteAccumulator(initialCapacity: Int) {
        private var buf: ByteArray = ByteArray(initialCapacity.coerceAtLeast(16))
        var size: Int = 0
            private set

        fun write(b: Int) {
            ensureCapacity(size + 1)
            buf[size] = b.toByte()
            size++
        }

        fun write(src: ByteArray, offset: Int, length: Int) {
            if (length <= 0) return
            ensureCapacity(size + length)
            src.copyInto(buf, destinationOffset = size, startIndex = offset, endIndex = offset + length)
            size += length
        }

        fun get(index: Int): Byte = buf[index]

        fun toByteArray(): ByteArray = buf.copyOf(size)

        private fun ensureCapacity(target: Int) {
            if (target <= buf.size) return
            var newSize = buf.size
            while (newSize < target) newSize = newSize * 2
            buf = buf.copyOf(newSize)
        }
    }
}

