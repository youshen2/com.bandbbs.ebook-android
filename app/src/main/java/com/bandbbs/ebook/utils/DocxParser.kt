package com.bandbbs.ebook.utils

import android.content.Context
import android.net.Uri
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.util.zip.ZipInputStream

object DocxParser {

    fun isDocxFile(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zip ->
                    var entry = zip.nextEntry
                    var checked = 0
                    while (entry != null && checked < 50) {
                        if (!entry.isDirectory && entry.name == "word/document.xml") {
                            return true
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                        checked++
                    }
                }
            }
            false
        } catch (_: Exception) {
            false
        }
    }

    fun extractPlainText(context: Context, uri: Uri): String {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            return extractPlainTextFromInputStream(inputStream)
        } ?: throw IllegalArgumentException("无法打开文件")
    }

    private fun extractPlainTextFromInputStream(inputStream: InputStream): String {
        val documentXml = readEntryBytes(inputStream, "word/document.xml")
            ?: throw IllegalArgumentException("无效的 DOCX 文件")

        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(documentXml.inputStream(), "UTF-8")

        val out = StringBuilder()
        var inParagraph = false
        var paragraph = StringBuilder()

        fun flushParagraph() {
            val p = paragraph.toString()
                .replace('\u00A0', ' ')
                .trim()
            if (p.isNotEmpty()) {
                if (out.isNotEmpty() && !out.endsWith("\n\n")) out.append("\n\n")
                out.append(p)
            }
            paragraph = StringBuilder()
        }

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "w:p", "p" -> {
                            inParagraph = true
                            paragraph = StringBuilder()
                        }

                        "w:t", "t", "w:instrText", "instrText" -> {
                            parser.next()
                            if (parser.eventType == XmlPullParser.TEXT) {
                                paragraph.append(parser.text)
                            }
                        }

                        "w:tab", "tab" -> paragraph.append('\t')
                        "w:br", "br", "w:cr", "cr" -> paragraph.append('\n')
                    }
                }

                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "w:p", "p" -> {
                            if (inParagraph) {
                                flushParagraph()
                                inParagraph = false
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        if (paragraph.isNotEmpty()) flushParagraph()

        return out.toString()
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }

    private fun readEntryBytes(inputStream: InputStream, targetName: String): ByteArray? {
        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name == targetName) {
                    return zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return null
    }
}

