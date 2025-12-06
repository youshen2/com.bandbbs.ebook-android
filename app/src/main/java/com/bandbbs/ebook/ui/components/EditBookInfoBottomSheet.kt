package com.bandbbs.ebook.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bandbbs.ebook.database.BookEntity

@Composable
fun EditBookInfoBottomSheet(
    book: BookEntity,
    categories: List<String>,
    localCategory: String,
    onLocalCategoryChanged: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: (BookEntity) -> Unit,
    onShowCategorySelector: () -> Unit,
    onResyncInfo: () -> Unit,
    isResyncing: Boolean = false
) {
    var bookName by remember { mutableStateOf(book.name) }
    var author by remember { mutableStateOf(book.author ?: "") }
    var summary by remember { mutableStateOf(book.summary ?: "") }
    var bookStatus by remember { mutableStateOf(book.bookStatus ?: "") }
    var category by remember { mutableStateOf(book.category ?: "") }

    
    LaunchedEffect(book) {
        bookName = book.name
        author = book.author ?: ""
        summary = book.summary ?: ""
        bookStatus = book.bookStatus ?: ""
        category = book.category ?: ""
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
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel) {
                Text("取消")
            }
            Text(
                "编辑书籍信息",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(
                onClick = {
                    onSave(
                        book.copy(
                            name = bookName.trim(),
                            author = author.takeIf { it.isNotBlank() },
                            summary = summary.takeIf { it.isNotBlank() },
                            bookStatus = bookStatus.takeIf { it.isNotBlank() },
                            category = category.takeIf { it.isNotBlank() },
                            localCategory = localCategory.takeIf { it.isNotBlank() }
                        )
                    )
                },
                enabled = bookName.isNotBlank()
            ) {
                Text("保存")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = bookName,
            onValueChange = { bookName = it },
            label = { Text("书名") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = author,
            onValueChange = { author = it },
            label = { Text("作者") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = bookStatus,
            onValueChange = { bookStatus = it },
            label = { Text("状态") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            placeholder = { Text("例如：已完结、连载中") }
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("分类/标签") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            placeholder = { Text("例如：已完结 | 动漫衍生 | 穿越") }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "本地分类",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            onClick = onShowCategorySelector,
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
                    text = localCategory.ifBlank { "未分类" },
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

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = summary,
            onValueChange = { summary = it },
            label = { Text("简介") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            maxLines = 8,
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onResyncInfo,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isResyncing
        ) {
            if (isResyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("同步中...")
            } else {
                Text("重新同步详情")
            }
        }

        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

