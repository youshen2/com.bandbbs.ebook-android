package com.bandbbs.ebook.ui.components.cell

import androidx.compose.runtime.Composable
import com.bandbbs.ebook.database.ChapterInfo
import top.yukonga.miuix.kmp.extra.SuperArrow

@Composable
fun ChapterItem(
    chapter: ChapterInfo,
    onClick: () -> Unit
) {
    SuperArrow(
        title = chapter.name,
        summary = "${chapter.wordCount} 字",
        onClick = onClick
    )
}
