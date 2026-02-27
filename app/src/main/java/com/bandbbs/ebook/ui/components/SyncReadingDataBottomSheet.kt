package com.bandbbs.ebook.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp
import com.bandbbs.ebook.ui.viewmodel.SyncReadingDataState
import com.bandbbs.ebook.ui.viewmodel.SyncResultState
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.extra.SuperBottomSheet
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SyncReadingDataBottomSheet(
    show: MutableState<Boolean>,
    state: SyncReadingDataState,
    resultState: SyncResultState? = null,
    onDismiss: () -> Unit
) {
    SuperBottomSheet(
        show = show,
        title = if (resultState != null && !state.isSyncing) "同步结果" else "同步阅读数据",
        onDismissRequest = {
            show.value = false
            onDismiss()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val isFailed = state.statusText.contains("失败", ignoreCase = true)
            val statusContainerColor = when {
                state.isSyncing -> MiuixTheme.colorScheme.primaryContainer
                isFailed -> MiuixTheme.colorScheme.errorContainer
                else -> MiuixTheme.colorScheme.primaryContainer
            }
            val statusContentColor =
                if (isFailed) MiuixTheme.colorScheme.onErrorContainer else MiuixTheme.colorScheme.onPrimaryContainer
            val displayStatusText = when {
                state.isSyncing -> "正在同步中..."
                resultState != null && resultState.changedBooks.isNotEmpty() -> {
                    val base = state.statusText.ifBlank { "同步完成" }
                    "$base，其中 ${resultState.changedBooks.size} 本有数据变化"
                }

                else -> state.statusText
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.defaultColors(
                    color = statusContainerColor,
                    contentColor = statusContentColor
                ),
                insideMargin = PaddingValues(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when {
                        state.isSyncing -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        isFailed -> {
                            Icon(
                                imageVector = MiuixIcons.Info,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = statusContentColor
                            )
                        }

                        else -> {
                            Icon(
                                imageVector = MiuixIcons.Ok,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = statusContentColor
                            )
                        }
                    }

                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        Text(
                            text = "同步阅读数据",
                            style = MiuixTheme.textStyles.title3,
                            color = statusContentColor
                        )
                        Text(
                            text = displayStatusText,
                            style = MiuixTheme.textStyles.body2,
                            color = statusContentColor.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            if (state.isSyncing || state.totalBooks > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.defaultColors(
                        color = MiuixTheme.colorScheme.secondaryVariant.copy(alpha = 0.5f)
                    ),
                    insideMargin = PaddingValues(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (state.isSyncing && state.totalBooks > 0) {
                            Text(
                                text = "同步进度",
                                style = MiuixTheme.textStyles.subtitle,
                                color = MiuixTheme.colorScheme.onSurface
                            )
                            LinearProgressIndicator(
                                progress = state.progress,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ProgressIndicatorDefaults.progressIndicatorColors(
                                    foregroundColor = MiuixTheme.colorScheme.primary
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${state.syncedBooks}/${state.totalBooks}",
                                    style = MiuixTheme.textStyles.body2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantActions
                                )
                                Text(
                                    text = "${(state.progress * 100).toInt()}%",
                                    style = MiuixTheme.textStyles.body2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantActions
                                )
                            }
                            if (state.currentBook.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = MiuixIcons.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MiuixTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "正在同步: ${state.currentBook}",
                                        style = MiuixTheme.textStyles.footnote1,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantActions
                                    )
                                }
                            }
                        } else if (state.totalBooks > 0) {
                            Text(
                                text = "同步完成",
                                style = MiuixTheme.textStyles.subtitle,
                                color = MiuixTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "共同步 ${state.syncedBooks} 本书",
                                style = MiuixTheme.textStyles.body1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantActions
                            )
                        }
                    }
                }
            }

            if (state.failedBooks.isNotEmpty() && !state.isSyncing) {
                var isExpanded by remember { mutableStateOf(false) }
                val maxVisibleItems = 3
                val failedBooksList = state.failedBooks.entries.toList()
                val visibleItems =
                    if (isExpanded) failedBooksList else failedBooksList.take(maxVisibleItems)
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
                                TextButton(
                                    text = if (isExpanded) "收起" else "展开全部",
                                    onClick = { isExpanded = !isExpanded }
                                )
                            }
                        }
                        visibleItems.forEach { (bookName, reason) ->
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = bookName,
                                    style = MiuixTheme.textStyles.body1,
                                    color = MiuixTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = reason,
                                    style = MiuixTheme.textStyles.body2,
                                    color = MiuixTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            if (!state.isSyncing && resultState != null && resultState.changedBooks.isNotEmpty()) {
                SmallTitle(text = "数据变化的书籍")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.defaultColors(
                        color = MiuixTheme.colorScheme.secondaryVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
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

            Button(
                onClick = {
                    show.value = false
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = if (!state.isSyncing) ButtonDefaults.buttonColorsPrimary() else ButtonDefaults.buttonColors()
            ) {
                Text(if (!state.isSyncing) "确定" else "取消")
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
