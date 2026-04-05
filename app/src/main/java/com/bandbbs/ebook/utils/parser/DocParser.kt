package com.bandbbs.ebook.utils.parser

import android.content.Context
import android.net.Uri
import org.apache.poi.hwpf.extractor.WordExtractor

object DocParser {
    fun isDocFile(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val header = ByteArray(8)
                val read = inputStream.read(header)
                if (read < 8) return false
                val docMagic = byteArrayOf(
                    0xD0.toByte(), 0xCF.toByte(), 0x11.toByte(), 0xE0.toByte(),
                    0xA1.toByte(), 0xB1.toByte(), 0x1A.toByte(), 0xE1.toByte()
                )
                header.contentEquals(docMagic)
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    fun extractPlainText(context: Context, uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val extractor = WordExtractor(inputStream)
            extractor.text ?: ""
        } ?: throw IllegalArgumentException("无法打开文件")
    }
}
