package com.bandbbs.ebook.utils

import android.content.Context
import android.net.Uri
import com.bandbbs.ebook.database.Chapter
import org.mozilla.universalchardet.UniversalDetector
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset

object ChapterSplitter {

    const val METHOD_DEFAULT = "METHOD_DEFAULT"
    const val METHOD_CHAPTER = "METHOD_CHAPTER"

    val methods = mapOf(
        METHOD_DEFAULT to "默认 (第X章/卷)",
        METHOD_CHAPTER to "英文 (Chapter X)"
    )

    private val regexDefault = Regex("""^\s*(.{0,10})第(\s*[一二三四五六七八九十百千万零〇\d]+\s*)(章|卷|节|部|篇)(.{0,30})$""")
    private val regexChapter = Regex("""^\s*(Chapter|CHAPTER)\s+(\d+)\s*.*$""")

    fun split(context: Context, uri: Uri, bookId: Int, method: String): List<Chapter> {
        val regex = when (method) {
            METHOD_CHAPTER -> regexChapter
            else -> regexDefault
        }

        val content = readTextFromUri(context, uri)
        val lines = content.lines()

        val chapters = mutableListOf<Chapter>()
        var currentChapterTitle = "前言"
        var currentChapterContent = StringBuilder()
        var chapterIndex = 0

        lines.forEach { line ->
            if (regex.matches(line.trim())) {
                if (currentChapterContent.isNotEmpty() || chapters.isEmpty()) {
                    chapters.add(
                        Chapter(
                            bookId = bookId,
                            index = chapterIndex++,
                            name = currentChapterTitle.trim(),
                            content = currentChapterContent.toString().trim(),
                            wordCount = currentChapterContent.toString().trim().length
                        )
                    )
                }
                currentChapterTitle = line
                currentChapterContent = StringBuilder()
            } else {
                currentChapterContent.append(line).append("\n")
            }
        }

        if (currentChapterContent.isNotEmpty()) {
            chapters.add(
                Chapter(
                    bookId = bookId,
                    index = chapterIndex,
                    name = currentChapterTitle.trim(),
                    content = currentChapterContent.toString().trim(),
                    wordCount = currentChapterContent.toString().trim().length
                )
            )
        }

        if (chapters.size == 1 && chapters[0].name == "前言") {
            chapters[0].name = "全文"
        }

        return chapters
    }

    private fun readTextFromUri(context: Context, uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return ""
        val bytes = inputStream.readBytes()
        inputStream.close()

        val detector = UniversalDetector(null)
        detector.handleData(bytes, 0, bytes.size)
        detector.dataEnd()
        val charset = detector.detectedCharset ?: "UTF-16LE"
        detector.reset()

        return bytes.toString(Charset.forName(charset))
    }
}
