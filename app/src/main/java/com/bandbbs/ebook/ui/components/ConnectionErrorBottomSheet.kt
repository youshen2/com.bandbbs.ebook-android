package com.bandbbs.ebook.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ConnectionErrorBottomSheet(
    deviceName: String?,
    isUnsupportedDevice: Boolean,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MiuixTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isUnsupportedDevice) MiuixIcons.Close else MiuixIcons.Info,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MiuixTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isUnsupportedDevice) "设备不受支持" else "连接失败",
            style = MiuixTheme.textStyles.title2,
            color = MiuixTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (isUnsupportedDevice) "当前设备：${deviceName ?: "未知设备"}" else "请检查以下项目以恢复连接",
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isUnsupportedDevice) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SmallTitle(text = "不支持的设备列表")
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
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                SmallTitle(text = "排查步骤")
                Card(modifier = Modifier.fillMaxWidth()) {
                    CheckItem(
                        number = "1",
                        title = "APP 后台运行",
                        description = "确保小米运动健康 APP 已打开并在后台保留"
                    )
                    CheckItem(
                        number = "2",
                        title = "系统权限设置",
                        description = "在系统设置中，为小米运动健康开启后台无限制运行权限"
                    )
                    CheckItem(
                        number = "3",
                        title = "手环连接状态",
                        description = "在小米运动健康中确认手环处于已连接状态"
                    )
                    CheckItem(
                        number = "4",
                        title = "手环端小程序",
                        description = "确保手环上已成功安装并打开弦电子书小程序",
                        isImportant = true
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = onDismiss,
                modifier = if (isUnsupportedDevice) Modifier.fillMaxWidth() else Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors()
            ) {
                Text("知道了")
            }

            if (!isUnsupportedDevice) {
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColorsPrimary()
                ) {
                    Text(
                        text = "重试连接",
                        color = MiuixTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckItem(
    number: String,
    title: String,
    description: String,
    isImportant: Boolean = false
) {
    BasicComponent(
        title = title,
        summary = description,
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        startAction = {
            Box(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (isImportant) MiuixTheme.colorScheme.errorContainer
                        else MiuixTheme.colorScheme.secondaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number,
                    style = MiuixTheme.textStyles.subtitle,
                    color = if (isImportant) MiuixTheme.colorScheme.error
                    else MiuixTheme.colorScheme.onBackgroundVariant
                )
            }
        }
    )
}
