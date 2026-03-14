package com.bandbbs.ebook.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bandbbs.ebook.ui.viewmodel.SyncReadingDataState
import com.bandbbs.ebook.ui.viewmodel.SyncResultState
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SyncReadingDataDialog(
    show: MutableState<Boolean>,
    state: SyncReadingDataState,
    resultState: SyncResultState? = null,
    onDismiss: () -> Unit
) {
    val isFailed = state.statusText.contains("失败", ignoreCase = true)
    val displayStatusText = when {
        state.isSyncing -> "正在同步中，请稍候..."
        resultState != null && resultState.changedBooks.isNotEmpty() -> {
            val base = state.statusText.ifBlank { "同步完成" }
            "$base，其中 ${resultState.changedBooks.size} 本有数据变化"
        }
        else -> state.statusText
    }

    SuperDialog(
        show = show,
        title = if (resultState != null && !state.isSyncing) "同步结果" else "同步阅读数据",
        summary = displayStatusText,
        summaryColor = if (isFailed) MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.onSurfaceVariantSummary,
        onDismissRequest = {
            if (!state.isSyncing) {
                show.value = false
                onDismiss()
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.isSyncing && state.totalBooks > 0) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = state.progress,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${state.syncedBooks}/${state.totalBooks}",
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantActions
                        )
                        Text(
                            text = "${(state.progress * 100).toInt()}%",
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantActions
                        )
                    }

                    if (state.currentBook.isNotEmpty()) {
                        Text(
                            text = "正在同步: ${state.currentBook}",
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (!state.isSyncing && (state.failedBooks.isNotEmpty() || (resultState != null && resultState.changedBooks.isNotEmpty()))) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.failedBooks.isNotEmpty()) {
                        var isExpanded by remember { mutableStateOf(false) }
                        val maxVisibleItems = 3
                        val failedBooksList = state.failedBooks.entries.toList()
                        val visibleItems = if (isExpanded) failedBooksList else failedBooksList.take(maxVisibleItems)
                        val hasMore = failedBooksList.size > maxVisibleItems

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.defaultColors(
                                color = MiuixTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                            ),
                            insideMargin = PaddingValues(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "失败书籍 (${state.failedBooks.size})",
                                        style = MiuixTheme.textStyles.subtitle,
                                        color = MiuixTheme.colorScheme.onErrorContainer
                                    )
                                    if (hasMore) {
                                        Text(
                                            text = if (isExpanded) "收起" else "展开全部",
                                            style = MiuixTheme.textStyles.body2,
                                            color = MiuixTheme.colorScheme.primary,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }
                                visibleItems.forEach { (bookName, reason) ->
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = bookName,
                                            style = MiuixTheme.textStyles.body1,
                                            color = MiuixTheme.colorScheme.onErrorContainer,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = reason,
                                            style = MiuixTheme.textStyles.footnote1,
                                            color = MiuixTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (resultState != null && resultState.changedBooks.isNotEmpty()) {
                        Column {
                            SmallTitle(
                                text = "数据变化的书籍",
                                insideMargin = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            )
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.defaultColors(
                                    color = MiuixTheme.colorScheme.secondaryVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    resultState.changedBooks.forEach { bookName ->
                                        BasicComponent(
                                            title = bookName,
                                            startAction = {
                                                Icon(
                                                    imageVector = MiuixIcons.Ok,
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .padding(end = 12.dp)
                                                        .size(18.dp),
                                                    tint = MiuixTheme.colorScheme.primary
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                if (state.isSyncing) {
                    TextButton(
                        text = "取消",
                        onClick = {
                            show.value = false
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    TextButton(
                        text = "确定",
                        onClick = {
                            show.value = false
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                }
            }
        }
    }
}
