package com.bandbbs.ebook.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bandbbs.ebook.utils.VersionChecker
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Phone
import top.yukonga.miuix.kmp.icon.extended.Stopwatch
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun UpdateCheckDialog(
    showDialog: MutableState<Boolean>,
    isChecking: Boolean,
    updateInfo: VersionChecker.UpdateInfo?,
    updateInfoList: List<VersionChecker.UpdateInfo> = emptyList(),
    errorMessage: String?,
    deviceName: String?,
    onDismiss: () -> Unit,
    onOpenWebsite: () -> Unit
) {
    val updatesToShow = if (updateInfoList.isNotEmpty()) updateInfoList else listOfNotNull(updateInfo)
    val hasUpdates = updatesToShow.any { it.hasUpdate }

    val dialogTitle = when {
        isChecking -> "检查更新"
        errorMessage != null -> "检查失败"
        hasUpdates -> "发现新版本"
        else -> "已是最新版本"
    }

    SuperDialog(
        title = dialogTitle,
        show = showDialog,
        onDismissRequest = {
            showDialog.value = false
            onDismiss()
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp, max = 350.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = if (isChecking) Alignment.CenterHorizontally else Alignment.Start
                ) {
                    when {
                        isChecking -> {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "正在获取最新版本信息...",
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        errorMessage != null -> {
                            Text(
                                text = errorMessage,
                                style = MiuixTheme.textStyles.body1,
                                color = MiuixTheme.colorScheme.error
                            )
                        }

                        hasUpdates -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                updatesToShow.filter { it.hasUpdate }.forEach { updateInfoItem ->
                                    UpdateItemCard(updateInfoItem = updateInfoItem, deviceName = deviceName)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        else -> {
                            Text(
                                text = if (deviceName != null) "手机端与手环端 ($deviceName) 均为最新，无需更新。" else "当前应用已是最新版本，无需更新。",
                                style = MiuixTheme.textStyles.body1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (isChecking) {
                    TextButton(
                        text = "取消",
                        onClick = {
                            showDialog.value = false
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    )
                } else if (hasUpdates) {
                    TextButton(
                        text = "稍后",
                        onClick = {
                            showDialog.value = false
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    TextButton(
                        text = "前往下载",
                        onClick = {
                            showDialog.value = false
                            onDismiss()
                            onOpenWebsite()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                } else {
                    TextButton(
                        text = "确定",
                        onClick = {
                            showDialog.value = false
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdateItemCard(
    updateInfoItem: VersionChecker.UpdateInfo,
    deviceName: String?
) {
    val isAndroid = updateInfoItem.deviceType == "android"
    val title = if (isAndroid) "手机端更新" else "手环端更新"
    val icon = if (isAndroid) MiuixIcons.Phone else MiuixIcons.Stopwatch
    val subtitle = if (!isAndroid && deviceName != null) deviceName else if (isAndroid) "Android App" else null

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.secondaryContainer
        ),
        cornerRadius = 16.dp
    ) {
        BasicComponent(
            title = title,
            summary = subtitle,
            startAction = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 12.dp)
                )
            },
            endActions = {
                Text(
                    text = updateInfoItem.versionName,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.primary
                )
            }
        )

        if (updateInfoItem.updateLog.isNotEmpty()) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MiuixTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "更新内容：",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
                updateInfoItem.updateLog.forEach { log ->
                    Row(
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text(
                            text = "• ",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                        Text(
                            text = log,
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
