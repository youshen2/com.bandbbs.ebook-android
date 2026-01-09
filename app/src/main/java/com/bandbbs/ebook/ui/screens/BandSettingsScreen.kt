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

                        if (settings.readMode == "nostalgic") {
                            ListItem(
                                headlineContent = { Text("怀旧模式翻页方式") },
                                trailingContent = {
                                    DropdownMenuBox(
                                        options = listOf("swipe" to "左右滑动", "sideClick" to "两侧点击", "topBottomClick" to "上下点击"),
                                        selectedKey = settings.nostalgicPageTurnMode,
                                        onSelected = { viewModel.updateBandSetting("EBOOK_NOSTALGIC_PAGE_TURN_MODE", it) }
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }

                        ListItem(
                            headlineContent = { Text("手势模式") },
                            supportingContent = { Text("单击或双击屏幕切换UI") },
                            trailingContent = {
                                DropdownMenuBox(
                                    options = listOf("single" to "单击切换", "double" to "双击切换"),
                                    selectedKey = settings.gesture,
                                    onSelected = { viewModel.updateBandSetting("EBOOK_GESTURE", it) }
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

                            var progressBarOpacity by remember(settings.progressBarOpacity) { mutableFloatStateOf(settings.progressBarOpacity.toFloat()) }
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("进度条透明度: ${progressBarOpacity.toInt()}%", style = MaterialTheme.typography.bodyLarge)
                                Slider(
                                    value = progressBarOpacity,
                                    onValueChange = { progressBarOpacity = it },
                                    onValueChangeFinished = {
                                        viewModel.updateBandSetting("EBOOK_PROGRESS_BAR_OPACITY", progressBarOpacity.toInt().toString())
                                    },
                                    valueRange = 0f..100f,
                                    steps = 99
                                )
                            }

                            var progressBarHeight by remember(settings.progressBarHeight) { mutableFloatStateOf(settings.progressBarHeight.toFloat()) }
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("进度条高度: ${progressBarHeight.toInt()}px", style = MaterialTheme.typography.bodyLarge)
                                Slider(
                                    value = progressBarHeight,
                                    onValueChange = { progressBarHeight = it },
                                    onValueChangeFinished = {
                                        viewModel.updateBandSetting("EBOOK_PROGRESS_BAR_HEIGHT", progressBarHeight.toInt().toString())
                                    },
                                    valueRange = 5f..50f,
                                    steps = 44
                                )
                            }
                        }

                        ListItem(
                            headlineContent = { Text("屏幕亮度") },
                            supportingContent = { Text(if (settings.brightnessFollowSystem) "跟随系统" else "自定义: ${settings.brightness}") },
                            trailingContent = {
                                Switch(
                                    checked = settings.brightnessFollowSystem,
                                    onCheckedChange = { viewModel.updateBandSetting("EBOOK_BRIGHTNESS_FOLLOW_SYSTEM", it.toString()) }
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )

                        if (!settings.brightnessFollowSystem) {
                            var brightness by remember(settings.brightness) { mutableFloatStateOf(settings.brightness.toFloat()) }
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("自定义亮度: ${brightness.toInt()}", style = MaterialTheme.typography.bodyLarge)
                                Slider(
                                    value = brightness,
                                    onValueChange = { brightness = it },
                                    onValueChangeFinished = {
                                        viewModel.updateBandSetting("EBOOK_BRIGHTNESS", brightness.toInt().toString())
                                    },
                                    valueRange = 10f..255f,
                                    steps = 244
                                )
                            }
                        }

                        ListItem(
                            headlineContent = { Text("时间常驻") },
                            supportingContent = { Text("在阅读页始终显示时间") },
                            trailingContent = {
                                Switch(
                                    checked = settings.alwaysShowTime,
                                    onCheckedChange = { viewModel.updateBandSetting("EBOOK_ALWAYS_SHOW_TIME", it.toString()) }
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )

                        if (settings.alwaysShowTime && settings.readMode != "nostalgic") {
                            ListItem(
                                headlineContent = { Text("显示电量") },
                                supportingContent = { Text("在时间常驻时显示电量") },
                                trailingContent = {
                                    Switch(
                                        checked = settings.alwaysShowBattery,
                                        onCheckedChange = { viewModel.updateBandSetting("EBOOK_ALWAYS_SHOW_BATTERY", it.toString()) }
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )

                            var timeSensitivity by remember(settings.alwaysShowTimeSensitivity) { mutableFloatStateOf(settings.alwaysShowTimeSensitivity.toFloat()) }
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("调整距离: ${timeSensitivity.toInt()}px", style = MaterialTheme.typography.bodyLarge)
                                Slider(
                                    value = timeSensitivity,
                                    onValueChange = { timeSensitivity = it },
                                    onValueChangeFinished = {
                                        viewModel.updateBandSetting("EBOOK_ALWAYS_SHOW_TIME_SENSITIVITY", timeSensitivity.toInt().toString())
                                    },
                                    valueRange = 0f..500f,
                                    steps = 49
                                )
                            }
                        }
                    }
                }

                item {
                    SettingsSection(title = "额外内容") {
                        ListItem(
                            headlineContent = { Text("章节开头空行") },
                            supportingContent = { Text("每章开头增加空行") },
                            trailingContent = {
                                Switch(
                                    checked = settings.chapterStartEmptyLines,
                                    onCheckedChange = { viewModel.updateBandSetting("EBOOK_CHAPTER_START_EMPTY_LINES", it.toString()) }
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )

                        ListItem(
                            headlineContent = { Text("显示章节编号") },
                            supportingContent = { Text("每章开头显示编号") },
                            trailingContent = {
                                Switch(
                                    checked = settings.chapterStartNumber,
                                    onCheckedChange = { viewModel.updateBandSetting("EBOOK_CHAPTER_START_NUMBER", it.toString()) }
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )

                        ListItem(
                            headlineContent = { Text("显示章节名称") },
                            supportingContent = { Text("每章开头显示名称") },
                            trailingContent = {
                                Switch(
                                    checked = settings.chapterStartName,
                                    onCheckedChange = { viewModel.updateBandSetting("EBOOK_CHAPTER_START_NAME", it.toString()) }
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )

                        ListItem(
                            headlineContent = { Text("显示章节字数") },
                            supportingContent = { Text("每章开头显示字数") },
                            trailingContent = {
                                Switch(
                                    checked = settings.chapterStartWordCount,
                                    onCheckedChange = { viewModel.updateBandSetting("EBOOK_CHAPTER_START_WORD_COUNT", it.toString()) }
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }

                item {
                    SettingsSection(title = "翻章设置") {
                        if (settings.readMode != "nostalgic") {
                            ListItem(
                                headlineContent = { Text("翻章方式") },
                                trailingContent = {
                                    DropdownMenuBox(
                                        options = listOf("button" to "按钮", "boundary" to "越界", "swipe" to "滑动"),
                                        selectedKey = settings.chapterSwitchStyle,
                                        onSelected = { viewModel.updateBandSetting("EBOOK_CHAPTER_SWITCH_STYLE", it) }
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )

                            if (settings.chapterSwitchStyle == "button") {
                                var switchHeight by remember(settings.chapterSwitchHeight) { mutableFloatStateOf(settings.chapterSwitchHeight.toFloat()) }
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("按钮高度: ${switchHeight.toInt()}px", style = MaterialTheme.typography.bodyLarge)
                                    Slider(
                                        value = switchHeight,
                                        onValueChange = { switchHeight = it },
                                        onValueChangeFinished = {
                                            viewModel.updateBandSetting("EBOOK_CHAPTER_SWITCH_HEIGHT", switchHeight.toInt().toString())
                                        },
                                        valueRange = 40f..120f,
                                        steps = 7
                                    )
                                }

                                ListItem(
                                    headlineContent = { Text("显示章节信息") },
                                    supportingContent = { Text("按钮上显示章节信息") },
                                    trailingContent = {
                                        Switch(
                                            checked = settings.chapterSwitchShowInfo,
                                            onCheckedChange = { viewModel.updateBandSetting("EBOOK_CHAPTER_SWITCH_SHOW_INFO", it.toString()) }
                                        )
                                    },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                )
                            }

                            if (settings.chapterSwitchStyle == "boundary") {
                                var boundarySensitivity by remember(settings.chapterSwitchSensitivity) { mutableFloatStateOf(settings.chapterSwitchSensitivity.toFloat()) }
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("越界灵敏度: ${boundarySensitivity.toInt()}", style = MaterialTheme.typography.bodyLarge)
                                    Slider(
                                        value = boundarySensitivity,
                                        onValueChange = { boundarySensitivity = it },
                                        onValueChangeFinished = {
                                            viewModel.updateBandSetting("EBOOK_CHAPTER_SWITCH_SENSITIVITY", boundarySensitivity.toInt().toString())
                                        },
                                        valueRange = 0f..100f,
                                        steps = 99
                                    )
                                }
                            }

                            if (settings.chapterSwitchStyle == "swipe") {
                                var swipeSensitivity by remember(settings.swipeSensitivity) { mutableFloatStateOf(settings.swipeSensitivity.toFloat()) }
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("滑动灵敏度: ${swipeSensitivity.toInt()}", style = MaterialTheme.typography.bodyLarge)
                                    Slider(
                                        value = swipeSensitivity,
                                        onValueChange = { swipeSensitivity = it },
                                        onValueChangeFinished = {
                                            viewModel.updateBandSetting("EBOOK_SWIPE_SENSITIVITY", swipeSensitivity.toInt().toString())
                                        },
                                        valueRange = 0f..100f,
                                        steps = 99
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    SettingsSection(title = "滑动与翻页") {
                        if (settings.readMode != "nostalgic") {
                            ListItem(
                                headlineContent = { Text("滑动翻页") },
                                trailingContent = {
                                    DropdownMenuBox(
                                        options = listOf("off" to "关闭", "column" to "上下滑动", "row" to "左右滑动"),
                                        selectedKey = settings.swipe,
                                        onSelected = { viewModel.updateBandSetting("EBOOK_SWIPE", it) }
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )

                            ListItem(
                                headlineContent = { Text("自动翻页") },
                                supportingContent = { Text(if (settings.autoReadEnabled) "间隔: ${settings.autoReadSpeed}秒" else "长按文本以启动自动翻页") },
                                trailingContent = {
                                    Switch(
                                        checked = settings.autoReadEnabled,
                                        onCheckedChange = { viewModel.updateAutoReadSetting(it, settings.autoReadSpeed) }
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )

                            if (settings.autoReadEnabled) {
                                var autoReadSpeed by remember(settings.autoReadSpeed) { mutableFloatStateOf(settings.autoReadSpeed.toFloat()) }
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("间隔时间: ${autoReadSpeed.toInt()}秒", style = MaterialTheme.typography.bodyLarge)
                                    Slider(
                                        value = autoReadSpeed,
                                        onValueChange = { autoReadSpeed = it },
                                        onValueChangeFinished = {
                                            viewModel.updateAutoReadSetting(settings.autoReadEnabled, autoReadSpeed.toInt())
                                        },
                                        valueRange = 1f..60f,
                                        steps = 58
                                    )
                                }

                                var autoReadDistance by remember(settings.autoReadDistance) { mutableFloatStateOf(settings.autoReadDistance.toFloat()) }
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("翻页距离: ${autoReadDistance.toInt()}px", style = MaterialTheme.typography.bodyLarge)
                                    Slider(
                                        value = autoReadDistance,
                                        onValueChange = { autoReadDistance = it },
                                        onValueChangeFinished = {
                                            viewModel.updateBandSetting("EBOOK_AUTO_READ_DISTANCE", autoReadDistance.toInt().toString())
                                        },
                                        valueRange = 0f..500f,
                                        steps = 49
                                    )
                                }
                            }
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

                        ListItem(
                            headlineContent = { Text("进度保存模式") },
                            trailingContent = {
                                DropdownMenuBox(
                                    options = listOf("exit" to "退出时保存", "periodic" to "定时保存"),
                                    selectedKey = settings.progressSaveMode,
                                    onSelected = { viewModel.updateBandSetting("EBOOK_PROGRESS_SAVE_MODE", it) }
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )

                        if (settings.progressSaveMode == "periodic") {
                            var saveInterval by remember(settings.progressSaveInterval) { mutableFloatStateOf(settings.progressSaveInterval.toFloat()) }
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("保存间隔: ${saveInterval.toInt()}秒", style = MaterialTheme.typography.bodyLarge)
                                Slider(
                                    value = saveInterval,
                                    onValueChange = { saveInterval = it },
                                    onValueChangeFinished = {
                                        viewModel.updateBandSetting("EBOOK_PROGRESS_SAVE_INTERVAL", saveInterval.toInt().toString())
                                    },
                                    valueRange = 1f..60f,
                                    steps = 58
                                )
                            }
                        }

                        ListItem(
                            headlineContent = { Text("书架跑马灯") },
                            supportingContent = { Text("内容滚动显示") },
                            trailingContent = {
                                Switch(
                                    checked = settings.shelfMarqueeEnabled,
                                    onCheckedChange = { viewModel.updateBandSetting("EBOOK_SHELF_MARQUEE_ENABLED", it.toString()) }
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )

                        ListItem(
                            headlineContent = { Text("书签列表跑马灯") },
                            supportingContent = { Text("页面标题滚动") },
                            trailingContent = {
                                Switch(
                                    checked = settings.bookmarkMarqueeEnabled,
                                    onCheckedChange = { viewModel.updateBandSetting("EBOOK_BOOKMARK_MARQUEE_ENABLED", it.toString()) }
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )

                        ListItem(
                            headlineContent = { Text("书籍详情跑马灯") },
                            supportingContent = { Text("内容滚动显示") },
                            trailingContent = {
                                Switch(
                                    checked = settings.bookinfoMarqueeEnabled,
                                    onCheckedChange = { viewModel.updateBandSetting("EBOOK_BOOKINFO_MARQUEE_ENABLED", it.toString()) }
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )

                        ListItem(
                            headlineContent = { Text("章节列表跑马灯") },
                            supportingContent = { Text("页面标题滚动") },
                            trailingContent = {
                                Switch(
                                    checked = settings.chapterListMarqueeEnabled,
                                    onCheckedChange = { viewModel.updateBandSetting("EBOOK_CHAPTER_LIST_MARQUEE_ENABLED", it.toString()) }
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )

                        ListItem(
                            headlineContent = { Text("书籍阅读器跑马灯") },
                            supportingContent = { Text("页面标题滚动") },
                            trailingContent = {
                                Switch(
                                    checked = settings.textReaderMarqueeEnabled,
                                    onCheckedChange = { viewModel.updateBandSetting("EBOOK_TEXT_READER_MARQUEE_ENABLED", it.toString()) }
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )

                        ListItem(
                            headlineContent = { Text("文本阅读页跑马灯") },
                            supportingContent = { Text("页面标题滚动") },
                            trailingContent = {
                                Switch(
                                    checked = settings.detailMarqueeEnabled,
                                    onCheckedChange = { viewModel.updateBandSetting("EBOOK_DETAIL_MARQUEE_ENABLED", it.toString()) }
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )

                        ListItem(
                            headlineContent = { Text("详情页进度跑马灯") },
                            supportingContent = { Text("底部进度滚动") },
                            trailingContent = {
                                Switch(
                                    checked = settings.detailProgressMarqueeEnabled,
                                    onCheckedChange = { viewModel.updateBandSetting("EBOOK_DETAIL_PROGRESS_MARQUEE_ENABLED", it.toString()) }
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )

                        ListItem(
                            headlineContent = { Text("快速退出应用") },
                            supportingContent = { Text("连续点击三次屏幕退出") },
                            trailingContent = {
                                Switch(
                                    checked = settings.teacherScreenEnabled,
                                    onCheckedChange = { viewModel.updateBandSetting("EBOOK_TEACHER_SCREEN_ENABLED", it.toString()) }
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