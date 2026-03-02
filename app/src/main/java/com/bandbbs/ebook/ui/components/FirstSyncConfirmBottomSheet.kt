package com.bandbbs.ebook.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.pressable

@Composable
fun FirstSyncConfirmDialog(
    show: MutableState<Boolean>,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    var countdown by remember { mutableIntStateOf(10) }
    val isCountingDown = countdown > 0

    LaunchedEffect(show.value) {
        if (show.value) {
            countdown = 10
            while (countdown > 0) {
                delay(1000)
                countdown--
            }
        }
    }

    val dismiss = {
        show.value = false
        onCancel()
    }

    SuperDialog(
        title = "同步确认",
        titleColor = MiuixTheme.colorScheme.error,
        show = show,
        onDismissRequest = dismiss
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                colors = CardDefaults.defaultColors(
                    color = MiuixTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "由于Vela优化问题，同步书籍时手环重启为正常现象，开机后继续同步即可。\n首次同步报错为正常现象。\n若某书籍同步一直报错，可删除书籍重新导入再试。",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isCountingDown) {
                Text(
                    text = "请仔细阅读以上内容（${countdown}秒后可继续）",
                    style = MiuixTheme.textStyles.footnote1.copy(fontWeight = FontWeight.Medium),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    text = "取消",
                    onClick = dismiss,
                    modifier = Modifier
                        .weight(1f)
                        .pressable(interactionSource = null, indication = SinkFeedback())
                )
                Spacer(Modifier.width(20.dp))
                TextButton(
                    text = if (isCountingDown) "确认 ($countdown)" else "确认",
                    onClick = {
                        show.value = false
                        onConfirm()
                    },
                    enabled = !isCountingDown,
                    modifier = Modifier
                        .weight(1f)
                        .pressable(interactionSource = null, indication = SinkFeedback()),
                    colors = ButtonDefaults.textButtonColorsPrimary()
                )
            }
        }
    }
}
