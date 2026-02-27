package com.bandbbs.ebook.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bandbbs.ebook.ui.viewmodel.PushState
import com.bandbbs.ebook.utils.bytesToReadable
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun PushBottomSheet(
    pushState: PushState,
    onCancel: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = pushState.progress.toFloat(),
        label = "ProgressAnimation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = pushState.book?.name ?: "未知文件",
            style = MiuixTheme.textStyles.title3.copy(fontWeight = FontWeight.Medium)
        )
        Text(
            text = bytesToReadable(pushState.book?.size ?: 0),
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            colors = CardDefaults.defaultColors(
                color = MiuixTheme.colorScheme.secondaryVariant.copy(alpha = 0.5f)
            )
        ) {
            val listState = rememberLazyListState()
            val logEntries = if (pushState.transferLog.isEmpty()) {
                listOf(pushState.preview)
            } else {
                pushState.transferLog
            }

            LaunchedEffect(logEntries.size) {
                if (logEntries.isNotEmpty()) {
                    listState.animateScrollToItem(logEntries.size - 1)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(logEntries) { logEntry ->
                    Text(
                        text = logEntry,
                        style = MiuixTheme.textStyles.footnote2.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = pushState.statusText,
                style = MiuixTheme.textStyles.body1.copy(fontWeight = FontWeight.Medium)
            )
            Text(
                text = pushState.speed,
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = animatedProgress,
            modifier = Modifier.fillMaxWidth()
        )

        if (pushState.isSendingCover && pushState.coverProgress.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = pushState.coverProgress,
                style = MiuixTheme.textStyles.footnote1.copy(fontWeight = FontWeight.Medium),
                color = MiuixTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
            colors = if (pushState.isFinished) ButtonDefaults.buttonColorsPrimary() else ButtonDefaults.buttonColors()
        ) {
            Text(if (pushState.isFinished) "完成" else "取消")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}
