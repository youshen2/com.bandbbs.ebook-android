package com.bandbbs.ebook.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bandbbs.ebook.database.ChapterInfo
import com.bandbbs.ebook.ui.viewmodel.PushState
import com.bandbbs.ebook.utils.bytesToReadable
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.pressable

@Composable
fun PushScreen(
    pushState: PushState,
    onBackClick: () -> Unit,
    onCancelOrDone: () -> Unit
) {
    BackHandler { onBackClick() }

    val animatedProgress by animateFloatAsState(
        targetValue = pushState.progress.toFloat(),
        label = "PushProgressAnimation"
    )

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "推送进度",
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = "返回",
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onCancelOrDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                        .pressable(interactionSource = null, indication = SinkFeedback()),
                    colors = if (pushState.isFinished) {
                        ButtonDefaults.buttonColorsPrimary()
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    Text(
                        text = if (pushState.isFinished) "完成" else "取消",
                        color = if (pushState.isFinished) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSecondaryVariant
                    )
                }
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        },
        popupHost = {}
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                insideMargin = PaddingValues(0.dp)
            ) {
                BasicComponent(
                    title = pushState.book?.name ?: "未知文件",
                    summary = bytesToReadable(pushState.book?.size ?: 0)
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = pushState.statusText,
                            style = MiuixTheme.textStyles.body1.copy(fontWeight = FontWeight.Medium),
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )
                        Text(
                            text = pushState.speed,
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    }

                    LinearProgressIndicator(
                        progress = animatedProgress,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (pushState.isSendingCover && pushState.coverProgress.isNotEmpty()) {
                        Text(
                            text = pushState.coverProgress,
                            style = MiuixTheme.textStyles.footnote1.copy(fontWeight = FontWeight.Medium),
                            color = MiuixTheme.colorScheme.primary
                        )
                    }
                }
            }

            SmallTitle(text = "传输日志")
            PushLogCard(
                transferLog = pushState.transferLog,
                preview = pushState.preview
            )

            SmallTitle(text = "章节详情")
            PushTransferSummary(
                currentIndex = pushState.currentChapterIndex,
                chapters = pushState.transferChapters
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PushLogCard(
    transferLog: List<String>,
    preview: String
) {
    val listState = rememberLazyListState()
    val logEntries = if (transferLog.isEmpty()) listOf(preview) else transferLog

    LaunchedEffect(logEntries.size) {
        if (logEntries.isNotEmpty()) {
            listState.animateScrollToItem(logEntries.size - 1)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .height(140.dp),
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.secondaryVariant.copy(alpha = 0.5f)
        )
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxHeight()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(logEntries) { _, logEntry ->
                Text(
                    text = logEntry,
                    style = MiuixTheme.textStyles.footnote2.copy(fontFamily = FontFamily.Monospace),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PushTransferSummary(
    currentIndex: Int?,
    chapters: List<ChapterInfo>
) {
    val total = chapters.size
    val currentPos = currentIndex?.let { idx ->
        chapters.indexOfFirst { it.index == idx }.takeIf { it >= 0 }?.plus(1)
    }
    val currentChapter = currentIndex?.let { idx -> chapters.firstOrNull { it.index == idx } }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .height(200.dp),
        insideMargin = PaddingValues(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (currentChapter != null) {
                        "${currentChapter.index + 1}. ${currentChapter.name}"
                    } else if (total == 0) {
                        "未选择章节"
                    } else {
                        "准备中..."
                    },
                    style = MiuixTheme.textStyles.body1.copy(fontWeight = FontWeight.Medium),
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                Text(
                    text = if (total > 0 && currentPos != null) "$currentPos/$total" else "--/--",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            val listState = rememberLazyListState()
            LaunchedEffect(currentIndex, chapters.size) {
                val idx = currentIndex?.let { target ->
                    chapters.indexOfFirst { it.index == target }
                } ?: -1
                if (idx >= 0) listState.scrollToItem(idx)
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                itemsIndexed(chapters, key = { _, ch -> ch.id }) { _, chapter ->
                    val isCurrent = currentIndex != null && chapter.index == currentIndex
                    BasicComponent(
                        title = "${chapter.index + 1}. ${chapter.name}",
                        titleColor = if (isCurrent) {
                            BasicComponentDefaults.titleColor(color = MiuixTheme.colorScheme.primary)
                        } else {
                            BasicComponentDefaults.titleColor()
                        }
                    )
                }
            }
        }
    }
}
