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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.MergeType
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bandbbs.ebook.ui.viewmodel.ImportState
import com.bandbbs.ebook.utils.ChapterSplitter

sealed class RenamePreviewResult {
    data class Success(val text: String) : RenamePreviewResult()
    data class Error(val text: String) : RenamePreviewResult()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportBookBottomSheet(
    state: ImportState,
    categories: List<String>,
    existingBookNames: List<String>,
    onCancel: () -> Unit,
    onConfirm: (bookName: String, splitMethod: String, noSplit: Boolean, wordsPerChapter: Int, selectedCategory: String?, enableChapterMerge: Boolean, mergeMinWords: Int, enableChapterRename: Boolean, renamePattern: String, customRegex: String) -> Unit,
    onShowCategorySelector: () -> Unit
) {
    var bookName by remember { mutableStateOf(state.bookName) }
    var splitMethod by remember { mutableStateOf(state.splitMethod) }
    var showSplitMethodMenu by remember { mutableStateOf(false) }
    var noSplit by remember { mutableStateOf(state.noSplit) }
    var wordsPerChapterText by remember { mutableStateOf(state.wordsPerChapter.toString()) }
    var selectedCategory by remember { mutableStateOf(state.selectedCategory) }
    var enableChapterMerge by remember { mutableStateOf(state.enableChapterMerge) }
    var mergeMinWordsText by remember { mutableStateOf(state.mergeMinWords.toString()) }
    var enableChapterRename by remember { mutableStateOf(state.enableChapterRename) }
    var renamePattern by remember { mutableStateOf(state.renamePattern) }
    var customRegex by remember { mutableStateOf(state.customRegex) }

    val isMultipleFiles = state.isMultipleFiles

    val hasEpubOrNvb = remember(state.files) {
        state.files.any { it.fileFormat == "epub" || it.fileFormat == "nvb" }
    }

    val hasTxt = remember(state.files) {
        state.files.any { it.fileFormat == "txt" }
    }

    val isBookNameExists = remember(bookName, existingBookNames) {
        !isMultipleFiles && bookName.trim()
            .isNotEmpty() && existingBookNames.contains(bookName.trim())
    }

    val renamePreview = remember(renamePattern) {
        if (renamePattern.isBlank()) {
            null
        } else {
            val parts = renamePattern.split(" -> ", limit = 2)
            if (parts.size == 2) {
                val example = "示例章节标题"
                try {
                    val regex = Regex(parts[0].trim())
                    val result = regex.replace(example) { matchResult ->
                        var res = parts[1].trim()
                        matchResult.groupValues.forEachIndexed { index, group ->
                            if (index > 0) {
                                res = res.replace("\$$index", group)
                            }
                        }
                        res
                    }
                    RenamePreviewResult.Success("预览: \"$example\" -> \"$result\"")
                } catch (e: Exception) {
                    RenamePreviewResult.Error("正则表达式格式错误")
                }
            } else {
                null
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(state.selectedCategory) {
        selectedCategory = state.selectedCategory
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
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
                if (isMultipleFiles) "批量导入 (${state.files.size})" else "导入书籍",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(
                onClick = {
                    val wordsPerChapter =
                        wordsPerChapterText.toIntOrNull()?.coerceIn(100, 50000) ?: 5000
                    val mergeMinWords = mergeMinWordsText.toIntOrNull()?.coerceIn(100, 10000) ?: 500
                    onConfirm(
                        bookName,
                        splitMethod,
                        noSplit,
                        wordsPerChapter,
                        selectedCategory,
                        enableChapterMerge,
                        mergeMinWords,
                        enableChapterRename,
                        renamePattern,
                        customRegex
                    )
                },
                enabled = if (isMultipleFiles) true else bookName.isNotBlank()
            ) {
                Text("导入")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // File Information Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (isMultipleFiles) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.LibraryBooks, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "待导入文件",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    state.files.take(3).forEach { fileInfo ->
                        Text(
                            text = "• ${fileInfo.bookName} (${fileInfo.fileFormat.uppercase()})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (state.files.size > 3) {
                        Text(
                            text = "... 还有 ${state.files.size - 3} 个文件",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = bookName,
                        onValueChange = { bookName = it },
                        label = { Text("书名") },
                        placeholder = { Text("请输入书籍名称") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        isError = isBookNameExists,
                        supportingText = if (isBookNameExists) {
                            { Text("书名已存在，导入将覆盖原书") }
                        } else null
                    )
                }
            }
        }

        if (!isBookNameExists || isMultipleFiles) {
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                ListItem(
                    headlineContent = { Text("分类") },
                    supportingContent = { Text(selectedCategory ?: "未分类") },
                    trailingContent = { Icon(Icons.Outlined.ExpandMore, contentDescription = null) },
                    modifier = Modifier.clickable { onShowCategorySelector() },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column {
                    Text(
                        text = "章节设置",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                    )

                    ListItem(
                        headlineContent = { Text("不分章") },
                        supportingContent = { Text("整本书作为单个章节导入") },
                        trailingContent = {
                            Switch(checked = noSplit, onCheckedChange = { noSplit = it })
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    if (hasTxt && !noSplit) {
                        ListItem(
                            headlineContent = { Text("分章方式") },
                            supportingContent = { Text(ChapterSplitter.methods[splitMethod] ?: "") },
                            trailingContent = {
                                Box {
                                    IconButton(onClick = { showSplitMethodMenu = true }) {
                                        Icon(Icons.Outlined.ExpandMore, contentDescription = null)
                                    }
                                    DropdownMenu(
                                        expanded = showSplitMethodMenu,
                                        onDismissRequest = { showSplitMethodMenu = false }
                                    ) {
                                        ChapterSplitter.methods.forEach { (key, value) ->
                                            DropdownMenuItem(
                                                text = { Text(value) },
                                                onClick = {
                                                    splitMethod = key
                                                    showSplitMethodMenu = false
                                                },
                                                trailingIcon = if (splitMethod == key) {
                                                    { Icon(Icons.Outlined.Check, null) }
                                                } else null
                                            )
                                        }
                                    }
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )

                        if (splitMethod == ChapterSplitter.METHOD_BY_WORD_COUNT) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                OutlinedTextField(
                                    value = wordsPerChapterText,
                                    onValueChange = { if (it.all { c -> c.isDigit() }) wordsPerChapterText = it },
                                    label = { Text("每章字数") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }

                        if (splitMethod == ChapterSplitter.METHOD_CUSTOM) {
                            androidx.compose.runtime.LaunchedEffect(splitMethod) {
                                if (customRegex.isBlank()) {
                                    customRegex = """^(第(\s{0,1}[一二三四五六七八九十百千万零〇\d]+\s{0,1})(章|卷|节|部|篇|回|本)|番外\s{0,2}[一二三四五六七八九十百千万零〇\d]*)(.{0,30})$"""
                                }
                            }
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                OutlinedTextField(
                                    value = customRegex,
                                    onValueChange = { customRegex = it },
                                    label = { Text("正则表达式") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 3
                                )
                            }
                        }
                    } else if (hasEpubOrNvb && !noSplit) {
                        ListItem(
                            headlineContent = { Text("使用内置章节") },
                            supportingContent = { Text("EPUB/NVB 格式将按原有章节导入") },
                            leadingContent = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (hasEpubOrNvb && !noSplit) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column {
                        Text(
                            text = "高级处理",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                        )

                        ListItem(
                            headlineContent = { Text("合并短章节") },
                            supportingContent = { Text("将字数过少的章节合并") },
                            trailingContent = {
                                Switch(checked = enableChapterMerge, onCheckedChange = { enableChapterMerge = it })
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )

                        if (enableChapterMerge) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                OutlinedTextField(
                                    value = mergeMinWordsText,
                                    onValueChange = { if (it.all { c -> c.isDigit() }) mergeMinWordsText = it },
                                    label = { Text("最小字数阈值") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }

                        ListItem(
                            headlineContent = { Text("重命名章节") },
                            supportingContent = { Text("使用正则替换章节标题") },
                            trailingContent = {
                                Switch(checked = enableChapterRename, onCheckedChange = { enableChapterRename = it })
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )

                        if (enableChapterRename) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                OutlinedTextField(
                                    value = renamePattern,
                                    onValueChange = { renamePattern = it },
                                    label = { Text("替换规则 (正则 -> 替换)") },
                                    placeholder = { Text("^第(\\d+)章 -> 第$1章") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 2
                                )
                                renamePreview?.let { preview ->
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = when(preview) {
                                            is RenamePreviewResult.Success -> preview.text
                                            is RenamePreviewResult.Error -> preview.text
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if(preview is RenamePreviewResult.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}