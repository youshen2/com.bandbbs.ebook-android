package com.bandbbs.ebook.ui.model

/**
 * 章节包含内容的数据模型，用于预览显示
 */
data class ChapterWithContent(
    val id: Int,
    val name: String,
    val content: String,
    val wordCount: Int
)

