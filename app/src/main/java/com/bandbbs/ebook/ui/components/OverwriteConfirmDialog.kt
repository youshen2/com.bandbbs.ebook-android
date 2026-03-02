package com.bandbbs.ebook.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bandbbs.ebook.ui.model.Book
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.pressable

@Composable
fun OverwriteConfirmDialog(
    show: MutableState<Boolean>,
    existingBook: Book,
    newBookName: String,
    onCancel: () -> Unit,
    onOverwrite: () -> Unit
) {
    SuperDialog(
        title = "重复书籍",
        summary = "已存在名为「$newBookName」的书籍。\n覆盖将会删除现有书籍及其所有章节数据，此操作不可恢复。",
        show = show,
        onDismissRequest = {
            show.value = false
            onCancel()
        }
    ) {
        Card(
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            BasicComponent(
                title = "现有章节数",
                endActions = {
                    Text(
                        text = "${existingBook.chapterCount}",
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantActions
                    )
                }
            )
            BasicComponent(
                title = "现有字数",
                endActions = {
                    Text(
                        text = "${existingBook.wordCount}",
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantActions
                    )
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                text = "取消",
                onClick = {
                    show.value = false
                    onCancel()
                },
                modifier = Modifier
                    .weight(1f)
                    .pressable(interactionSource = null, indication = SinkFeedback())
            )
            Spacer(modifier = Modifier.width(16.dp))
            TextButton(
                text = "确认覆盖",
                onClick = {
                    show.value = false
                    onOverwrite()
                },
                modifier = Modifier
                    .weight(1f)
                    .pressable(interactionSource = null, indication = SinkFeedback()),
                colors = ButtonDefaults.textButtonColorsPrimary()
            )
        }
    }
}
