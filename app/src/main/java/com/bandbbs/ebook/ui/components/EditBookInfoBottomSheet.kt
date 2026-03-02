package com.bandbbs.ebook.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bandbbs.ebook.database.BookEntity
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.pressable

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
    isResyncing: Boolean = false,
    onSaveBeforeResync: (suspend (BookEntity) -> Unit)? = null
) {
    var bookName by remember { mutableStateOf(book.name) }
    var author by remember { mutableStateOf(book.author ?: "") }
    var summary by remember { mutableStateOf(book.summary ?: "") }
    var bookStatus by remember { mutableStateOf(book.bookStatus ?: "") }
    var category by remember { mutableStateOf(book.category ?: "") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(book) {
        bookName = book.name
        author = book.author ?: ""
        summary = book.summary ?: ""
        bookStatus = book.bookStatus ?: ""
        category = book.category ?: ""
        onLocalCategoryChanged(book.localCategory ?: "")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(top = 4.dp)
    ) {
        Text(
            text = "编辑书籍信息",
            style = MiuixTheme.textStyles.title2,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        SmallTitle(text = "基础信息")

        TextField(
            value = bookName,
            onValueChange = { bookName = it },
            label = "书名",
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        TextField(
            value = author,
            onValueChange = { author = it },
            label = "作者",
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        TextField(
            value = bookStatus,
            onValueChange = { bookStatus = it },
            label = "状态",
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            useLabelAsPlaceholder = false
        )

        Spacer(modifier = Modifier.height(12.dp))

        TextField(
            value = category,
            onValueChange = { category = it },
            label = "分类/标签",
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            useLabelAsPlaceholder = false
        )

        Spacer(modifier = Modifier.height(12.dp))

        SmallTitle(text = "本地分类")

        Card(
            colors = CardDefaults.defaultColors(
                color = MiuixTheme.colorScheme.secondaryVariant
            )
        ) {
            SuperArrow(
                title = localCategory.ifBlank { "未分类" },
                onClick = onShowCategorySelector
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        SmallTitle(text = "简介")

        TextField(
            value = summary,
            onValueChange = { summary = it },
            label = "简介",
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            maxLines = 8
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                scope.launch {
                    if (bookName.isNotBlank()) {
                        val updatedBook = book.copy(
                            name = bookName.trim(),
                            author = author.takeIf { it.isNotBlank() },
                            summary = summary.takeIf { it.isNotBlank() },
                            bookStatus = bookStatus.takeIf { it.isNotBlank() },
                            category = category.takeIf { it.isNotBlank() },
                            localCategory = localCategory.takeIf { it.isNotBlank() }
                        )

                        if (onSaveBeforeResync != null) {
                            onSaveBeforeResync(updatedBook)
                        } else {
                            onSave(updatedBook)
                        }
                    }
                    onResyncInfo()
                }
            },
            enabled = !isResyncing,
            modifier = Modifier
                .fillMaxWidth()
                .pressable(interactionSource = null, indication = SinkFeedback())
        ) {
            if (isResyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(end = 8.dp),
                    size = 20.dp,
                    strokeWidth = 3.dp
                )
                Text("同步中...")
            } else {
                Text("重新同步详情")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                text = "取消",
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .pressable(interactionSource = null, indication = SinkFeedback())
            )
            Spacer(Modifier.width(16.dp))
            TextButton(
                text = "保存",
                colors = ButtonDefaults.textButtonColorsPrimary(),
                enabled = bookName.isNotBlank(),
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
                modifier = Modifier
                    .weight(1f)
                    .pressable(interactionSource = null, indication = SinkFeedback())
            )
        }

        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}
