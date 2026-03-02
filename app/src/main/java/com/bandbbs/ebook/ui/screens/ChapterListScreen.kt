package com.bandbbs.ebook.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.bandbbs.ebook.database.ChapterInfo
import com.bandbbs.ebook.ui.model.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.FloatingToolbar
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.ToolbarPosition
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.extra.SuperListPopup
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.SelectAll
import top.yukonga.miuix.kmp.icon.extended.Sort
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

private enum class SortType(val label: String) {
    INDEX_ASC("按索引升序"),
    INDEX_DESC("按索引降序"),
    NAME_ASC("按名称升序"),
    NAME_DESC("按名称降序"),
    WORD_COUNT_ASC("按字数升序"),
    WORD_COUNT_DESC("按字数降序")
}

@Composable
fun ChapterListScreen(
    book: Book,
    chapters: List<ChapterInfo>,
    readOnly: Boolean = false,
    onBackClick: () -> Unit,
    onPreviewChapter: (Int) -> Unit,
    onEditContent: (Int) -> Unit,
    onSaveChapterContent: (Int, String, String) -> Unit,
    onRenameChapter: (Int, String) -> Unit,
    onAddChapter: (Int, String, String) -> Unit,
    onMergeChapters: (List<Int>, String, Boolean) -> Unit,
    onBatchRename: (List<Int>, String, String, Int, Int) -> Unit,
    loadChapterContent: suspend (Int) -> String
) {
    val isPdf = book.format == "pdf"
    var isEditMode by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<Int>()) }

    val showMergeDialog = remember { mutableStateOf(false) }
    val showAddDialog = remember { mutableStateOf(false) }
    val showBatchRenameDialog = remember { mutableStateOf(false) }
    val showEditContentDialog = remember { mutableStateOf(false) }
    var renamingChapter by remember { mutableStateOf<ChapterInfo?>(null) }
    var editingChapterId by remember { mutableStateOf<Int?>(null) }

    var sortType by remember { mutableStateOf(SortType.INDEX_ASC) }
    val showSortMenu = remember { mutableStateOf(false) }
    val orderedChapters = remember { mutableStateListOf<ChapterInfo>() }
    val listState = rememberLazyListState()
    var hasInitialScroll by remember { mutableStateOf(false) }

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
        val wasOrderChanged =
            currentIds == newIds && currentIds.isNotEmpty() && orderedChapters.map { it.id } != sortedChapters.map { it.id }

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
            showBatchRenameDialog.value = false
        }

        if (wasOrderChanged && orderedChapters.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(orderedChapters, book.chapterIndex) {
        if (!hasInitialScroll && book.chapterIndex != null && orderedChapters.isNotEmpty()) {
            val targetIndex = orderedChapters.indexOfFirst { it.index == book.chapterIndex }
            if (targetIndex >= 0) {
                listState.scrollToItem(targetIndex)
                hasInitialScroll = true
            }
        }
    }

    LaunchedEffect(isEditMode) {
        if (!isEditMode) {
            selected = emptySet()
            showAddDialog.value = false
            showBatchRenameDialog.value = false
            showMergeDialog.value = false
            renamingChapter = null
            editingChapterId = null
            showEditContentDialog.value = false
        }
    }

    val selectedDetails = orderedChapters.filter { selected.contains(it.id) }
    val isAllSelected = selected.size == orderedChapters.size && orderedChapters.isNotEmpty()

    BackHandler {
        onBackClick()
    }

    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            TopAppBar(
                title = book.name,
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = "返回",
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                },
                actions = {
                    if (!readOnly && !isEditMode) {
                        Box {
                            IconButton(onClick = { showSortMenu.value = true }) {
                                Icon(MiuixIcons.Sort, contentDescription = "排序")
                            }

                            val sortOptions = SortType.values()
                            val selectedIndex = sortOptions.indexOf(sortType)

                            SuperListPopup(
                                show = showSortMenu,
                                alignment = PopupPositionProvider.Align.BottomEnd,
                                onDismissRequest = { showSortMenu.value = false }
                            ) {
                                ListPopupColumn {
                                    sortOptions.forEachIndexed { index, type ->
                                        DropdownImpl(
                                            text = type.label,
                                            optionSize = sortOptions.size,
                                            isSelected = selectedIndex == index,
                                            onSelectedIndexChange = {
                                                sortType = sortOptions[it]
                                                showSortMenu.value = false
                                            },
                                            index = index
                                        )
                                    }
                                }
                            }
                        }
                        IconButton(
                            onClick = { isEditMode = true },
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Icon(MiuixIcons.Edit, contentDescription = "编辑模式")
                        }
                    }
                    if (isEditMode) {
                        IconButton(onClick = {
                            selected =
                                if (isAllSelected) emptySet() else orderedChapters.map { it.id }
                                    .toSet()
                        }) {
                            Icon(
                                MiuixIcons.SelectAll,
                                contentDescription = if (isAllSelected) "取消全选" else "全选"
                            )
                        }
                        IconButton(
                            onClick = { isEditMode = false },
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Icon(MiuixIcons.Ok, contentDescription = "完成")
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingToolbar = {
            if (isEditMode) {
                FloatingToolbar {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        IconButton(onClick = { showAddDialog.value = true }) {
                            Icon(MiuixIcons.Add, contentDescription = "新增")
                        }
                        IconButton(
                            onClick = { showMergeDialog.value = true },
                            enabled = selected.size >= 2
                        ) {
                            Icon(MiuixIcons.Edit, contentDescription = "合并")
                        }
                        IconButton(
                            onClick = { showBatchRenameDialog.value = true },
                            enabled = selected.isNotEmpty()
                        ) {
                            Icon(MiuixIcons.Notes, contentDescription = "批量改名")
                        }
                    }
                }
            }
        },
        floatingToolbarPosition = ToolbarPosition.BottomCenter,
        popupHost = {}
    ) { paddingValues ->
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding() + 12.dp)
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
            insideMargin = PaddingValues(0.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .overScrollVertical()
                    .scrollEndHaptic()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                state = listState,
                contentPadding = PaddingValues(
                    top = 8.dp,
                    bottom = paddingValues.calculateBottomPadding() + if (isEditMode) 80.dp else 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 26.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (isPdf) "${book.chapterCount} 页" else "${book.chapterCount} 章 · ${book.wordCount} 字",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    }
                }

                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(
                            start = 26.dp,
                            end = 26.dp,
                            bottom = 8.dp
                        )
                    )
                }

                itemsIndexed(orderedChapters, key = { _, chapter -> chapter.id }) { _, chapter ->
                    val isCurrentChapter = chapter.index == book.chapterIndex

                    if (isEditMode) {
                        EditableChapterCard(
                            isPdf = isPdf,
                            chapter = chapter,
                            checked = selected.contains(chapter.id),
                            isCurrent = isCurrentChapter,
                            onCheckedChange = { checked ->
                                selected = selected.toMutableSet().apply {
                                    if (checked) add(chapter.id) else remove(chapter.id)
                                }
                            },
                            onPreview = { onPreviewChapter(chapter.id) },
                            onEditContent = {
                                editingChapterId = chapter.id
                                showEditContentDialog.value = true
                            },
                            onRename = { renamingChapter = chapter }
                        )
                    } else {
                        SuperArrow(
                            title = chapter.name,
                            titleColor = if (isCurrentChapter) {
                                BasicComponentDefaults.titleColor(color = MiuixTheme.colorScheme.primary)
                            } else {
                                BasicComponentDefaults.titleColor()
                            },
                            summary = if (!isPdf) "${chapter.wordCount} 字" else null,
                            onClick = { onPreviewChapter(chapter.id) }
                        )
                    }
                }
            }
        }
    }

    if (isEditMode) {
        AddChapterDialog(
            show = showAddDialog,
            chapterCount = chapters.size,
            onConfirm = { index, title, content ->
                onAddChapter(index, title, content)
                showAddDialog.value = false
            }
        )

        if (selectedDetails.isNotEmpty()) {
            BatchRenameDialog(
                show = showBatchRenameDialog,
                selected = selectedDetails,
                onConfirm = { prefix, suffix, startNumber, padding ->
                    onBatchRename(
                        selectedDetails.map { it.id },
                        prefix,
                        suffix,
                        startNumber,
                        padding
                    )
                    showBatchRenameDialog.value = false
                }
            )
        }

        editingChapterId?.let { chapterId ->
            val chapter = orderedChapters.find { it.id == chapterId }
            if (chapter != null) {
                EditContentDialog(
                    show = showEditContentDialog,
                    chapter = chapter,
                    onConfirm = { title, content ->
                        onSaveChapterContent(chapterId, title, content)
                        editingChapterId = null
                        showEditContentDialog.value = false
                    },
                    loadContent = loadChapterContent
                )
            }
        }

        MergeChaptersDialog(
            show = showMergeDialog,
            selectedChapters = chapters.filter { selected.contains(it.id) },
            onConfirm = { title, insertBlank ->
                onMergeChapters(selected.toList(), title, insertBlank)
                selected = emptySet()
                showMergeDialog.value = false
            }
        )

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
private fun EditableChapterCard(
    modifier: Modifier = Modifier,
    isPdf: Boolean,
    chapter: ChapterInfo,
    checked: Boolean,
    isCurrent: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onPreview: () -> Unit,
    onEditContent: () -> Unit,
    onRename: () -> Unit
) {
    val showActionDialog = remember { mutableStateOf(false) }

    BasicComponent(
        modifier = modifier,
        title = chapter.name,
        titleColor = if (isCurrent) {
            BasicComponentDefaults.titleColor(color = MiuixTheme.colorScheme.primary)
        } else {
            BasicComponentDefaults.titleColor()
        },
        summary = if (!isPdf) "${chapter.wordCount} 字" else null,
        startAction = {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.padding(end = 12.dp)
            )
        },
        onClick = { showActionDialog.value = true }
    )

    if (showActionDialog.value) {
        SuperDialog(
            title = "章节操作",
            show = showActionDialog,
            onDismissRequest = { showActionDialog.value = false }
        ) {
            Card(insideMargin = PaddingValues(0.dp)) {
                SuperArrow(
                    title = "预览",
                    onClick = {
                        showActionDialog.value = false
                        onPreview()
                    }
                )
                SuperArrow(
                    title = "编辑内容",
                    onClick = {
                        showActionDialog.value = false
                        onEditContent()
                    }
                )
                SuperArrow(
                    title = "改标题",
                    onClick = {
                        showActionDialog.value = false
                        onRename()
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                text = "取消",
                onClick = { showActionDialog.value = false },
                modifier = Modifier.fillMaxWidth()
            )
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
    val showDialog = remember { mutableStateOf(true) }

    LaunchedEffect(showDialog.value) {
        if (!showDialog.value) onDismiss()
    }

    SuperDialog(
        title = "编辑章节标题",
        show = showDialog,
        onDismissRequest = { showDialog.value = false }
    ) {
        TextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            label = "章节标题",
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextButton(
                text = "取消",
                onClick = { showDialog.value = false },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(20.dp))
            TextButton(
                text = "保存",
                onClick = {
                    if (text.isNotBlank()) {
                        onConfirm(text.trim())
                        showDialog.value = false
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary()
            )
        }
    }
}

@Composable
private fun AddChapterDialog(
    show: MutableState<Boolean>,
    chapterCount: Int,
    onConfirm: (Int, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var position by remember { mutableStateOf((chapterCount + 1).toString()) }

    SuperDialog(
        title = "新增章节",
        show = show,
        onDismissRequest = { show.value = false }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TextField(
                value = title,
                onValueChange = { title = it },
                label = "章节标题",
                singleLine = true
            )
            TextField(
                value = position,
                onValueChange = { value -> position = value.filter { it.isDigit() } },
                label = "插入位置(1-${chapterCount + 1})",
                singleLine = true
            )
            TextField(
                value = content,
                onValueChange = { content = it },
                label = "章节内容",
                modifier = Modifier.height(180.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    text = "取消",
                    onClick = { show.value = false },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(20.dp))
                TextButton(
                    text = "创建",
                    onClick = {
                        val index =
                            position.toIntOrNull()?.minus(1)?.coerceAtLeast(0) ?: chapterCount
                        onConfirm(index.coerceAtMost(chapterCount), title, content)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary()
                )
            }
        }
    }
}

@Composable
private fun BatchRenameDialog(
    show: MutableState<Boolean>,
    selected: List<ChapterInfo>,
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

    SuperDialog(
        title = "批量改名（${selected.size}）",
        show = show,
        onDismissRequest = { show.value = false }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TextField(
                value = prefix,
                onValueChange = { prefix = it },
                label = "前缀",
                singleLine = true
            )
            TextField(
                value = suffix,
                onValueChange = { suffix = it },
                label = "后缀",
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextField(
                    value = start,
                    onValueChange = { start = it.filter { ch -> ch.isDigit() } },
                    label = "起始编号",
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                TextField(
                    value = padding,
                    onValueChange = { padding = it.filter { ch -> ch.isDigit() } },
                    label = "补零位数",
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            if (previewTitles.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("预览：", style = MiuixTheme.textStyles.body2)
                    previewTitles.take(3).forEachIndexed { index, title ->
                        Text(
                            "· 第 ${selected[index].index + 1} 章 → $title",
                            style = MiuixTheme.textStyles.body2
                        )
                    }
                    if (previewTitles.size > 3) {
                        Text("... 等 ${previewTitles.size} 项", style = MiuixTheme.textStyles.body2)
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    text = "取消",
                    onClick = { show.value = false },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(20.dp))
                TextButton(
                    text = "应用",
                    onClick = {
                        val startNumber = start.toIntOrNull() ?: 1
                        val digits = padding.toIntOrNull() ?: 0
                        onConfirm(prefix, suffix, startNumber, digits)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary()
                )
            }
        }
    }
}

@Composable
private fun MergeChaptersDialog(
    show: MutableState<Boolean>,
    selectedChapters: List<ChapterInfo>,
    onConfirm: (String, Boolean) -> Unit
) {
    val sorted = remember(selectedChapters) { selectedChapters.sortedBy { it.index } }
    var title by remember { mutableStateOf(sorted.firstOrNull()?.name ?: "合并章节") }
    var insertBlank by remember { mutableStateOf(true) }
    val canMerge = sorted.size >= 2

    SuperDialog(
        title = "合并所选章节",
        show = show,
        onDismissRequest = { show.value = false }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("预计合并 ${sorted.size} 个章节：", style = MiuixTheme.textStyles.body2)
            sorted.take(3).forEach {
                Text("· ${it.index + 1}. ${it.name}", style = MiuixTheme.textStyles.body2)
            }
            if (sorted.size > 3) {
                Text("... 等 ${sorted.size} 项", style = MiuixTheme.textStyles.body2)
            }
            TextField(
                value = title,
                onValueChange = { title = it },
                label = "合并后标题",
                singleLine = true
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = insertBlank, onCheckedChange = { insertBlank = it })
                Spacer(Modifier.width(8.dp))
                Text("章节之间插入空行", style = MiuixTheme.textStyles.body2)
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                TextButton(
                    text = "取消",
                    onClick = { show.value = false },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(20.dp))
                TextButton(
                    text = "合并",
                    onClick = {
                        if (canMerge) {
                            onConfirm(title, insertBlank)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary()
                )
            }
        }
    }
}

private fun MutableList<ChapterInfo>.replaceWith(source: List<ChapterInfo>) {
    clear()
    addAll(source)
}

@Composable
private fun EditContentDialog(
    show: MutableState<Boolean>,
    chapter: ChapterInfo,
    onConfirm: (String, String) -> Unit,
    loadContent: suspend (Int) -> String
) {
    var title by remember(chapter.id) { mutableStateOf(chapter.name) }
    var content by remember(chapter.id) { mutableStateOf("") }

    LaunchedEffect(chapter.id) {
        content = withContext(Dispatchers.IO) {
            try {
                loadContent(chapter.id)
            } catch (e: Exception) {
                ""
            }
        }
    }

    SuperDialog(
        title = "编辑内容",
        show = show,
        onDismissRequest = { show.value = false }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TextField(
                value = title,
                onValueChange = { title = it },
                label = "章节标题",
                singleLine = true
            )
            TextField(
                value = content,
                onValueChange = { content = it },
                label = "章节内容",
                modifier = Modifier.height(250.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    text = "取消",
                    onClick = { show.value = false },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(20.dp))
                TextButton(
                    text = "保存",
                    onClick = { onConfirm(title.trim(), content) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary()
                )
            }
        }
    }
}
