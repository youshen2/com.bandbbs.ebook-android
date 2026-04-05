package com.bandbbs.ebook.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.pressable

@Composable
fun ConnectionErrorDialog(
    show: MutableState<Boolean>,
    deviceName: String?,
    isUnsupportedDevice: Boolean,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    val context = LocalContext.current
    SuperDialog(
        title = if (isUnsupportedDevice) "设备不受支持" else "连接失败",
        summary = if (isUnsupportedDevice) "当前设备：${deviceName ?: "未知设备"}" else "请点击下方按钮查看疑难杂症文档排查问题。",
        show = show,
        onDismissRequest = {
            show.value = false
            onDismiss()
        }
    ) {
        if (isUnsupportedDevice) {
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    BasicComponent(
                        title = "小米手环 8",
                        summary = "及更早发布的旧款设备"
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    BasicComponent(
                        title = "解决方案",
                        summary = "请使用小米手环 8 Pro、小米手环 9 等支持的较新设备进行连接。"
                    )
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.defaultColors(
                    color = MiuixTheme.colorScheme.errorContainer,
                    contentColor = MiuixTheme.colorScheme.onErrorContainer
                ),
                cornerRadius = 16.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = MiuixIcons.Info,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MiuixTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "未能成功连接到弦电子书手环端，请检查小米运动健康状态。",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.width(8.dp))

                TextButton(
                    text = "查看文档",
                    onClick = {
                        try {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://docs.luoxe.cn/docs/sine/book/question/")
                            )
                            context.startActivity(intent)
                        } catch (e: Exception) {
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .pressable(interactionSource = null, indication = SinkFeedback())
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    text = "重试",
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    onClick = {
                        show.value = false
                        onRetry()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .pressable(interactionSource = null, indication = SinkFeedback())
                )
            }
        }
    }
}
