package com.bandbbs.ebook.ui.components

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
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
fun VersionIncompatibleDialog(
    show: MutableState<Boolean>,
    currentVersion: Int,
    requiredVersion: Int,
    requiredVersionName: String,
    onDismiss: () -> Unit = {},
    onOpenWebsite: () -> Unit
) {
    val context = LocalContext.current
    val dismiss = {
        show.value = false
        onDismiss()
    }

    BackHandler(enabled = show.value) {
        // 拦截返回键，防止用户绕开弹窗
    }

    SuperDialog(
        title = "版本不兼容",
        titleColor = MiuixTheme.colorScheme.error,
        show = show,
        onDismissRequest = { /* 禁止点击外部取消 */ }
    ) {
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
                        text = "手环端版本过低，无法继续操作",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.defaultColors(
                    color = MiuixTheme.colorScheme.secondaryVariant.copy(alpha = 0.5f)
                ),
                cornerRadius = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "版本详情",
                        style = MiuixTheme.textStyles.subtitle,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "当前手环端版本号：$currentVersion",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantActions
                    )
                    Text(
                        text = "所需最低版本：$requiredVersionName (版本号: $requiredVersion)",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "请在手环上将弦电子书小程序更新到最新版本后再使用。",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.8f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    text = "退出应用",
                    onClick = {
                        val activity = context as? Activity
                        activity?.finish()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .pressable(interactionSource = null, indication = SinkFeedback())
                )
                Spacer(Modifier.width(16.dp))
                TextButton(
                    text = "前往官网下载",
                    onClick = {
                        dismiss()
                        onOpenWebsite()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .pressable(interactionSource = null, indication = SinkFeedback()),
                    colors = ButtonDefaults.textButtonColorsPrimary()
                )
            }
        }
    }
}
