package com.bandbbs.ebook.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bandbbs.ebook.ui.viewmodel.ImportState
import com.bandbbs.ebook.utils.ChapterSplitter
import androidx.compose.foundation.layout.fillMaxWidth

sealed class RenamePreviewResult {
    data class Success(val text: String) : RenamePreviewResult()
    data class Error(val text: String) : RenamePreviewResult()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportBookBottomSheet(
    state: ImportState,
    categories: List<String>,
    onCancel: () -> Unit,
    onConfirm: (bookName: String, splitMethod: String, noSplit: Boolean, wordsPerChapter: Int, selectedCategory: String?, enableChapterMerge: Boolean, mergeMinWords: Int, enableChapterRename: Boolean, renamePattern: String) -> Unit,
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
                "导入书籍",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(
                onClick = { 
                    val wordsPerChapter = wordsPerChapterText.toIntOrNull()?.coerceIn(100, 50000) ?: 5000
                    val mergeMinWords = mergeMinWordsText.toIntOrNull()?.coerceIn(100, 10000) ?: 500
                    onConfirm(bookName, splitMethod, noSplit, wordsPerChapter, selectedCategory, enableChapterMerge, mergeMinWords, enableChapterRename, renamePattern) 
                },
                enabled = bookName.isNotBlank()
            ) {
                Text("导入")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = bookName,
            onValueChange = { bookName = it },
            label = { Text("书名") },
            placeholder = { Text("请输入书籍名称") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )
        
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
            modifier = Modifier.fillMaxWidth().clickable { noSplit = !noSplit },
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

        if (state.fileFormat != "epub" && state.fileFormat != "nvb") {
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
                                tint = if (noSplit) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
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
                                    if (splitMethod == key) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
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
            
            if (!noSplit) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (splitMethod == ChapterSplitter.METHOD_BY_WORD_COUNT) 
                            "系统将按指定字数自动分章" 
                        else 
                            "系统将根据所选方式自动识别章节标题并分章",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        } else {
            if (!noSplit) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (state.fileFormat == "epub") "EPUB 格式自带章节信息，将按原有章节导入" else "NVB 格式自带章节信息，将按原有章节导入",
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
                    modifier = Modifier.fillMaxWidth().clickable { enableChapterMerge = !enableChapterMerge },
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
                    modifier = Modifier.fillMaxWidth().clickable { enableChapterRename = !enableChapterRename },
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
                                Text("支持正则表达式，使用 $1, $2 等引用捕获组", 
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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

        Spacer(modifier = Modifier.height(16.dp))
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}
