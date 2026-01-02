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
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.vector.ImageVector
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

private enum class ChapterSortType {
    INDEX_ASC,
    INDEX_DESC,
    NAME_ASC,
    NAME_DESC,
    WORD_COUNT_ASC,
    WORD_COUNT_DESC
}

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
    var sortType by remember { mutableStateOf(ChapterSortType.INDEX_ASC) }
    var showSortMenu by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    fun sortChapters(chapters: List<com.bandbbs.ebook.database.ChapterInfo>): List<com.bandbbs.ebook.database.ChapterInfo> {
        return when (sortType) {
            ChapterSortType.INDEX_ASC -> chapters.sortedBy { it.index }
            ChapterSortType.INDEX_DESC -> chapters.sortedByDescending { it.index }
            ChapterSortType.NAME_ASC -> chapters.sortedBy { it.name }
            ChapterSortType.NAME_DESC -> chapters.sortedByDescending { it.name }
            ChapterSortType.WORD_COUNT_ASC -> chapters.sortedBy { it.wordCount }
            ChapterSortType.WORD_COUNT_DESC -> chapters.sortedByDescending { it.wordCount }
        }
    }

    val sortedChapters = remember(state.chapters, sortType) {
        sortChapters(state.chapters)
    }

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
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel) {
                Text("取消")
            }
            Text(
                "传输设置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(
                onClick = {
                    if (selectedChapters.isNotEmpty()) {
                        onConfirm(selectedChapters, syncCover)
                    }
                },
                enabled = selectedChapters.isNotEmpty()
            ) {
                Text("开始传输")
            }
        }

        if (state.totalChapters == 0 && state.book.chapterCount > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
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
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
            ) {

                item {
                    BookInfoCard(state, selectedCount)
                }


                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column {
                            if (state.hasCover && !state.isCoverSynced) {
                                ListItem(
                                    headlineContent = { Text("同步封面") },
                                    supportingContent = { Text("传输书籍封面图片") },
                                    leadingContent = {
                                        Icon(
                                            Icons.Default.Image,
                                            contentDescription = null
                                        )
                                    },
                                    trailingContent = {
                                        Switch(
                                            checked = syncCover,
                                            onCheckedChange = { syncCover = it }
                                        )
                                    },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    modifier = Modifier.clickable { syncCover = !syncCover }
                                )
                            } else if (state.hasCover && state.isCoverSynced && onResyncCoverOnly != null) {
                                ListItem(
                                    headlineContent = { Text("重新同步封面") },
                                    supportingContent = { Text("封面已存在，点击覆盖") },
                                    leadingContent = {
                                        Icon(
                                            Icons.Outlined.Image,
                                            contentDescription = null
                                        )
                                    },
                                    trailingContent = {
                                        Icon(
                                            Icons.Default.Sync,
                                            contentDescription = null
                                        )
                                    },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    modifier = Modifier.clickable { onResyncCoverOnly() }
                                )
                            } else {
                                ListItem(
                                    headlineContent = { Text("无封面") },
                                    leadingContent = {
                                        Icon(
                                            Icons.Outlined.Image,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.5f
                                            )
                                        )
                                    },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                )
                            }
                        }
                    }
                }


                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "批量选择",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = quickSelectText,
                                    onValueChange = {
                                        quickSelectText = it.filter { c -> c.isDigit() || c == '-' }
                                    },
                                    label = { Text("范围 (如 1-50)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            val range =
                                                parseChapterRange(quickSelectText, state.totalChapters)
                                            if (range != null) {
                                                selectedChapters = range
                                                quickSelectText = ""
                                                focusManager.clearFocus()
                                            }
                                        }
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                IconButton(
                                    onClick = {
                                        val range = parseChapterRange(quickSelectText, state.totalChapters)
                                        if (range != null) {
                                            selectedChapters = range
                                            quickSelectText = ""
                                            focusManager.clearFocus()
                                        }
                                    }
                                ) {
                                    Icon(Icons.Outlined.CheckCircle, contentDescription = "应用")
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                QuickActionButton(
                                    icon = Icons.Outlined.SelectAll,
                                    label = "全选",
                                    onClick = { selectedChapters = (0 until state.totalChapters).toSet() }
                                )
                                QuickActionButton(
                                    icon = Icons.Outlined.RadioButtonUnchecked,
                                    label = "清空",
                                    onClick = { selectedChapters = emptySet() }
                                )
                                QuickActionButton(
                                    icon = Icons.Outlined.VisibilityOff,
                                    label = "未读",
                                    onClick = {

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
                                    }
                                )
                                QuickActionButton(
                                    icon = Icons.Outlined.Visibility,
                                    label = "已读",
                                    onClick = {
                                        val currentChapterIndex = state.book.chapterIndex
                                        if (currentChapterIndex != null && currentChapterIndex >= 0) {
                                            selectedChapters = (0 until currentChapterIndex).filter { it in state.syncedChapterIndices }.toSet()
                                        } else {
                                            selectedChapters = emptySet()
                                        }
                                    }
                                )
                            }

                            if (onDeleteChapters != null && selectedCount > 0) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { onDeleteChapters(selectedChapters) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("删除手环上选中的章节")
                                }
                            }
                        }
                    }
                }


                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "章节列表",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Box {
                            Row(
                                modifier = Modifier.clickable { showSortMenu = true },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.Sort,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "排序",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("按索引升序") },
                                    onClick = {
                                        sortType = ChapterSortType.INDEX_ASC; showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("按索引降序") },
                                    onClick = {
                                        sortType = ChapterSortType.INDEX_DESC; showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("按名称升序") },
                                    onClick = {
                                        sortType = ChapterSortType.NAME_ASC; showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("按名称降序") },
                                    onClick = {
                                        sortType = ChapterSortType.NAME_DESC; showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("按字数升序") },
                                    onClick = {
                                        sortType = ChapterSortType.WORD_COUNT_ASC; showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("按字数降序") },
                                    onClick = {
                                        sortType = ChapterSortType.WORD_COUNT_DESC; showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                itemsIndexed(
                    items = sortedChapters,
                    key = { _, chapter -> chapter.id }
                ) { _, chapter ->
                    val isSelected = selectedChapters.contains(chapter.index)
                    val isSynced = chapter.index in state.syncedChapterIndices

                    Surface(
                        onClick = {
                            selectedChapters = if (isSelected) {
                                selectedChapters - chapter.index
                            } else {
                                selectedChapters + chapter.index
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = chapter.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${chapter.wordCount} 字",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (isSynced) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "•  已同步",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun BookInfoCard(state: SyncOptionsState, selectedCount: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (state.book.coverImagePath != null && File(state.book.coverImagePath).exists()) {
                AsyncImage(
                    model = File(state.book.coverImagePath),
                    contentDescription = null,
                    modifier = Modifier
                        .width(60.dp)
                        .height(80.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier
                        .width(60.dp)
                        .height(80.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.FormatListNumbered,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = state.book.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "手环: ${state.syncedChapters} / 总计: ${state.totalChapters}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (state.totalChapters > 0) {
                    LinearProgressIndicator(
                        progress = { state.syncedChapters.toFloat() / state.totalChapters },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                    )
                }
                Text(
                    text = "本次选择: $selectedCount 章",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )
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
