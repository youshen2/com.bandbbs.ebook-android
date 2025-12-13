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
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
                if (isMultipleFiles) "批量导入书籍 (${state.files.size})" else "导入书籍",
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
                Text(if (isMultipleFiles) "批量导入" else "导入")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isMultipleFiles) {

            Text(
                text = "待导入文件 (${state.files.size} 个)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.files.take(5).forEach { fileInfo ->
                        Text(
                            text = "• ${fileInfo.bookName} (${fileInfo.fileFormat.uppercase()})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (state.files.size > 5) {
                        Text(
                            text = "... 还有 ${state.files.size - 5} 个文件",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "将使用文件名作为书名，以下设置将统一应用到所有文件",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {

            OutlinedTextField(
                value = bookName,
                onValueChange = { bookName = it },
                label = { Text("书名") },
                placeholder = { Text("请输入书籍名称") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                supportingText = if (isBookNameExists) {
                    {
                        Text(
                            "检测到同名书籍，将根据已有内容覆盖",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else null
            )
        }

        if (!isBookNameExists || isMultipleFiles) {
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "分类",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                onClick = { onShowCategorySelector() },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = selectedCategory ?: "未分类",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.Outlined.ExpandMore,
                        contentDescription = "选择分类",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "章节设置",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { noSplit = !noSplit },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = noSplit,
                    onCheckedChange = { noSplit = it }
                )
                Column {
                    Text(
                        text = "不分章",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = "将整本书作为单个章节导入",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (hasTxt) {
                Spacer(modifier = Modifier.height(12.dp))

                Box {
                    ExposedDropdownMenuBox(
                        expanded = showSplitMethodMenu,
                        onExpandedChange = {
                            if (!noSplit) {
                                showSplitMethodMenu = !showSplitMethodMenu
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = ChapterSplitter.methods[splitMethod] ?: "",
                            onValueChange = {},
                            label = { Text("分章方式") },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            readOnly = true,
                            enabled = !noSplit,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.ExpandMore,
                                    contentDescription = null,
                                    tint = if (noSplit) MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = 0.38f
                                    )
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = showSplitMethodMenu,
                            onDismissRequest = { showSplitMethodMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            ChapterSplitter.methods.forEach { (key, value) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            value,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    },
                                    onClick = {
                                        splitMethod = key
                                        showSplitMethodMenu = false
                                    },
                                    modifier = Modifier.background(
                                        if (splitMethod == key) MaterialTheme.colorScheme.secondaryContainer.copy(
                                            alpha = 0.3f
                                        )
                                        else Color.Transparent
                                    )
                                )
                            }
                        }
                    }
                }

                if (!noSplit && splitMethod == ChapterSplitter.METHOD_BY_WORD_COUNT) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = wordsPerChapterText,
                        onValueChange = {
                            if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                wordsPerChapterText = it
                            }
                        },
                        label = { Text("每章字数") },
                        placeholder = { Text("5000") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        supportingText = { Text("范围: 100 - 50000 字") }
                    )
                }

                if (!noSplit && splitMethod == ChapterSplitter.METHOD_CUSTOM) {
                    Spacer(modifier = Modifier.height(12.dp))
                    androidx.compose.runtime.LaunchedEffect(splitMethod) {
                        if (splitMethod == ChapterSplitter.METHOD_CUSTOM && customRegex.isBlank()) {
                            customRegex =
                                """^(第(\s{0,1}[一二三四五六七八九十百千万零〇\d]+\s{0,1})(章|卷|节|部|篇|回|本)|番外\s{0,2}[一二三四五六七八九十百千万零〇\d]*)(.{0,30})$"""
                        }
                    }
                    var regexError by remember { mutableStateOf<String?>(null) }
                    OutlinedTextField(
                        value = customRegex,
                        onValueChange = {
                            customRegex = it
                            regexError = try {
                                if (it.isNotBlank()) {
                                    Regex(it)
                                    null
                                } else {
                                    null
                                }
                            } catch (e: Exception) {
                                "正则表达式格式错误: ${e.message}"
                            }
                        },
                        label = { Text("自定义正则表达式") },
                        placeholder = { Text("请输入正则表达式") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(16.dp),
                        supportingText = {
                            Column {
                                Text("用于匹配章节标题的正则表达式")
                                if (regexError != null) {
                                    Text(
                                        regexError!!,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    )
                                }
                            }
                        },
                        isError = regexError != null
                    )
                }

                if (!noSplit) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = when {
                                splitMethod == ChapterSplitter.METHOD_BY_WORD_COUNT -> "系统将按指定字数自动分章"
                                splitMethod == ChapterSplitter.METHOD_CUSTOM -> "系统将使用自定义正则表达式识别章节标题并分章"
                                else -> "系统将根据所选方式自动识别章节标题并分章"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            if (hasEpubOrNvb) {
                if (!noSplit) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isMultipleFiles) {
                                val epubCount = state.files.count { it.fileFormat == "epub" }
                                val nvbCount = state.files.count { it.fileFormat == "nvb" }
                                when {
                                    epubCount > 0 && nvbCount > 0 -> "EPUB 和 NVB 格式自带章节信息，将按原有章节导入"
                                    epubCount > 0 -> "EPUB 格式自带章节信息，将按原有章节导入"
                                    else -> "NVB 格式自带章节信息，将按原有章节导入"
                                }
                            } else {
                                if (state.fileFormat == "epub") "EPUB 格式自带章节信息，将按原有章节导入" else "NVB 格式自带章节信息，将按原有章节导入"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "章节处理",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { enableChapterMerge = !enableChapterMerge },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = enableChapterMerge,
                            onCheckedChange = { enableChapterMerge = it }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "合并短章节",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = "将字数少于指定值的章节合并到上一章节",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (enableChapterMerge) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = mergeMinWordsText,
                            onValueChange = {
                                if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                    mergeMinWordsText = it
                                }
                            },
                            label = { Text("最小字数") },
                            placeholder = { Text("500") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            supportingText = { Text("字数少于此值的章节将被合并") }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { enableChapterRename = !enableChapterRename },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = enableChapterRename,
                            onCheckedChange = { enableChapterRename = it }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "重命名章节",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = "使用正则表达式替换章节标题",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (enableChapterRename) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = renamePattern,
                            onValueChange = { renamePattern = it },
                            label = { Text("替换规则") },
                            placeholder = { Text("例如: ^第(\\d+)章 -> 第$1章") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = false,
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(16.dp),
                            supportingText = {
                                Column {
                                    Text("格式: 查找模式 -> 替换文本")
                                    Text(
                                        "支持正则表达式，使用 $1, $2 等引用捕获组",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.7f
                                            )
                                        )
                                    )
                                    renamePreview?.let { preview ->
                                        when (preview) {
                                            is RenamePreviewResult.Success -> {
                                                Text(
                                                    preview.text,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                )
                                            }

                                            is RenamePreviewResult.Error -> {
                                                Text(
                                                    preview.text,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
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
