package com.bandbbs.ebook.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.ModeEdit
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bandbbs.ebook.database.ChapterInfo
import com.bandbbs.ebook.ui.model.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class SortType {
    INDEX_ASC,
    INDEX_DESC,
    NAME_ASC,
    NAME_DESC,
    WORD_COUNT_ASC,
    WORD_COUNT_DESC
}

@Composable
fun ChapterListBottomSheet(
    book: Book,
    chapters: List<ChapterInfo>,
    readOnly: Boolean = false,
    onPreviewChapter: (Int) -> Unit,
    onEditContent: (Int) -> Unit,
    onSaveChapterContent: (Int, String, String) -> Unit,
    onRenameChapter: (Int, String) -> Unit,
    onAddChapter: (Int, String, String) -> Unit,
    onMergeChapters: (List<Int>, String, Boolean) -> Unit,
    onBatchRename: (List<Int>, String, String, Int, Int) -> Unit,
    loadChapterContent: suspend (Int) -> String
) {
    var isEditMode by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<Int>()) }
    var showMergeDialog by remember { mutableStateOf(false) }
    var renamingChapter by remember { mutableStateOf<ChapterInfo?>(null) }
    var showAddPanel by remember { mutableStateOf(false) }
    var showBatchRename by remember { mutableStateOf(false) }
    var editingChapterId by remember { mutableStateOf<Int?>(null) }
    var sortType by remember { mutableStateOf(SortType.INDEX_ASC) }
    var showSortMenu by remember { mutableStateOf(false) }
    val orderedChapters = remember { mutableStateListOf<ChapterInfo>() }
    val listState = rememberLazyListState()

    fun sortChapters(chapters: List<ChapterInfo>): List<ChapterInfo> {
        return when (sortType) {
            SortType.INDEX_ASC -> chapters.sortedBy { it.index }
            SortType.INDEX_DESC -> chapters.sortedByDescending { it.index }
            SortType.NAME_ASC -> chapters.sortedBy { it.name }
            SortType.NAME_DESC -> chapters.sortedByDescending { it.name }
            SortType.WORD_COUNT_ASC -> chapters.sortedBy { it.wordCount }
            SortType.WORD_COUNT_DESC -> chapters.sortedByDescending { it.wordCount }
        }
    }

    LaunchedEffect(chapters, sortType) {
        val currentIds = orderedChapters.map { it.id }.toSet()
        val newIds = chapters.map { it.id }.toSet()
        val sortedChapters = sortChapters(chapters)
        val wasOrderChanged = currentIds == newIds && currentIds.isNotEmpty() && orderedChapters.map { it.id } != sortedChapters.map { it.id }

        if (currentIds != newIds) {
            orderedChapters.replaceWith(sortedChapters)
        } else if (currentIds.isEmpty()) {
            orderedChapters.replaceWith(sortedChapters)
        } else {
            val currentOrder = orderedChapters.map { it.id }
            val newOrder = sortedChapters.map { it.id }

            if (currentOrder == newOrder) {
                val chapterMap = sortedChapters.associateBy { it.id }
                orderedChapters.forEachIndexed { index, existingChapter ->
                    chapterMap[existingChapter.id]?.let { updatedChapter ->
                        if (existingChapter != updatedChapter) {
                            orderedChapters[index] = updatedChapter
                        }
                    }
                }
            } else {
                orderedChapters.replaceWith(sortedChapters)
            }
        }

        val ids = orderedChapters.map { it.id }.toSet()
        selected = selected.filter { ids.contains(it) }.toSet()
        if (selected.isEmpty()) {
            showBatchRename = false
        }

        if (wasOrderChanged && orderedChapters.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(isEditMode) {
        if (!isEditMode) {
            selected = emptySet()
            showAddPanel = false
            showBatchRename = false
            showMergeDialog = false
            renamingChapter = null
            editingChapterId = null
        }
    }

    val selectedDetails = orderedChapters.filter { selected.contains(it.id) }
    val isAllSelected = selected.size == orderedChapters.size && orderedChapters.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
    ) {
        Text(
            text = book.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${book.chapterCount} 章 · ${book.wordCount} 字",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (isEditMode) {
            Surface(
                tonalElevation = 1.dp,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "编辑模式",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Row {
                            TextButton(onClick = {
                                selected = if (isAllSelected) {
                                    emptySet()
                                } else {
                                    orderedChapters.map { it.id }.toSet()
                                }
                            }) {
                                Text(if (isAllSelected) "取消全选" else "全选")
                            }
                            TextButton(onClick = { isEditMode = false }) {
                                Text("完成")
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { showAddPanel = !showAddPanel },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.width(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (showAddPanel) "收起" else "新增",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        TextButton(
                            enabled = selected.size >= 2,
                            onClick = { showMergeDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Outlined.Edit,
                                contentDescription = null,
                                modifier = Modifier.width(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "合并(${selected.size})",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        TextButton(
                            enabled = selected.isNotEmpty(),
                            onClick = { showBatchRename = !showBatchRename },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Outlined.EditNote,
                                contentDescription = null,
                                modifier = Modifier.width(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (showBatchRename) "收起" else "批量改名",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        } else {
            if (!readOnly) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        TextButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Outlined.Sort, contentDescription = "排序")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("排序")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("按索引升序") },
                                onClick = { sortType = SortType.INDEX_ASC; showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("按索引降序") },
                                onClick = { sortType = SortType.INDEX_DESC; showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("按名称升序") },
                                onClick = { sortType = SortType.NAME_ASC; showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("按名称降序") },
                                onClick = { sortType = SortType.NAME_DESC; showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("按字数升序") },
                                onClick = { sortType = SortType.WORD_COUNT_ASC; showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("按字数降序") },
                                onClick = { sortType = SortType.WORD_COUNT_DESC; showSortMenu = false }
                            )
                        }
                    }
                    TextButton(onClick = { isEditMode = true }) {
                        Icon(Icons.Outlined.ModeEdit, contentDescription = "编辑模式")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("编辑")
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        AnimatedVisibility(visible = isEditMode && showAddPanel) {
            AddChapterPanel(
                chapterCount = chapters.size,
                onCancel = { showAddPanel = false },
                onConfirm = { index, title, content ->
                    onAddChapter(index, title, content)
                    showAddPanel = false
                }
            )
        }
        AnimatedVisibility(visible = isEditMode && showBatchRename && selectedDetails.isNotEmpty()) {
            BatchRenamePanel(
                selected = selectedDetails,
                onCancel = { showBatchRename = false },
                onConfirm = { prefix, suffix, startNumber, padding ->
                    onBatchRename(
                        selectedDetails.map { it.id },
                        prefix,
                        suffix,
                        startNumber,
                        padding
                    )
                    showBatchRename = false
                }
            )
        }
        AnimatedVisibility(visible = isEditMode && editingChapterId != null) {
            editingChapterId?.let { chapterId ->
                val chapter = orderedChapters.find { it.id == chapterId }
                if (chapter != null) {
                    EditContentPanel(
                        chapter = chapter,
                        onCancel = { editingChapterId = null },
                        onSave = { title, content ->
                            onSaveChapterContent(chapterId, title, content)
                            editingChapterId = null
                        },
                        loadContent = loadChapterContent
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Divider()
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(orderedChapters, key = { _, chapter -> chapter.id }) { index, chapter ->
                if (isEditMode) {
                    EditableChapterCard(
                        chapter = chapter,
                        checked = selected.contains(chapter.id),
                        onCheckedChange = { checked ->
                            selected = selected.toMutableSet().apply {
                                if (checked) add(chapter.id) else remove(chapter.id)
                            }
                        },
                        onPreview = { onPreviewChapter(chapter.id) },
                        onEditContent = { editingChapterId = chapter.id },
                        onRename = { renamingChapter = chapter },
                        isEditMode = true
                    )
                } else {
                    SimpleChapterCard(
                        chapter = chapter,
                        onPreview = { onPreviewChapter(chapter.id) }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }

    if (isEditMode && showMergeDialog) {
        MergeChaptersDialog(
            selectedChapters = chapters.filter { selected.contains(it.id) },
            onDismiss = { showMergeDialog = false },
            onConfirm = { title, insertBlank ->
                onMergeChapters(selected.toList(), title, insertBlank)
                selected = emptySet()
                showMergeDialog = false
            }
        )
    }

    if (isEditMode) {
        renamingChapter?.let { chapter ->
            RenameChapterDialog(
                chapter = chapter,
                onDismiss = { renamingChapter = null },
                onConfirm = { title ->
                    onRenameChapter(chapter.id, title)
                    renamingChapter = null
                }
            )
        }
    }
}

@Composable
private fun SimpleChapterCard(
    chapter: ChapterInfo,
    onPreview: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPreview),
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = "${chapter.name}",
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${chapter.wordCount} 字",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EditableChapterCard(
    modifier: Modifier = Modifier,
    chapter: ChapterInfo,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onPreview: () -> Unit,
    onEditContent: () -> Unit,
    onRename: () -> Unit,
    isEditMode: Boolean = true
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${chapter.name}",
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${chapter.wordCount} 字",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Checkbox(
                    checked = checked,
                    onCheckedChange = onCheckedChange
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onPreview) { Text("预览") }
                TextButton(onClick = onEditContent) { Text("编辑内容") }
                TextButton(onClick = onRename) { Text("改标题") }
            }
        }
    }
}

@Composable
private fun RenameChapterDialog(
    chapter: ChapterInfo,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(chapter.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.trim()) },
                enabled = text.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text("编辑章节标题") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text("章节标题") }
            )
        }
    )
}

@Composable
private fun AddChapterPanel(
    chapterCount: Int,
    onCancel: () -> Unit,
    onConfirm: (Int, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var position by remember { mutableStateOf((chapterCount + 1).toString()) }
    val scrollState = rememberScrollState()

    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("新增章节", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("章节标题") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = position,
                onValueChange = { value ->
                    position = value.filter { it.isDigit() }
                },
                label = { Text("插入位置(1-${chapterCount + 1})") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("章节内容") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("取消") }
                TextButton(
                    onClick = {
                        val index =
                            position.toIntOrNull()?.minus(1)?.coerceAtLeast(0) ?: chapterCount
                        onConfirm(index.coerceAtMost(chapterCount), title, content)
                    },
                    enabled = content.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) { Text("创建") }
            }
        }
    }
}

@Composable
private fun BatchRenamePanel(
    selected: List<ChapterInfo>,
    onCancel: () -> Unit,
    onConfirm: (String, String, Int, Int) -> Unit
) {
    var prefix by remember { mutableStateOf("第") }
    var suffix by remember { mutableStateOf("章") }
    var start by remember {
        mutableStateOf(
            (selected.firstOrNull()?.index?.plus(1) ?: 1).toString()
        )
    }
    var padding by remember { mutableStateOf("2") }
    val previewTitles = remember(selected, prefix, suffix, start, padding) {
        val startNumber = start.toIntOrNull() ?: 1
        val digits = padding.toIntOrNull()?.coerceAtLeast(0) ?: 0
        selected.mapIndexed { idx, chapter ->
            val number = (startNumber + idx).coerceAtLeast(0)
            val formatted = number.toString().let { value ->
                if (digits > 0) value.padStart(digits, '0') else value
            }
            buildString {
                if (prefix.isNotBlank()) append(prefix.trim())
                append(formatted)
                if (suffix.isNotBlank()) append(suffix.trim())
            }.ifBlank { chapter.name }
        }
    }

    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("批量改名（${selected.size}）", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = prefix,
                onValueChange = { prefix = it },
                label = { Text("前缀") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = suffix,
                onValueChange = { suffix = it },
                label = { Text("后缀") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = start,
                    onValueChange = { start = it.filter { ch -> ch.isDigit() } },
                    label = { Text("起始编号") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = padding,
                    onValueChange = { padding = it.filter { ch -> ch.isDigit() } },
                    label = { Text("补零位数") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            if (previewTitles.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("预览：", style = MaterialTheme.typography.bodySmall)
                    previewTitles.take(3).forEachIndexed { index, title ->
                        Text(
                            "· 第 ${selected[index].index + 1} 章 → $title",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (previewTitles.size > 3) {
                        Text(
                            "... 等 ${previewTitles.size} 项",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("取消") }
                TextButton(
                    onClick = {
                        val startNumber = start.toIntOrNull() ?: 1
                        val digits = padding.toIntOrNull() ?: 0
                        onConfirm(prefix, suffix, startNumber, digits)
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("应用") }
            }
        }
    }
}

@Composable
private fun MergeChaptersDialog(
    selectedChapters: List<ChapterInfo>,
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean) -> Unit
) {
    val sorted = remember(selectedChapters) { selectedChapters.sortedBy { it.index } }
    var title by remember { mutableStateOf(sorted.firstOrNull()?.name ?: "合并章节") }
    var insertBlank by remember { mutableStateOf(true) }

    val canMerge = sorted.size >= 2

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = canMerge,
                onClick = {
                    if (canMerge) {
                        onConfirm(title, insertBlank)
                    }
                }
            ) { Text("合并") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text("合并所选章节") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("预计合并 ${sorted.size} 个章节：")
                sorted.forEach {
                    Text(
                        "· ${it.index + 1}. ${it.name}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("合并后标题") },
                    singleLine = true
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = insertBlank, onCheckedChange = { insertBlank = it })
                    Text("章节之间插入空行")
                }
            }
        }
    )
}

private fun MutableList<ChapterInfo>.replaceWith(source: List<ChapterInfo>) {
    clear()
    addAll(source)
}

@Composable
private fun EditContentPanel(
    chapter: ChapterInfo,
    onCancel: () -> Unit,
    onSave: (String, String) -> Unit,
    loadContent: suspend (Int) -> String
) {
    var title by remember(chapter.id) { mutableStateOf(chapter.name) }
    var content by remember(chapter.id) { mutableStateOf("") }
    val scrollState = rememberScrollState()

    LaunchedEffect(chapter.id) {

        content = withContext(Dispatchers.IO) {
            try {
                loadContent(chapter.id)
            } catch (e: Exception) {
                ""
            }
        }
    }

    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "编辑内容 - ${chapter.name}",
                style = MaterialTheme.typography.titleMedium
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("章节标题") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("章节内容") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text("取消")
                }
                TextButton(
                    onClick = { onSave(title.trim(), content) },
                    enabled = content.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("保存")
                }
            }
        }
    }
}
