package com.bandbbs.ebook.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bandbbs.ebook.ui.viewmodel.SyncOptionsState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncOptionsBottomSheet(
    state: SyncOptionsState,
    onCancel: () -> Unit,
    onConfirm: (selectedChapterIndices: Set<Int>, syncCover: Boolean) -> Unit,
    onResyncCoverOnly: (() -> Unit)? = null,
    onDeleteChapters: ((Set<Int>) -> Unit)? = null
) {
    var selectedChapters by remember { mutableStateOf(setOf<Int>()) }
    var syncCover by remember { mutableStateOf(true) }
    var quickSelectText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(state.syncedChapterIndices, state.totalChapters, state.chapters) {
        if (state.chapters.isNotEmpty() && selectedChapters.isEmpty()) {
            
            val firstUnsyncedIndex = state.chapters.firstOrNull { 
                it.index !in state.syncedChapterIndices 
            }?.index ?: state.totalChapters
            val endIndex = state.totalChapters
            selectedChapters = (firstUnsyncedIndex until endIndex).toSet()
        }
    }

    val selectedCount by remember { derivedStateOf { selectedChapters.size } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel) {
                Text("取消")
            }
            Text(
                "选择章节",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium
            )
            TextButton(
                onClick = {
                    if (selectedChapters.isNotEmpty()) {
                        onConfirm(selectedChapters, syncCover)
                    }
                },
                enabled = selectedChapters.isNotEmpty()
            ) {
                Text("同步")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (state.totalChapters == 0 && state.book.chapterCount > 0) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    "正在获取手环状态...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {

                    if (state.book.coverImagePath != null && File(state.book.coverImagePath).exists()) {
                        AsyncImage(
                            model = File(state.book.coverImagePath),
                            contentDescription = "书籍封面",
                            modifier = Modifier
                                .size(width = 60.dp, height = 80.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier
                                .size(width = 60.dp, height = 80.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(16.dp)
                                    .size(28.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = state.book.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "手环已有",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${state.syncedChapters} / ${state.totalChapters} 章",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "已选择",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$selectedCount 章",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        if (state.totalChapters > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { state.syncedChapters.toFloat() / state.totalChapters },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))


            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
            ) {

                if (state.hasCover && !state.isCoverSynced) {
                    item {
                        SyncCoverOption(
                            syncCover = syncCover,
                            onSyncCoverChanged = { syncCover = it }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }


                if (state.hasCover && state.isCoverSynced && onResyncCoverOnly != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "封面已同步",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "如需更新封面，可以重新同步",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                FilledTonalButton(
                                    onClick = onResyncCoverOnly,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Image,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("重新同步")
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }


                item {
                    BulkActionsCard(
                        state = state,
                        syncCover = syncCover,
                        quickSelectText = quickSelectText,
                        onSyncCoverChanged = { syncCover = it },
                        onQuickSelectTextChanged = { quickSelectText = it },
                        onQuickSelectConfirmed = {
                            val range = parseChapterRange(quickSelectText, state.totalChapters)
                            if (range != null) {
                                selectedChapters = range
                                quickSelectText = ""
                            }
                        },
                        onSelectAll = {
                            selectedChapters = (0 until state.totalChapters).toSet()
                        },
                        onSelectNone = { selectedChapters = emptySet() },
                        onSelectUnread = {
                            
                            val currentChapterIndex = state.book.chapterIndex
                            
                            val startIndex = if (currentChapterIndex != null && currentChapterIndex >= 0) {
                                currentChapterIndex.coerceIn(0, state.totalChapters - 1)
                            } else {
                                
                                state.chapters.firstOrNull { 
                                    it.index !in state.syncedChapterIndices 
                                }?.index ?: state.totalChapters
                            }
                            val endIndex = state.totalChapters
                            selectedChapters = (startIndex until endIndex).filter { it !in state.syncedChapterIndices }.toSet()
                        },
                        onSelectRead = {
                            val currentChapterIndex = state.book.chapterIndex
                            if (currentChapterIndex != null && currentChapterIndex >= 0) {
                                selectedChapters = (0 until currentChapterIndex).filter { it in state.syncedChapterIndices }.toSet()
                            } else {
                                selectedChapters = emptySet()
                            }
                        },
                        onDeleteSelected = onDeleteChapters?.let { deleteFunc ->
                            {
                                if (selectedChapters.isNotEmpty()) {
                                    deleteFunc(selectedChapters)
                                }
                            }
                        },
                        selectedCount = selectedCount,
                        onResyncCoverOnly = onResyncCoverOnly
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }



                item {
                    ChapterListHeader()
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }


                itemsIndexed(
                    items = state.chapters,
                    key = { _, chapter -> chapter.id }
                ) { index, chapter ->

                    val isFirst = index == 0
                    val isLast = index == state.chapters.size - 1
                    val shape = when {
                        isFirst && isLast -> RoundedCornerShape(16.dp)
                        isFirst -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                        isLast -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                        else -> RoundedCornerShape(0.dp)
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shape = shape
                    ) {
                        ChapterItem(
                            chapter = chapter,
                            index = index,
                            totalChapters = state.chapters.size,
                            syncedChapterIndices = state.syncedChapterIndices,
                            isSelected = selectedChapters.contains(chapter.index),
                            currentSelection = selectedChapters,
                            onSelectionChanged = { newSelection ->
                                selectedChapters = newSelection
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun ChapterListHeader() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = "章节列表",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ChapterItem(
    chapter: com.bandbbs.ebook.database.ChapterInfo,
    index: Int,
    totalChapters: Int,
    syncedChapterIndices: Set<Int>,
    isSelected: Boolean,
    currentSelection: Set<Int>,
    onSelectionChanged: (Set<Int>) -> Unit
) {
    val isSynced = chapter.index in syncedChapterIndices

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected || isSynced) {
                    Modifier.background(
                        when {
                            isSelected -> MaterialTheme.colorScheme.secondaryContainer
                            isSynced -> MaterialTheme.colorScheme.surfaceContainerHigh
                            else -> Color.Transparent
                        }
                    )
                } else {
                    Modifier
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onSelectionChanged(
                        if (isSelected) currentSelection - chapter.index
                        else currentSelection + chapter.index
                    )
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "第 ${chapter.index + 1} 章",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isSynced) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "已同步",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = chapter.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = "${chapter.wordCount} 字",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (index < totalChapters - 1) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .padding(start = 50.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )
        }
    }
}

@Composable
private fun BulkActionsCard(
    state: SyncOptionsState,
    syncCover: Boolean,
    quickSelectText: String,
    onSyncCoverChanged: (Boolean) -> Unit,
    onQuickSelectTextChanged: (String) -> Unit,
    onQuickSelectConfirmed: () -> Unit,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit,
    onSelectUnread: () -> Unit,
    onSelectRead: () -> Unit,
    onDeleteSelected: (() -> Unit)?,
    selectedCount: Int,
    onResyncCoverOnly: (() -> Unit)?
) {
    val focusManager = LocalFocusManager.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "批量操作",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Column {
                Text(
                    text = "按范围选择",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = quickSelectText,
                        onValueChange = { onQuickSelectTextChanged(it.filter { c -> c.isDigit() || c == '-' }) },
                        label = { Text("输入章节范围") },
                        placeholder = { Text("例如: 1-50") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                onQuickSelectConfirmed()
                            }
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    IconButton(
                        onClick = {
                            focusManager.clearFocus()
                            onQuickSelectConfirmed()
                        }
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = "应用范围")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(onClick = onSelectAll, modifier = Modifier.weight(1f)) {
                    Text("全选")
                }
                FilledTonalButton(onClick = onSelectNone, modifier = Modifier.weight(1f)) {
                    Text("清空")
                }
                FilledTonalButton(onClick = onSelectUnread, modifier = Modifier.weight(1f)) {
                    Text("未读")
                }
            }

            if (onSelectRead != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(onClick = onSelectRead, modifier = Modifier.weight(1f)) {
                        Text("已读")
                    }
                    if (onDeleteSelected != null && selectedCount > 0) {
                        androidx.compose.material3.Button(
                            onClick = onDeleteSelected,
                            modifier = Modifier.weight(1f),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("删除选中($selectedCount)")
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncCoverOption(
    syncCover: Boolean,
    onSyncCoverChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable { onSyncCoverChanged(!syncCover) }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "同步封面图片",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "将书籍封面一起传输到手环",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Checkbox(checked = syncCover, onCheckedChange = onSyncCoverChanged)
    }
}

private fun parseChapterRange(text: String, maxChapters: Int): Set<Int>? {
    if (text.isBlank()) return null

    return try {
        if (text.contains("-")) {
            val parts = text.split("-")
            if (parts.size != 2) return null

            val start = parts[0].trim().toIntOrNull() ?: return null
            val end = parts[1].trim().toIntOrNull() ?: return null

            if (start < 1 || end < 1 || start > end) return null

            val adjustedStart = (start - 1).coerceIn(0, maxChapters - 1)
            val adjustedEnd = end.coerceAtMost(maxChapters)

            (adjustedStart until adjustedEnd).toSet()
        } else {
            val chapter = text.toIntOrNull() ?: return null
            if (chapter < 1 || chapter > maxChapters) return null

            setOf(chapter - 1)
        }
    } catch (e: Exception) {
        null
    }
}
