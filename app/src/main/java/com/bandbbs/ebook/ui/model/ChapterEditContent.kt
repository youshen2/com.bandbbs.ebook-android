package com.bandbbs.ebook.ui.model

/**
 * 章节内容编辑态
 */
data class ChapterEditContent(
    val id: Int,
    val bookId: Int,
    val index: Int,
    val title: String,
    val content: String
)


