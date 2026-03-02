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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bandbbs.ebook.ui.viewmodel.SyncOptionsState
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.extra.SuperCheckbox
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.extra.SuperDropdown
import top.yukonga.miuix.kmp.extra.SuperSwitch
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import java.io.File

private val sortOptions = listOf(
    "按索引升序", "按索引降序", "按名称升序", "按名称降序", "按字数升序", "按字数降序"
)

private enum class ChapterSortType {
    INDEX_ASC, INDEX_DESC, NAME_ASC, NAME_DESC, WORD_COUNT_ASC, WORD_COUNT_DESC
}

@Composable
fun SyncOptionsScreen(
    state: SyncOptionsState,
    onBackClick: () -> Unit,
    onConfirm: (selectedChapterIndices: Set<Int>, syncCover: Boolean) -> Unit,
    onResyncCoverOnly: (() -> Unit)? = null,
    onDeleteChapters: ((Set<Int>) -> Unit)? = null
) {
    var selectedChapters by remember { mutableStateOf(setOf<Int>()) }
    var syncCover by remember { mutableStateOf(true) }
    var quickSelectText by remember { mutableStateOf("") }
    var sortIndex by remember { mutableStateOf(0) }

    val showChapterDialog = remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val sortType = ChapterSortType.values()[sortIndex]

    val sortedChapters = remember(state.chapters, sortType) {
        when (sortType) {
            ChapterSortType.INDEX_ASC -> state.chapters.sortedBy { it.index }
            ChapterSortType.INDEX_DESC -> state.chapters.sortedByDescending { it.index }
            ChapterSortType.NAME_ASC -> state.chapters.sortedBy { it.name }
            ChapterSortType.NAME_DESC -> state.chapters.sortedByDescending { it.name }
            ChapterSortType.WORD_COUNT_ASC -> state.chapters.sortedBy { it.wordCount }
            ChapterSortType.WORD_COUNT_DESC -> state.chapters.sortedByDescending { it.wordCount }
        }
    }

    LaunchedEffect(state.syncedChapterIndices, state.totalChapters, state.chapters) {
        if (state.chapters.isNotEmpty() && selectedChapters.isEmpty()) {
            val unsyncedChapterIndices = state.chapters
                .filter { it.index !in state.syncedChapterIndices }
                .map { it.index }
                .toSet()

            selectedChapters = unsyncedChapterIndices
        }
    }

    val selectedCount by remember { derivedStateOf { selectedChapters.size } }

    BackHandler {
        onBackClick()
    }

    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            TopAppBar(
                title = "传输设置",
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.padding(start = 6.dp)) {
                        Icon(imageVector = MiuixIcons.Back, contentDescription = "返回")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        popupHost = {}
    ) { paddingValues ->
        if (state.totalChapters == 0 && state.book.chapterCount > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("正在获取手环状态...", style = MiuixTheme.textStyles.body1)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .overScrollVertical()
                    .scrollEndHaptic()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    bottom = 48.dp
                )
            ) {
                item {
                    BookInfoHeader(state, selectedCount)
                }

                item {
                    SmallTitle(text = "封面设置")
                    Card(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        if (state.hasCover && !state.isCoverSynced) {
                            SuperSwitch(
                                title = "同步封面",
                                summary = "传输书籍封面图片",
                                checked = syncCover,
                                onCheckedChange = { syncCover = it }
                            )
                        } else if (state.hasCover && state.isCoverSynced && onResyncCoverOnly != null) {
                            SuperArrow(
                                title = "重新同步封面",
                                summary = "封面已存在，点击覆盖",
                                onClick = onResyncCoverOnly
                            )
                        } else {
                            SuperArrow(
                                title = "无封面",
                                summary = "当前书籍没有可用的封面",
                                enabled = false
                            )
                        }
                    }
                }

                item {
                    SmallTitle(text = "章节选择")
                    Card(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Column {
                            SuperArrow(
                                title = "手动选择章节",
                                summary = "当前已选 $selectedCount 章",
                                onClick = { showChapterDialog.value = true },
                                holdDownState = showChapterDialog.value
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextField(
                                    value = quickSelectText,
                                    onValueChange = {
                                        quickSelectText = it.filter { c -> c.isDigit() || c == '-' }
                                    },
                                    label = "范围 (如 1-50)",
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            val range = parseChapterRange(
                                                quickSelectText,
                                                state.totalChapters
                                            )
                                            if (range != null) {
                                                selectedChapters = range
                                                quickSelectText = ""
                                                focusManager.clearFocus()
                                            }
                                        }
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TextButton(
                                    text = "全选",
                                    onClick = {
                                        selectedChapters = (0 until state.totalChapters).toSet()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 4.dp)
                                )
                                TextButton(
                                    text = "清空",
                                    onClick = { selectedChapters = emptySet() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 4.dp)
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TextButton(
                                    text = "未读",
                                    onClick = {
                                        val currentChapterIndex = state.book.chapterIndex
                                        val startIndex =
                                            if (currentChapterIndex != null && currentChapterIndex >= 0) {
                                                currentChapterIndex.coerceIn(
                                                    0,
                                                    state.totalChapters - 1
                                                )
                                            } else {
                                                state.chapters.firstOrNull { it.index !in state.syncedChapterIndices }?.index
                                                    ?: state.totalChapters
                                            }
                                        selectedChapters =
                                            (startIndex until state.totalChapters).filter { it !in state.syncedChapterIndices }
                                                .toSet()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 4.dp)
                                )
                                TextButton(
                                    text = "已读",
                                    onClick = {
                                        val currentChapterIndex = state.book.chapterIndex
                                        if (currentChapterIndex != null && currentChapterIndex >= 0) {
                                            selectedChapters =
                                                (0 until currentChapterIndex).filter { it in state.syncedChapterIndices }
                                                    .toSet()
                                        } else {
                                            selectedChapters = emptySet()
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 4.dp)
                                )
                                if (onDeleteChapters != null && selectedCount > 0) {
                                    Button(
                                        onClick = { onDeleteChapters(selectedChapters) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 4.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            color = MiuixTheme.colorScheme.error
                                        )
                                    ) {
                                        Text(
                                            text = "删除",
                                            color = MiuixTheme.colorScheme.onError
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))

                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = onBackClick,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors()
                            ) {
                                Text("取消")
                            }

                            Button(
                                onClick = {
                                    if (selectedChapters.isNotEmpty()) {
                                        onConfirm(selectedChapters, syncCover)
                                    }
                                },
                                enabled = selectedChapters.isNotEmpty(),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColorsPrimary()
                            ) {
                                Text(
                                    text = "开始传输",
                                    color = MiuixTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        SuperDialog(
            title = "选择章节",
            show = showChapterDialog,
            onDismissRequest = { showChapterDialog.value = false }
        ) {
            val chapterListState = rememberLazyListState()

            Column {
                Card(
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    SuperDropdown(
                        title = "排序方式",
                        items = sortOptions,
                        selectedIndex = sortIndex,
                        onSelectedIndexChange = { sortIndex = it }
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    state = chapterListState
                ) {
                    itemsIndexed(
                        items = sortedChapters,
                        key = { _, chapter -> chapter.index }
                    ) { _, chapter ->
                        val isSelected = selectedChapters.contains(chapter.index)
                        val isSynced = chapter.index in state.syncedChapterIndices

                        SuperCheckbox(
                            title = chapter.name,
                            summary = "${chapter.wordCount} 字" + if (isSynced) " • 已同步" else "",
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                selectedChapters = if (checked) {
                                    selectedChapters + chapter.index
                                } else {
                                    selectedChapters - chapter.index
                                }
                            }
                        )
                    }
                }

                LaunchedEffect(
                    showChapterDialog.value,
                    sortedChapters,
                    state.syncedChapterIndices
                ) {
                    if (showChapterDialog.value) {
                        val firstUnsyncedPosition =
                            sortedChapters.indexOfFirst { it.index !in state.syncedChapterIndices }
                        if (firstUnsyncedPosition >= 0) {
                            chapterListState.scrollToItem(firstUnsyncedPosition)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { showChapterDialog.value = false },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColorsPrimary()
                ) {
                    Text(
                        text = "完成",
                        color = MiuixTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun BookInfoHeader(state: SyncOptionsState, selectedCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp), // 更宽敞的边距
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 封面
        if (state.book.coverImagePath != null && File(state.book.coverImagePath).exists()) {
            AsyncImage(
                model = File(state.book.coverImagePath),
                contentDescription = null,
                modifier = Modifier
                    .width(72.dp)
                    .height(96.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Surface(
                modifier = Modifier
                    .width(72.dp)
                    .height(96.dp),
                shape = RoundedCornerShape(8.dp),
                color = MiuixTheme.colorScheme.secondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("无封面", style = MiuixTheme.textStyles.footnote1)
                }
            }
        }

        Spacer(modifier = Modifier.width(20.dp))

        // 信息文本
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = state.book.name,
                style = MiuixTheme.textStyles.title3,
                color = MiuixTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "手环: ${state.syncedChapters} / 总计: ${state.totalChapters}",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantActions
            )
            if (state.totalChapters > 0) {
                LinearProgressIndicator(
                    progress = state.syncedChapters.toFloat() / state.totalChapters,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                )
            }
            Text(
                text = "本次选择: $selectedCount 章",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.primary
            )
        }
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
