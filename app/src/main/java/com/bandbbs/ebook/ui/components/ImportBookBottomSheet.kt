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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportBookBottomSheet(
    state: ImportState,
    onCancel: () -> Unit,
    onConfirm: (bookName: String, splitMethod: String, noSplit: Boolean) -> Unit
) {
    var bookName by remember { mutableStateOf(state.bookName) }
    var splitMethod by remember { mutableStateOf(state.splitMethod) }
    var showSplitMethodMenu by remember { mutableStateOf(false) }
    var noSplit by remember { mutableStateOf(state.noSplit) }

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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Book,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "导入书籍",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            TextButton(
                onClick = { onConfirm(bookName, splitMethod, noSplit) },
                enabled = bookName.isNotBlank()
            ) {
                Text("导入")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

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
                    text = "书籍信息",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = bookName,
                    onValueChange = { bookName = it },
                    label = { Text("书名") },
                    placeholder = { Text("请输入书籍名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
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
                    text = "章节设置",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { noSplit = !noSplit }
                        .border(
                            width = if (noSplit) 2.dp else 1.dp,
                            color = if (noSplit) MaterialTheme.colorScheme.primary 
                                   else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    color = if (noSplit) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                               else MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (noSplit) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = if (noSplit) MaterialTheme.colorScheme.primary 
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "不分章",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (noSplit) FontWeight.Medium else FontWeight.Normal,
                                color = if (noSplit) MaterialTheme.colorScheme.onPrimaryContainer
                                       else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "将整本书作为单个章节导入",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                                shape = RoundedCornerShape(12.dp),
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
                    
                    if (!noSplit) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "系统将根据所选方式自动识别章节标题并分章",
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
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (state.fileFormat == "epub") "EPUB 格式自带章节信息，将按原有章节导入" else "NVB 格式自带章节信息，将按原有章节导入",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}
