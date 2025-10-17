package com.bandbbs.ebook.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bandbbs.ebook.database.Chapter
import com.bandbbs.ebook.ui.viewmodel.SyncOptionsState
import kotlin.math.min

@Composable
fun SyncOptionsBottomSheet(
    state: SyncOptionsState,
    onCancel: () -> Unit,
    onConfirm: (selectedChapterIndices: Set<Int>) -> Unit
) {
    var selectedChapters by remember { mutableStateOf(setOf<Int>()) }
    var quickSelectText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    
    LaunchedEffect(state.syncedChapters, state.totalChapters, state.chapters) {
        if (state.chapters.isNotEmpty() && selectedChapters.isEmpty()) {
            val startIndex = state.syncedChapters.coerceIn(0, state.totalChapters)
            val maxCount = min(400, state.totalChapters - startIndex)
            val endIndex = (startIndex + maxCount).coerceAtMost(state.totalChapters)
            selectedChapters = (startIndex until endIndex).toSet()
        }
    }

    val selectedCount by remember { derivedStateOf { selectedChapters.size } }
    val isOverLimit by remember { derivedStateOf { selectedChapters.size > 400 } }

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
                    if (!isOverLimit && selectedChapters.isNotEmpty()) {
                        onConfirm(selectedChapters)
                    }
                },
                enabled = !isOverLimit && selectedChapters.isNotEmpty()
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
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
                                color = if (isOverLimit) MaterialTheme.colorScheme.error 
                                       else MaterialTheme.colorScheme.onPrimaryContainer
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

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "快速选择",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = quickSelectText,
                            onValueChange = { quickSelectText = it.filter { c -> c.isDigit() || c == '-' } },
                            label = { Text("输入范围") },
                            placeholder = { Text("如 1-50") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    val range = parseChapterRange(quickSelectText, state.totalChapters)
                                    if (range != null) {
                                        selectedChapters = range
                                        quickSelectText = ""
                                    }
                                }
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        FilledTonalButton(
                            onClick = {
                                focusManager.clearFocus()
                                val range = parseChapterRange(quickSelectText, state.totalChapters)
                                if (range != null) {
                                    selectedChapters = range
                                    quickSelectText = ""
                                }
                            },
                            modifier = Modifier.align(Alignment.CenterVertically),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("应用")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                selectedChapters = (0 until min(400, state.totalChapters)).toSet()
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(12.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("全选")
                        }
                        
                        FilledTonalButton(
                            onClick = { selectedChapters = emptySet() },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(12.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("清空")
                        }
                        
                        FilledTonalButton(
                            onClick = {
                                val startIndex = state.syncedChapters.coerceIn(0, state.totalChapters)
                                val endIndex = min(startIndex + 400, state.totalChapters)
                                selectedChapters = (startIndex until endIndex).toSet()
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(12.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("未读")
                        }
                    }
                }
            }

            if (isOverLimit) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "最多选择 400 章（防止手环内存溢出）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "章节列表 (${state.chapters.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                itemsIndexed(state.chapters, key = { _, chapter -> chapter.id }) { index, chapter ->
                    val isSelected = selectedChapters.contains(index)
                    val isSynced = index < state.syncedChapters
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedChapters = if (isSelected) {
                                    selectedChapters - index
                                } else {
                                    selectedChapters + index
                                }
                            }
                            .background(
                                when {
                                    isSelected -> MaterialTheme.colorScheme.secondaryContainer
                                    isSynced -> MaterialTheme.colorScheme.surfaceContainerHigh
                                    else -> Color.Transparent
                                }
                            )
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
                                    text = "第 ${index + 1} 章",
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
                    
                    if (index < state.chapters.size - 1) {
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
        }

        Spacer(modifier = Modifier.height(16.dp))
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
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
