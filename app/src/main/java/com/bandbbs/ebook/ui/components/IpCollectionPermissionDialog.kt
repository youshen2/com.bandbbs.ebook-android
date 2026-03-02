package com.bandbbs.ebook.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.extra.SuperDialogDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.pressable

@Composable
fun IpCollectionPermissionDialog(
    show: MutableState<Boolean>,
    isFirstTime: Boolean,
    onAllow: () -> Unit,
    onDeny: () -> Unit
) {
    var isSecondConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(show.value) {
        if (!show.value) {
            isSecondConfirmation = false
        }
    }

    val title = if (isSecondConfirmation) "确认禁用更新检测？" else "允许收集IP地址"

    val summary = if (isSecondConfirmation) {
        "如果您选择不允许，我们将无法为您提供版本更新检测功能。您需要自行前往米坛社区寻找更新。\n\n您确定要禁用此功能吗？"
    } else {
        if (isFirstTime) {
            "为了提供版本更新检测功能，我们需要收集您的IP地址用于统计。\n\n是否允许收集IP地址？"
        } else {
            "版本更新检测功能需要收集IP地址。如果您选择不允许，将禁用更新检测功能。\n\n是否允许收集IP地址？"
        }
    }

    SuperDialog(
        show = show,
        title = title,
        summary = summary,
        titleColor = if (isSecondConfirmation) MiuixTheme.colorScheme.error else SuperDialogDefaults.titleColor(),
        onDismissRequest = {
            show.value = false
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (isSecondConfirmation) {
                TextButton(
                    text = "再想想",
                    onClick = { isSecondConfirmation = false },
                    modifier = Modifier
                        .weight(1f)
                        .pressable(interactionSource = null, indication = SinkFeedback())
                )
                Spacer(Modifier.width(20.dp))
                TextButton(
                    text = "确认禁用",
                    onClick = {
                        show.value = false
                        onDeny()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .pressable(interactionSource = null, indication = SinkFeedback())
                )
            } else {
                TextButton(
                    text = "不允许",
                    onClick = { isSecondConfirmation = true },
                    modifier = Modifier
                        .weight(1f)
                        .pressable(interactionSource = null, indication = SinkFeedback())
                )
                Spacer(Modifier.width(20.dp))
                TextButton(
                    text = "允许",
                    onClick = {
                        show.value = false
                        onAllow()
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
