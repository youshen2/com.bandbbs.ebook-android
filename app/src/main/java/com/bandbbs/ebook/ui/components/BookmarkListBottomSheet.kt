package com.bandbbs.ebook.ui.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bandbbs.ebook.database.BookmarkEntity
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.extra.SuperListPopup
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BookmarkListBottomSheet(
    bookmarks: List<BookmarkEntity>,
    onBookmarkClick: (BookmarkEntity) -> Unit,
    onDeleteBookmark: (BookmarkEntity) -> Unit,
    onEditBookmark: (BookmarkEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val PAGE_SIZE = 8
    var currentPage by remember { mutableIntStateOf(0) }

    val showEditDialog = remember { mutableStateOf(false) }
    var bookmarkToEdit by remember { mutableStateOf<BookmarkEntity?>(null) }

    val showDeleteDialog = remember { mutableStateOf(false) }
    var bookmarkToDelete by remember { mutableStateOf<BookmarkEntity?>(null) }

    val totalPages = (bookmarks.size + PAGE_SIZE - 1) / PAGE_SIZE.coerceAtLeast(1)
    val visibleBookmarks = remember(bookmarks, currentPage) {
        val start = currentPage * PAGE_SIZE
        val end = (start + PAGE_SIZE).coerceAtMost(bookmarks.size)
        bookmarks.subList(start, end)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        if (bookmarks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无书签",
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantActions
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(visibleBookmarks) { bookmark ->
                    BookmarkItem(
                        bookmark = bookmark,
                        onClick = { onBookmarkClick(bookmark) },
                        onDelete = {
                            bookmarkToDelete = bookmark
                            showDeleteDialog.value = true
                        },
                        onEdit = {
                            bookmarkToEdit = bookmark
                            showEditDialog.value = true
                        }
                    )
                }

                if (totalPages > 1) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                text = "上一页",
                                onClick = { if (currentPage > 0) currentPage-- },
                                enabled = currentPage > 0
                            )
                            Text(
                                text = "${currentPage + 1} / $totalPages",
                                style = MiuixTheme.textStyles.body2
                            )
                            TextButton(
                                text = "下一页",
                                onClick = { if (currentPage < totalPages - 1) currentPage++ },
                                enabled = currentPage < totalPages - 1
                            )
                        }
                    }
                }
            }
        }
    }

    bookmarkToEdit?.let { bookmark ->
        EditBookmarkDialog(
            bookmark = bookmark,
            showDialog = showEditDialog,
            onConfirm = { newName ->
                onEditBookmark(bookmark.copy(name = newName))
                showEditDialog.value = false
            }
        )
    }

    bookmarkToDelete?.let { bookmark ->
        SuperDialog(
            title = "删除书签",
            summary = "确定要删除书签「${bookmark.name}」吗？",
            show = showDeleteDialog,
            onDismissRequest = { showDeleteDialog.value = false }
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    text = "取消",
                    onClick = { showDeleteDialog.value = false },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(16.dp))
                TextButton(
                    text = "删除",
                    onClick = {
                        onDeleteBookmark(bookmark)
                        showDeleteDialog.value = false
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary()
                )
            }
        }
    }
}

@Composable
private fun BookmarkItem(
    bookmark: BookmarkEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val showMenu = remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(16.dp),
        cornerRadius = 16.dp,
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.secondaryVariant
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = bookmark.name,
                    style = MiuixTheme.textStyles.headline1,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MiuixTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = bookmark.chapterName,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatBookmarkTime(bookmark.time),
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantActions
                )
            }

            Box {
                IconButton(
                    onClick = { showMenu.value = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = MiuixIcons.More,
                        contentDescription = "更多",
                        tint = MiuixTheme.colorScheme.onSurface
                    )
                }

                SuperListPopup(
                    show = showMenu,
                    alignment = PopupPositionProvider.Align.End,
                    onDismissRequest = { showMenu.value = false }
                ) {
                    ListPopupColumn {
                        DropdownImpl(
                            text = "编辑",
                            optionSize = 2,
                            isSelected = false,
                            index = 0,
                            onSelectedIndexChange = {
                                showMenu.value = false
                                onEdit()
                            }
                        )
                        DropdownImpl(
                            text = "删除",
                            optionSize = 2,
                            isSelected = false,
                            index = 1,
                            onSelectedIndexChange = {
                                showMenu.value = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditBookmarkDialog(
    bookmark: BookmarkEntity,
    showDialog: androidx.compose.runtime.MutableState<Boolean>,
    onConfirm: (String) -> Unit
) {
    var bookmarkName by remember { mutableStateOf(bookmark.name) }

    SuperDialog(
        title = "编辑书签",
        show = showDialog,
        onDismissRequest = { showDialog.value = false }
    ) {
        Column {
            TextField(
                value = bookmarkName,
                onValueChange = {
                    if (it.length <= 20) {
                        bookmarkName = it
                    }
                },
                label = "书签名称",
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${bookmarkName.length} / 20",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(start = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    text = "取消",
                    onClick = { showDialog.value = false },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(16.dp))
                TextButton(
                    text = "确定",
                    onClick = {
                        if (bookmarkName.isNotBlank()) {
                            onConfirm(bookmarkName)
                        }
                    },
                    enabled = bookmarkName.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary()
                )
            }
        }
    }
}

private fun formatBookmarkTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
