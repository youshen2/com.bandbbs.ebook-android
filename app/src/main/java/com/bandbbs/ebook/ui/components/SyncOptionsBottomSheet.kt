package com.bandbbs.ebook.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bandbbs.ebook.ui.viewmodel.SyncOptionsState
import kotlin.math.min

@Composable
fun SyncOptionsBottomSheet(
    state: SyncOptionsState,
    onCancel: () -> Unit,
    onConfirm: (startChapter: Int, chapterCount: Int) -> Unit
) {
    var startChapter by remember { mutableStateOf("") }
    var chapterCount by remember { mutableStateOf("") }

    LaunchedEffect(state.syncedChapters, state.totalChapters) {
        if (state.totalChapters > 0 || state.syncedChapters > 0) {
            startChapter = (state.syncedChapters + 1).toString()
            chapterCount = min(400, state.totalChapters - state.syncedChapters)
                .coerceAtLeast(0).toString()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel) {
                Text("取消")
            }
            Text("同步选项", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = {
                onConfirm(
                    startChapter.toIntOrNull() ?: 1,
                    chapterCount.toIntOrNull() ?: 0
                )
            }) {
                Text("同步")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (state.totalChapters == 0 && state.book.chapterCount > 0) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text("正在获取手环状态...")
            }
        } else {
            Text(
                text = state.book.name,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "手环已有 ${state.syncedChapters} / ${state.totalChapters} 章",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = startChapter,
                onValueChange = { startChapter = it.filter { c -> c.isDigit() } },
                label = { Text("从第几章开始") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = chapterCount,
                onValueChange = { chapterCount = it.filter { c -> c.isDigit() } },
                label = { Text("同步章节数量 (最多400)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = { Text("一次性传输过多章节会导致手环爆内存，故设置上限。") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}
