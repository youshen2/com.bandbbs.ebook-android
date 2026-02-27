package com.bandbbs.ebook.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bandbbs.ebook.ui.model.Book
import com.bandbbs.ebook.utils.bytesToReadable
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.extra.SuperListPopup
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File

@Composable
fun BookItem(
    book: Book,
    onDeleteClick: () -> Unit,
    onSyncClick: () -> Unit,
    onChapterListClick: () -> Unit,
    onContinueReadingClick: () -> Unit,
    onImportCoverClick: () -> Unit = {},
    onEditInfoClick: () -> Unit = {},
    isSyncEnabled: Boolean,
    lastChapterName: String? = null,
    isSelected: Boolean = false,
    showSyncButton: Boolean = true
) {
    val showCoverDialog = remember { mutableStateOf(false) }
    val showDetailsDialog = remember { mutableStateOf(false) }
    val isPdf = book.format == "pdf"

    if (showCoverDialog.value && book.coverImagePath != null) {
        SuperDialog(
            title = "封面大图",
            show = showCoverDialog,
            onDismissRequest = { showCoverDialog.value = false }
        ) {
            AsyncImage(
                model = File(book.coverImagePath),
                contentDescription = "封面大图",
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.FillWidth
            )
        }
    }

    SuperDialog(
        title = book.name,
        show = showDetailsDialog,
        onDismissRequest = { showDetailsDialog.value = false }
    ) {
        Column(
            modifier = Modifier.padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BookStatItem(if (isPdf) "页数" else "章节", book.chapterCount.toString())
                if (!isPdf) {
                    BookStatItem("字数", book.wordCount.toString())
                }
                BookStatItem("大小", bytesToReadable(book.size))
            }

            if (book.localCategory != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.primaryContainer),
                    cornerRadius = 8.dp,
                    insideMargin = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = book.localCategory,
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            book.lastReadInfo?.let { info ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = info,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                    fontWeight = FontWeight.Medium
                )
            }

            if (book.chapterProgressPercent > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "阅读进度",
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantActions
                    )
                    Text(
                        text = "${String.format("%.1f", book.chapterProgressPercent)}%",
                        style = MiuixTheme.textStyles.body2,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (lastChapterName != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "最后一章",
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantActions
                    )
                    Text(
                        text = lastChapterName,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            showDetailsDialog.value = false
                            onContinueReadingClick()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColorsPrimary()
                    ) {
                        Icon(
                            MiuixIcons.Play,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MiuixTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "继续",
                            color = MiuixTheme.colorScheme.onPrimary
                        )
                    }

                    if (showSyncButton) {
                        Button(
                            onClick = {
                                showDetailsDialog.value = false
                                onSyncClick()
                            },
                            enabled = isSyncEnabled,
                            modifier = Modifier.weight(1.6f)
                        ) {
                            Icon(
                                MiuixIcons.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("传输书籍")
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            showDetailsDialog.value = false
                            onChapterListClick()
                        },
                        modifier = Modifier.weight(1.7f)
                    ) {
                        Icon(
                            MiuixIcons.ListView,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("目录")
                    }

                    val showMenu = remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { showMenu.value = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                MiuixIcons.More,
                                contentDescription = "更多",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("更多")
                        }
                        SuperListPopup(
                            show = showMenu,
                            onDismissRequest = { showMenu.value = false }
                        ) {
                            ListPopupColumn {
                                DropdownImpl(
                                    text = "编辑书籍信息",
                                    optionSize = 3,
                                    isSelected = false,
                                    onSelectedIndexChange = {
                                        showMenu.value = false
                                        showDetailsDialog.value = false
                                        onEditInfoClick()
                                    },
                                    index = 0
                                )
                                DropdownImpl(
                                    text = "更换封面",
                                    optionSize = 3,
                                    isSelected = false,
                                    onSelectedIndexChange = {
                                        showMenu.value = false
                                        showDetailsDialog.value = false
                                        onImportCoverClick()
                                    },
                                    index = 1
                                )
                                DropdownImpl(
                                    text = "删除书籍",
                                    optionSize = 3,
                                    isSelected = false,
                                    onSelectedIndexChange = {
                                        showMenu.value = false
                                        showDetailsDialog.value = false
                                        onDeleteClick()
                                    },
                                    index = 2
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    Card(
        onClick = { showDetailsDialog.value = true },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = if (isSelected) MiuixTheme.colorScheme.primaryContainer else MiuixTheme.colorScheme.surfaceContainer
        ),
        insideMargin = PaddingValues(16.dp)
    ) {
        Row {
            if (book.coverImagePath != null && File(book.coverImagePath).exists()) {
                AsyncImage(
                    model = File(book.coverImagePath),
                    contentDescription = "书籍封面",
                    modifier = Modifier
                        .size(width = 60.dp, height = 80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showCoverDialog.value = true },
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = book.name,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MiuixTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isPdf) "${book.chapterCount} 页" else "${book.chapterCount} 章",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantActions
                    )
                    Text(
                        text = bytesToReadable(book.size),
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantActions
                    )
                }

                if (book.localCategory != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "分类: ${book.localCategory}",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (book.chapterProgressPercent > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "进度: ${String.format("%.1f", book.chapterProgressPercent)}%",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantActions
                    )
                }
            }
        }
    }
}

@Composable
private fun BookStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantActions
        )
        Text(value, style = MiuixTheme.textStyles.body1)
    }
}
