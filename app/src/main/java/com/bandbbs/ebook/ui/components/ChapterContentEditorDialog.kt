package com.bandbbs.ebook.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bandbbs.ebook.ui.model.ChapterEditContent
import com.bandbbs.ebook.ui.model.ChapterSegment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterContentEditorPanel(
    state: ChapterEditContent,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onSplit: (List<ChapterSegment>) -> Unit
) {
    var title by remember(state.id) { mutableStateOf(state.title) }
    var content by remember(state.id) { mutableStateOf(state.content) }
    var showSplit by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                CenterAlignedTopAppBar(
                    title = {
                        Column {
                            Text("编辑章节", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "第 ${state.index + 1} 章",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Outlined.Close, contentDescription = "关闭")
                        }
                    },
                    actions = {
                        TextButton(onClick = { showSplit = true }) { Text("拆分") }
                        TextButton(
                            onClick = { onSave(title, content) },
                            enabled = content.isNotBlank()
                        ) { Text("保存") }
                    }
                )
                Divider()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("章节标题") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("章节内容") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 320.dp)
                    )
                }
            }
        }

        AnimatedVisibility(visible = showSplit) {
            ChapterSplitPanel(
                initialTitle = title.ifBlank { state.title },
                initialContent = content,
                onDismiss = { showSplit = false },
                onConfirm = {
                    onSplit(it)
                    showSplit = false
                }
            )
        }
    }
}

@Composable
private fun ChapterSplitPanel(
    initialTitle: String,
    initialContent: String,
    onDismiss: () -> Unit,
    onConfirm: (List<ChapterSegment>) -> Unit
) {
    val segments = remember { mutableStateListOf<SegmentForm>() }

    LaunchedEffect(initialContent) {
        segments.clear()
        val autoSegments = autoSplit(initialTitle, initialContent)
        if (autoSegments.size > 1) {
            segments.addAll(autoSegments.map { SegmentForm(it.title, it.content) })
        } else {
            segments.add(SegmentForm(initialTitle, initialContent))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(16.dp),
            tonalElevation = 8.dp,
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "返回")
                    }
                    Text("拆分章节", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(onClick = { segments.add(SegmentForm("", "")) }) {
                        Text("追加片段")
                    }
                    TextButton(onClick = {
                        segments.clear()
                        segments.addAll(
                            autoSplit(initialTitle, initialContent).map {
                                SegmentForm(it.title, it.content)
                            }
                        )
                    }) {
                        Text("按空行自动拆分")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(segments, key = { index, _ -> index }) { index, segment ->
                        SegmentEditorCard(
                            index = index,
                            form = segment,
                            canDelete = segments.size > 1,
                            onDelete = { segments.removeAt(index) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) { Text("返回编辑") }
                    TextButton(
                        enabled = segments.size >= 2 && segments.all { it.content.isNotBlank() },
                        onClick = {
                            val result = segments.map {
                                ChapterSegment(
                                    title = it.title.ifBlank { initialTitle },
                                    content = it.content
                                )
                            }
                            onConfirm(result)
                        }
                    ) { Text("确认拆分") }
                }
            }
        }
    }
}

@Composable
private fun SegmentEditorCard(
    index: Int,
    form: SegmentForm,
    canDelete: Boolean,
    onDelete: () -> Unit
) {
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("片段 ${index + 1}", style = MaterialTheme.typography.titleMedium)
                if (canDelete) {
                    TextButton(onClick = onDelete) { Text("删除") }
                }
            }
            OutlinedTextField(
                value = form.title,
                onValueChange = { form.title = it },
                label = { Text("标题") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = form.content,
                onValueChange = { form.content = it },
                label = { Text("内容") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )
        }
    }
}

private class SegmentForm(
    title: String,
    content: String
) {
    var title by mutableStateOf(title)
    var content by mutableStateOf(content)
}

private fun autoSplit(baseTitle: String, content: String): List<ChapterSegment> {
    val parts = content.split(Regex("(\\r?\\n){2,}"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    if (parts.size <= 1) {
        return listOf(ChapterSegment(baseTitle, content))
    }
    return parts.mapIndexed { index, part ->
        ChapterSegment(
            title = "$baseTitle-${index + 1}",
            content = part
        )
    }
}

