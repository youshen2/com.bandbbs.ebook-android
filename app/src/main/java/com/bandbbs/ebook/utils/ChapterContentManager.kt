package com.bandbbs.ebook.utils

import android.content.Context
import java.io.File

/**
 * 章节内容管理器
 * 负责章节内容的文件存储和读取,避免数据库CursorWindow 2MB限制
 */
object ChapterContentManager {
    
    /**
     * 获取章节内容存储目录
     */
    fun getChaptersDir(context: Context): File {
        return File(context.filesDir, "chapters").apply { mkdirs() }
    }
    
    /**
     * 保存章节内容到文件
     * @param context 上下文
     * @param bookId 书籍ID
     * @param chapterIndex 章节索引
     * @param content 章节内容
     * @return 章节内容文件路径
     */
    fun saveChapterContent(context: Context, bookId: Int, chapterIndex: Int, content: String): String {
        val chaptersDir = getChaptersDir(context)
        val bookDir = File(chaptersDir, "book_$bookId").apply { mkdirs() }
        val chapterFile = File(bookDir, "chapter_$chapterIndex.txt")
        chapterFile.writeText(content)
        return chapterFile.absolutePath
    }
    
    /**
     * 读取章节内容
     * @param filePath 章节内容文件路径
     * @return 章节内容,如果文件不存在返回空字符串
     */
    fun readChapterContent(filePath: String): String {
        val file = File(filePath)
        return if (file.exists()) {
            file.readText()
        } else {
            ""
        }
    }
    
    /**
     * 删除书籍的所有章节内容文件
     * @param context 上下文
     * @param bookId 书籍ID
     */
    fun deleteBookChapters(context: Context, bookId: Int) {
        val chaptersDir = getChaptersDir(context)
        val bookDir = File(chaptersDir, "book_$bookId")
        if (bookDir.exists()) {
            bookDir.deleteRecursively()
        }
    }
    
    /**
     * 删除单个章节内容文件
     * @param filePath 章节内容文件路径
     */
    fun deleteChapterContent(filePath: String) {
        val file = File(filePath)
        if (file.exists()) {
            file.delete()
        }
    }
}

