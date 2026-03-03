package com.bandbbs.ebook.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
fun FirstBandExportWarningDialog(
    show: MutableState<Boolean>,
    onConfirm: () -> Unit
) {
    var remainingSeconds by remember { mutableIntStateOf(15) }
    val canConfirm = remainingSeconds <= 0
    val scrollState = rememberScrollState()

    LaunchedEffect(show.value) {
        if (show.value) {
            remainingSeconds = 15
            while (remainingSeconds > 0 && show.value) {
                delay(1000)
                remainingSeconds--
            }
        }
    }

    SuperDialog(
        title = "保存前必读",
        summary = "请务必仔细阅读以下说明后再继续。",
        show = show,
        onDismissRequest = { show.value = false }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.defaultColors(
                    color = MiuixTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "关于自动重启的说明（重要）",
                        style = MiuixTheme.textStyles.subtitle,
                        color = MiuixTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "由于小米 Vela 系统固件的问题，频繁调用文件相关接口有可能导致设备卡死或自动重启。\n传输或使用过程中出现手环自动重启，属于系统限制导致的正常现象，并非本软件 Bug。",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "同步与报错提示",
                    style = MiuixTheme.textStyles.subtitle,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "• File already exist：初次同步出现此报错属正常情况，请直接再尝试一次。",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantActions
                    )
                    Text(
                        text = "• No chapter data：若无法同步，请务必同时在手机和手环端删除该书籍，重新导入并同步。",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantActions
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    text = "取消",
                    onClick = { show.value = false },
                    modifier = Modifier
                        .weight(1f)
                        .pressable(interactionSource = null, indication = SinkFeedback())
                )
                Spacer(modifier = Modifier.width(16.dp))
                TextButton(
                    text = if (canConfirm) "继续保存" else "等待 (${remainingSeconds}s)",
                    onClick = {
                        if (canConfirm) {
                            show.value = false
                            onConfirm()
                        }
                    },
                    enabled = canConfirm,
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    modifier = Modifier
                        .weight(1f)
                        .pressable(interactionSource = null, indication = SinkFeedback())
                )
            }
        }
    }
}
