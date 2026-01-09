package com.bandbbs.ebook.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bandbbs.ebook.ui.components.LoadingDialog
import com.bandbbs.ebook.ui.viewmodel.BandSettingsState
import com.bandbbs.ebook.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BandSettingsScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val bandSettingsState by viewModel.bandSettingsState.collectAsState()
    val isLoading by viewModel.globalLoadingState.collectAsState()

    BackHandler {
        onBackClick()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearBandSettings()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("手环端设置") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        if (bandSettingsState == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isLoading.isLoading) {
                    Text("无法加载设置", color = MaterialTheme.colorScheme.error)
                }
            }
        } else {
            val settings = bandSettingsState!!

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SettingsSection(title = "阅读设置") {
                        var fontSize by remember(settings.fontSize) { mutableFloatStateOf(settings.fontSize.toFloat()) }
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("字号: ${fontSize.toInt()}", style = MaterialTheme.typography.bodyLarge)
                            Slider(
                                value = fontSize,
                                onValueChange = { fontSize = it },
                                onValueChangeFinished = {
                                    viewModel.updateBandSetting("EBOOK_FONT", fontSize.toInt().toString())
                                },
                                valueRange = 20f..40f,
                                steps = 19
                            )
                        }

                        var opacity by remember(settings.opacity) { mutableFloatStateOf(settings.opacity.toFloat()) }
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("字体亮度 (透明度): ${opacity.toInt()}%", style = MaterialTheme.typography.bodyLarge)
                            Slider(
                                value = opacity,
                                onValueChange = { opacity = it },
                                onValueChangeFinished = {
                                    viewModel.updateBandSetting("EBOOK_OPACITY", opacity.toInt().toString())
                                },
                                valueRange = 10f..100f,
                                steps = 89
                            )
                        }

                        ListItem(
                            headlineContent = { Text("粗体显示") },
                            trailingContent = {
                                Switch(
                                    checked = settings.boldEnabled,
                                    onCheckedChange = { viewModel.updateBandSetting("EBOOK_BOLD_ENABLED", it.toString()) }
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )

                        var margin by remember(settings.verticalMargin) { mutableFloatStateOf(settings.verticalMargin.toFloat()) }
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("垂直边距: ${margin.toInt()}px", style = MaterialTheme.typography.bodyLarge)
                            Slider(
                                value = margin,
                                onValueChange = { margin = it },
                                onValueChangeFinished = {
                                    viewModel.updateBandSetting("EBOOK_VERTICAL_MARGIN", margin.toInt().toString())
                                },
                                valueRange = 0f..50f,
                                steps = 9
                            )
                        }
                    }
                }

                item {
                    SettingsSection(title = "交互与显示") {
                        ListItem(
                            headlineContent = { Text("时间格式") },
                            trailingContent = {
                                DropdownMenuBox(
                                    options = listOf("24h" to "24小时制", "12h" to "12小时制"),
                                    selectedKey = settings.timeFormat,
                                    onSelected = { viewModel.updateBandSetting("EBOOK_TIME_FORMAT", it) }
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )

                        ListItem(
                            headlineContent = { Text("阅读模式") },
                            trailingContent = {
                                DropdownMenuBox(
                                    options = listOf("scroll" to "滚动模式", "nostalgic" to "怀旧模式"),
                                    selectedKey = settings.readMode,
                                    onSelected = { viewModel.updateBandSetting("EBOOK_READ_MODE", it) }
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )

                        ListItem(
                            headlineContent = { Text("显示进度条") },
                            trailingContent = {
                                Switch(
                                    checked = settings.showProgressBar,
                                    onCheckedChange = { viewModel.updateBandSetting("EBOOK_SHOW_PROGRESS_BAR", it.toString()) }
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )

                        if(settings.showProgressBar) {
                            ListItem(
                                headlineContent = { Text("显示百分比") },
                                trailingContent = {
                                    Switch(
                                        checked = settings.showProgressBarPercent,
                                        onCheckedChange = { viewModel.updateBandSetting("EBOOK_SHOW_PROGRESS_BAR_PERCENT", it.toString()) }
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }
                    }
                }

                item {
                    SettingsSection(title = "高级设置") {
                        var pageSize by remember(settings.txtSizePage) { mutableFloatStateOf(settings.txtSizePage.toFloat()) }
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("分段大小: ${pageSize.toInt()} 字节", style = MaterialTheme.typography.bodyLarge)
                            Slider(
                                value = pageSize,
                                onValueChange = { pageSize = it },
                                onValueChangeFinished = {
                                    viewModel.updateBandSetting("EBOOK_TXTSZPAGE", pageSize.toInt().toString())
                                },
                                valueRange = 400f..1200f,
                                steps = 7
                            )
                        }

                        ListItem(
                            headlineContent = { Text("预载章节") },
                            supportingContent = { Text("预先加载完整章节，减少翻页卡顿") },
                            trailingContent = {
                                Switch(
                                    checked = settings.preloadChapter,
                                    onCheckedChange = { viewModel.updateBandSetting("EBOOK_PRELOAD_CHAPTER", it.toString()) }
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )

                        ListItem(
                            headlineContent = { Text("段落防分割") },
                            supportingContent = { Text("尽量保持段落完整") },
                            trailingContent = {
                                Switch(
                                    checked = settings.preventParagraphSplitting,
                                    onCheckedChange = { viewModel.updateBandSetting("EBOOK_PREVENT_PARAGRAPH_SPLITTING", it.toString()) }
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun DropdownMenuBox(
    options: List<Pair<String, String>>,
    selectedKey: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.find { it.first == selectedKey }?.second ?: selectedKey

    androidx.compose.foundation.layout.Box {
        Row(
            modifier = Modifier
                .clickable { expanded = true }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(selectedLabel, style = MaterialTheme.typography.bodyMedium)
            Icon(Icons.Outlined.ExpandMore, contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (key, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelected(key)
                        expanded = false
                    },
                    trailingIcon = if (key == selectedKey) {
                        { Icon(Icons.Outlined.Check, contentDescription = null) }
                    } else null
                )
            }
        }
    }
}