package com.bandbbs.ebook.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    var bookmarkToEdit by remember { mutableStateOf<BookmarkEntity?>(null) }
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
            .padding(16.dp)
    ) {
        Text(
            text = "书签列表",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (bookmarks.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "暂无书签",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        onDelete = { bookmarkToDelete = bookmark },
                        onEdit = { bookmarkToEdit = bookmark }
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
                                onClick = { if (currentPage > 0) currentPage-- },
                                enabled = currentPage > 0
                            ) {
                                Text("上一页")
                            }
                            Text(
                                text = "${currentPage + 1} / $totalPages",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            TextButton(
                                onClick = { if (currentPage < totalPages - 1) currentPage++ },
                                enabled = currentPage < totalPages - 1
                            ) {
                                Text("下一页")
                            }
                        }
                    }
                }
            }
        }
    }

    // 编辑对话框
    bookmarkToEdit?.let { bookmark ->
        EditBookmarkDialog(
            bookmark = bookmark,
            onDismiss = { bookmarkToEdit = null },
            onConfirm = { newName ->
                onEditBookmark(bookmark.copy(name = newName))
                bookmarkToEdit = null
            }
        )
    }

    // 删除确认对话框
    bookmarkToDelete?.let { bookmark ->
        AlertDialog(
            onDismissRequest = { bookmarkToDelete = null },
            title = { Text("删除书签") },
            text = { Text("确定要删除书签「${bookmark.name}」吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteBookmark(bookmark)
                        bookmarkToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { bookmarkToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun BookmarkItem(
    bookmark: BookmarkEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = bookmark.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = bookmark.chapterName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = formatBookmarkTime(bookmark.time),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Box {
                FilledTonalIconButton(
                    onClick = { showMenu = true },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "更多",
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("编辑") },
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("删除") },
                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        colors = androidx.compose.material3.MenuDefaults.itemColors(
                            textColor = MaterialTheme.colorScheme.error,
                            leadingIconColor = MaterialTheme.colorScheme.error
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun EditBookmarkDialog(
    bookmark: BookmarkEntity,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var bookmarkName by remember { mutableStateOf(bookmark.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑书签") },
        text = {
            Column {
                OutlinedTextField(
                    value = bookmarkName,
                    onValueChange = { 
                        if (it.length <= 20) {
                            bookmarkName = it
                        }
                    },
                    label = { Text("书签名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${bookmarkName.length} / 20",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (bookmarkName.isNotBlank()) {
                        onConfirm(bookmarkName)
                    }
                },
                enabled = bookmarkName.isNotBlank()
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

private fun formatBookmarkTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

