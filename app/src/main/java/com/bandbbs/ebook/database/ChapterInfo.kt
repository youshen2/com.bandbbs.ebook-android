package com.bandbbs.ebook.database

import kotlinx.serialization.Serializable

/**
 * 轻量级章节信息类，不包含章节内容
 * 用于避免查询大量章节时CursorWindow溢出问题
 */
@Serializable
data class ChapterInfo(
    val id: Int,
    val bookId: Int,
    val index: Int,
    val name: String,
    val wordCount: Int = 0
)

