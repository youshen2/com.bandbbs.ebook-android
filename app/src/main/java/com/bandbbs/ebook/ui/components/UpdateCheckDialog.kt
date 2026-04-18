package com.bandbbs.ebook.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bandbbs.ebook.utils.VersionChecker
import top.yukonga.miuix.kmp.basic.*
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
    onOpenWebsite: () -> Unit,
    onRetry: (() -> Unit)? = null
) {
    val updatesToShow =
        if (updateInfoList.isNotEmpty()) updateInfoList else listOfNotNull(updateInfo)
    val hasUpdates = updatesToShow.any { it.hasUpdate }

    val dialogState = when {
        isChecking -> UpdateDialogState.Checking
        errorMessage != null -> UpdateDialogState.Error(errorMessage)
        hasUpdates -> UpdateDialogState.HasUpdates
        else -> UpdateDialogState.UpToDate
    }

    SuperDialog(
        show = showDialog,
        onDismissRequest = {
            showDialog.value = false
            onDismiss()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
        ) {
            UpdateDialogHeader(dialogState = dialogState)

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp, max = 380.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (dialogState) {
                        UpdateDialogState.Checking -> {
                            CheckingContent()
                        }

                        is UpdateDialogState.Error -> {
                            ErrorContent(errorMessage = dialogState.message)
                        }

                        UpdateDialogState.HasUpdates -> {
                            UpdateListContent(
                                updatesToShow = updatesToShow,
                                deviceName = deviceName
                            )
                        }

                        UpdateDialogState.UpToDate -> {
                            UpToDateContent(deviceName = deviceName)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ActionArea(
                state = dialogState,
                onDismiss = {
                    showDialog.value = false
                    onDismiss()
                },
                onOpenWebsite = {
                    showDialog.value = false
                    onDismiss()
                    onOpenWebsite()
                },
                onRetry = onRetry
            )
        }
    }
}

private sealed class UpdateDialogState {
    data object Checking : UpdateDialogState()
    data class Error(val message: String) : UpdateDialogState()
    data object HasUpdates : UpdateDialogState()
    data object UpToDate : UpdateDialogState()
}

@Composable
private fun UpdateDialogHeader(dialogState: UpdateDialogState) {
    val title = when (dialogState) {
        UpdateDialogState.Checking -> "正在检查更新"
        is UpdateDialogState.Error -> "检查更新失败"
        UpdateDialogState.HasUpdates -> "发现新版本"
        UpdateDialogState.UpToDate -> "已是最新版本"
    }

    val subtitle = when (dialogState) {
        UpdateDialogState.Checking -> "正在连接服务器获取版本信息"
        is UpdateDialogState.Error -> "未能获取更新信息，请稍后重试"
        UpdateDialogState.HasUpdates -> "建议尽快升级以获得新功能和优化"
        UpdateDialogState.UpToDate -> "你正在使用最新版本，无需额外操作"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MiuixTheme.textStyles.title2,
                color = MiuixTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
        }
    }
}

@Composable
private fun CheckingContent() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.size(34.dp))
        Text(
            text = "正在获取最新版本信息...",
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
    }
}

@Composable
private fun ErrorContent(errorMessage: String) {
    Text(
        text = errorMessage,
        style = MiuixTheme.textStyles.body1,
        color = MiuixTheme.colorScheme.error
    )
}

@Composable
private fun UpToDateContent(deviceName: String?) {
    Text(
        text = if (deviceName != null) {
            "手机端与手环端（$deviceName）均为最新，无需更新。"
        } else {
            "当前应用已是最新版本，无需更新。"
        },
        style = MiuixTheme.textStyles.body1,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
    )
}

@Composable
private fun UpdateListContent(
    updatesToShow: List<VersionChecker.UpdateInfo>,
    deviceName: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        updatesToShow.filter { it.hasUpdate }.forEach { updateInfoItem ->
            UpdateItemCard(
                updateInfoItem = updateInfoItem,
                deviceName = deviceName
            )
        }
    }
}

@Composable
private fun ActionArea(
    state: UpdateDialogState,
    onDismiss: () -> Unit,
    onOpenWebsite: () -> Unit,
    onRetry: (() -> Unit)?
) {
    when (state) {
        UpdateDialogState.Checking -> {
            TextButton(
                text = "取消",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }

        is UpdateDialogState.Error -> {
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    text = "关闭",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                if (onRetry != null) {
                    Spacer(modifier = Modifier.width(12.dp))
                    TextButton(
                        text = "重试",
                        onClick = onRetry,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                }
            }
        }

        UpdateDialogState.HasUpdates -> {
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    text = "稍后",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                TextButton(
                    text = "去下载",
                    onClick = onOpenWebsite,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary()
                )
            }
        }

        UpdateDialogState.UpToDate -> {
            TextButton(
                text = "确定",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColorsPrimary()
            )
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
    val subtitle = when {
        !isAndroid && deviceName != null -> deviceName
        isAndroid -> "Android App"
        else -> null
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = MiuixTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
        ),
        cornerRadius = 18.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = updateInfoItem.versionName,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.primary
                    )
                }
            }

            if (updateInfoItem.updateLog.isNotEmpty()) {
                HorizontalDivider(
                    color = MiuixTheme.colorScheme.outline.copy(alpha = 0.2f)
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "更新内容",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )

                    updateInfoItem.updateLog.take(5).forEach { log ->
                        Row {
                            Text(
                                text = "•",
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = log,
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurface,
                                overflow = TextOverflow.Clip
                            )
                        }
                    }

                    if (updateInfoItem.updateLog.size > 5) {
                        Text(
                            text = "还有 ${updateInfoItem.updateLog.size - 5} 条更新内容...",
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    }
                }
            }
        }
    }
}
