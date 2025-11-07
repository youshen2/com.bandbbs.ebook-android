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
    const val METHOD_BY_WORD_COUNT = "METHOD_BY_WORD_COUNT"

    val methods = mapOf(
        METHOD_DEFAULT to "默认 (第X章/卷/节/部/篇/回/番外…)",
        METHOD_DEFAULT_LOOSE to "默认-宽松 (…X章/卷/节/部/篇/回…/番外…)",
        METHOD_CHAPTER to "英文 (Chapter X)",
        METHOD_ZH_NUM_DOT to "中文数字 (一、 二.)",
        METHOD_DIGIT_DOT to "阿拉伯数字 (1. 2、)",
        METHOD_BY_WORD_COUNT to "按字数分章"
    )

    private val regexDefault =
        Regex("""^(第(\s{0,1}[一二三四五六七八九十百千万零〇\d]+\s{0,1})(章|卷|节|部|篇|回|本)|番外\s{0,2}[一二三四五六七八九十百千万零〇\d]*)(.{0,30})$""")
    private val regexDefaultLoose =
        Regex("""^(\s*[第]?(\s*[一二三四五六七八九十百千万零〇\d]+\s*)(章|卷|节|部|篇|回|本)|番外\s*[一二三四五六七八九十百千万零〇\d]*)(.{0,30})$""")
    private val regexChapter = Regex("""^\s*(Chapter|CHAPTER)\s+(\d+)\s*.*$""")
    private val regexZhNumDot = Regex("""^\s*([一二三四五六七八九十百千万零〇]+)[、.\s]+(.*)$""")
    private val regexDigitDot = Regex("""^\s*(\d+)[、.\s]+(.*)$""")

    fun split(
        context: Context,
        uri: Uri,
        bookId: Int,
        method: String,
        onProgress: (progress: Float, status: String) -> Unit,
        wordsPerChapter: Int = 5000
    ): List<Chapter> {
        
        if (method == METHOD_BY_WORD_COUNT) {
            return splitByWordCount(context, uri, bookId, onProgress, wordsPerChapter)
        }
        
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
                    val contentFilePath = ChapterContentManager.saveChapterContent(
                        context, bookId, chapterIndex, chapterContent
                    )
                    chapters.add(
                        Chapter(
                            bookId = bookId,
                            index = chapterIndex++,
                            name = currentChapterTitle.trim(),
                            contentFilePath = contentFilePath,
                            wordCount = chapterContent.length
                        )
                    )
                } else if (currentChapterContent.toString().trim().isNotEmpty()) {
                    val prologueContent = currentChapterContent.toString().trim()
                    val contentFilePath = ChapterContentManager.saveChapterContent(
                        context, bookId, chapterIndex, prologueContent
                    )
                    chapters.add(
                        Chapter(
                            bookId = bookId,
                            index = chapterIndex++,
                            name = "前言",
                            contentFilePath = contentFilePath,
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
            val contentFilePath = ChapterContentManager.saveChapterContent(
                context, bookId, chapterIndex, chapterContent
            )
            chapters.add(
                Chapter(
                    bookId = bookId,
                    index = chapterIndex,
                    name = currentChapterTitle.trim(),
                    contentFilePath = contentFilePath,
                    wordCount = chapterContent.length
                )
            )
        } else if (chapters.isEmpty() && currentChapterContent.toString().trim().isNotEmpty()) {
            val fullContent = currentChapterContent.toString().trim()
            val contentFilePath = ChapterContentManager.saveChapterContent(
                context, bookId, chapterIndex, fullContent
            )
            chapters.add(
                Chapter(
                    bookId = bookId,
                    index = chapterIndex,
                    name = "全文",
                    contentFilePath = contentFilePath,
                    wordCount = fullContent.length
                )
            )
        }

        onProgress(0.95f, "分章完成, 共 ${chapters.size} 章")
        return chapters
    }
    
    private fun splitByWordCount(
        context: Context,
        uri: Uri,
        bookId: Int,
        onProgress: (progress: Float, status: String) -> Unit,
        wordsPerChapter: Int
    ): List<Chapter> {
        onProgress(0f, "正在读取文件...")
        val content = readTextFromUri(context, uri)
        onProgress(0.2f, "正在按字数分章...")
        
        val chapters = mutableListOf<Chapter>()
        val totalLength = content.length
        var chapterIndex = 0
        var startIndex = 0
        
        while (startIndex < totalLength) {
            val endIndex = (startIndex + wordsPerChapter).coerceAtMost(totalLength)
            val chapterContent = content.substring(startIndex, endIndex).trim()
            
            if (chapterContent.isNotEmpty()) {
                val contentFilePath = ChapterContentManager.saveChapterContent(
                    context, bookId, chapterIndex, chapterContent
                )
                chapters.add(
                    Chapter(
                        bookId = bookId,
                        index = chapterIndex,
                        name = "第 ${chapterIndex + 1} 章",
                        contentFilePath = contentFilePath,
                        wordCount = chapterContent.length
                    )
                )
                chapterIndex++
            }
            
            startIndex = endIndex
            
            onProgress(
                0.2f + 0.75f * (startIndex.toFloat() / totalLength),
                "正在分章: ${chapterIndex} 章"
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
