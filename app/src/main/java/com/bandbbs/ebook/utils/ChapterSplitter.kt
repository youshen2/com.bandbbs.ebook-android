package com.bandbbs.ebook.utils

import android.content.Context
import android.net.Uri
import com.bandbbs.ebook.database.Chapter
import org.mozilla.universalchardet.UniversalDetector
import java.nio.charset.Charset

object ChapterSplitter {

    const val METHOD_DEFAULT = "METHOD_DEFAULT"
    const val METHOD_DEFAULT_LOOSE = "METHOD_DEFAULT_LOOSE"
    const val METHOD_CHAPTER = "METHOD_CHAPTER"
    const val METHOD_ZH_NUM_DOT = "METHOD_ZH_NUM_DOT"
    const val METHOD_DIGIT_DOT = "METHOD_DIGIT_DOT"

    val methods = mapOf(
        METHOD_DEFAULT to "默认 (第X章)",
        METHOD_DEFAULT_LOOSE to "默认-宽松 (…第X章…)",
        METHOD_CHAPTER to "英文 (Chapter X)",
        METHOD_ZH_NUM_DOT to "中文数字 (一、 二.)",
        METHOD_DIGIT_DOT to "阿拉伯数字 (1. 2、)"
    )

    private val regexDefault =
        Regex("""^\s*第(\s*[一二三四五六七八九十百千万零〇\d]+\s*)(章|卷|节|部|篇)(.*)$""")
    private val regexDefaultLoose =
        Regex("""^\s*(.{0,10})第(\s*[一二三四五六七八九十百千万零〇\d]+\s*)(章|卷|节|部|篇)(.{0,30})$""")
    private val regexChapter = Regex("""^\s*(Chapter|CHAPTER)\s+(\d+)\s*.*$""")
    private val regexZhNumDot = Regex("""^\s*([一二三四五六七八九十百千万零〇]+)[、.\s]+(.*)$""")
    private val regexDigitDot = Regex("""^\s*(\d+)[、.\s]+(.*)$""")

    fun split(
        context: Context,
        uri: Uri,
        bookId: Int,
        method: String,
        onProgress: (progress: Float, status: String) -> Unit
    ): List<Chapter> {
        val regex = when (method) {
            METHOD_CHAPTER -> regexChapter
            METHOD_DEFAULT_LOOSE -> regexDefaultLoose
            METHOD_ZH_NUM_DOT -> regexZhNumDot
            METHOD_DIGIT_DOT -> regexDigitDot
            else -> regexDefault
        }

        onProgress(0f, "正在读取文件...")
        val content = readTextFromUri(context, uri)
        onProgress(0.1f, "正在分行...")
        val lines = content.lines()
        val totalLines = lines.size

        val chapters = mutableListOf<Chapter>()
        var currentChapterTitle: String? = null
        var currentChapterContent = StringBuilder()
        var chapterIndex = 0

        lines.forEachIndexed { lineIndex, line ->
            if (lineIndex % 500 == 0) {
                onProgress(
                    0.1f + 0.8f * (lineIndex.toFloat() / totalLines),
                    "正在分析章节: ${lineIndex}/${totalLines}"
                )
            }

            if (regex.matches(line.trim())) {
                if (currentChapterTitle != null) {
                    val chapterContent = currentChapterContent.toString().trim()
                    chapters.add(
                        Chapter(
                            bookId = bookId,
                            index = chapterIndex++,
                            name = currentChapterTitle.trim(),
                            content = chapterContent,
                            wordCount = chapterContent.length
                        )
                    )
                } else if (currentChapterContent.toString().trim().isNotEmpty()) {
                    val prologueContent = currentChapterContent.toString().trim()
                    chapters.add(
                        Chapter(
                            bookId = bookId,
                            index = chapterIndex++,
                            name = "前言",
                            content = prologueContent,
                            wordCount = prologueContent.length
                        )
                    )
                }
                currentChapterTitle = line
                currentChapterContent = StringBuilder()
            } else {
                currentChapterContent.append(line).append("\n")
            }
        }

        if (currentChapterTitle != null) {
            val chapterContent = currentChapterContent.toString().trim()
            chapters.add(
                Chapter(
                    bookId = bookId,
                    index = chapterIndex,
                    name = currentChapterTitle.trim(),
                    content = chapterContent,
                    wordCount = chapterContent.length
                )
            )
        } else if (chapters.isEmpty() && currentChapterContent.toString().trim().isNotEmpty()) {
            val fullContent = currentChapterContent.toString().trim()
            chapters.add(
                Chapter(
                    bookId = bookId,
                    index = chapterIndex,
                    name = "全文",
                    content = fullContent,
                    wordCount = fullContent.length
                )
            )
        }

        onProgress(0.95f, "分章完成, 共 ${chapters.size} 章")
        return chapters
    }

    fun readTextFromUri(context: Context, uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return ""
        val bytes = inputStream.readBytes()
        inputStream.close()

        val detector = UniversalDetector(null)
        detector.handleData(bytes, 0, bytes.size)
        detector.dataEnd()
        val charset = detector.detectedCharset ?: "UTF-8"
        detector.reset()

        return try {
            bytes.toString(Charset.forName(charset))
        } catch (e: Exception) {
            bytes.toString(Charset.defaultCharset())
        }
    }
}
