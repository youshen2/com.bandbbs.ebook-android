package com.bandbbs.ebook.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.bandbbs.ebook.ui.viewmodel.MainViewModel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.extra.SuperDropdown
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

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

    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            TopAppBar(
                title = "手环端设置",
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.padding(start = 6.dp)) {
                        Icon(imageVector = MiuixIcons.Back, contentDescription = "返回")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        popupHost = {}
    ) { paddingValues ->
        if (bandSettingsState == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                if (!isLoading.isLoading) {
                    Text("无法加载设置", color = MiuixTheme.colorScheme.error)
                } else {
                    CircularProgressIndicator()
                }
            }
        } else {
            val settings = bandSettingsState!!

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    bottom = 32.dp
                )
            ) {
                item {
                    SettingsSection(title = "阅读设置") {
                        var fontSize by remember(settings.fontSize) { mutableFloatStateOf(settings.fontSize.toFloat()) }
                        SettingSlider(
                            title = "字号: ${fontSize.toInt()}",
                            value = fontSize,
                            onValueChange = { fontSize = it },
                            onValueChangeFinished = {
                                viewModel.updateBandSetting(
                                    "EBOOK_FONT",
                                    fontSize.toInt().toString()
                                )
                            },
                            valueRange = 20f..40f,
                            steps = 19
                        )

                        var opacity by remember(settings.opacity) { mutableFloatStateOf(settings.opacity.toFloat()) }
                        SettingSlider(
                            title = "字体亮度 (透明度): ${opacity.toInt()}%",
                            value = opacity,
                            onValueChange = { opacity = it },
                            onValueChangeFinished = {
                                viewModel.updateBandSetting(
                                    "EBOOK_OPACITY",
                                    opacity.toInt().toString()
                                )
                            },
                            valueRange = 10f..100f,
                            steps = 89
                        )

                        SettingSwitch(
                            title = "粗体显示",
                            checked = settings.boldEnabled,
                            onCheckedChange = {
                                viewModel.updateBandSetting(
                                    "EBOOK_BOLD_ENABLED",
                                    it.toString()
                                )
                            }
                        )

                        var margin by remember(settings.verticalMargin) {
                            mutableFloatStateOf(
                                settings.verticalMargin.toFloat()
                            )
                        }
                        SettingSlider(
                            title = "垂直边距: ${margin.toInt()}px",
                            value = margin,
                            onValueChange = { margin = it },
                            onValueChangeFinished = {
                                viewModel.updateBandSetting(
                                    "EBOOK_VERTICAL_MARGIN",
                                    margin.toInt().toString()
                                )
                            },
                            valueRange = 0f..50f,
                            steps = 9
                        )
                    }
                }

                item {
                    SettingsSection(title = "交互与显示") {
                        SettingDropdown(
                            title = "时间格式",
                            options = listOf("24h" to "24小时制", "12h" to "12小时制"),
                            selectedKey = settings.timeFormat,
                            onSelected = { viewModel.updateBandSetting("EBOOK_TIME_FORMAT", it) }
                        )

                        SettingDropdown(
                            title = "阅读模式",
                            options = listOf("scroll" to "滚动模式", "nostalgic" to "怀旧模式"),
                            selectedKey = settings.readMode,
                            onSelected = { viewModel.updateBandSetting("EBOOK_READ_MODE", it) }
                        )

                        if (settings.readMode == "nostalgic") {
                            SettingDropdown(
                                title = "怀旧模式翻页方式",
                                options = listOf(
                                    "swipe" to "左右滑动",
                                    "sideClick" to "两侧点击",
                                    "topBottomClick" to "上下点击"
                                ),
                                selectedKey = settings.nostalgicPageTurnMode,
                                onSelected = {
                                    viewModel.updateBandSetting(
                                        "EBOOK_NOSTALGIC_PAGE_TURN_MODE",
                                        it
                                    )
                                }
                            )
                        }

                        SettingDropdown(
                            title = "手势模式",
                            summary = "单击或双击屏幕切换UI",
                            options = listOf("single" to "单击切换", "double" to "双击切换"),
                            selectedKey = settings.gesture,
                            onSelected = { viewModel.updateBandSetting("EBOOK_GESTURE", it) }
                        )

                        SettingSwitch(
                            title = "显示进度条",
                            checked = settings.showProgressBar,
                            onCheckedChange = {
                                viewModel.updateBandSetting(
                                    "EBOOK_SHOW_PROGRESS_BAR",
                                    it.toString()
                                )
                            }
                        )

                        if (settings.showProgressBar) {
                            SettingSwitch(
                                title = "显示百分比",
                                checked = settings.showProgressBarPercent,
                                onCheckedChange = {
                                    viewModel.updateBandSetting(
                                        "EBOOK_SHOW_PROGRESS_BAR_PERCENT",
                                        it.toString()
                                    )
                                }
                            )

                            var progressBarOpacity by remember(settings.progressBarOpacity) {
                                mutableFloatStateOf(
                                    settings.progressBarOpacity.toFloat()
                                )
                            }
                            SettingSlider(
                                title = "进度条透明度: ${progressBarOpacity.toInt()}%",
                                value = progressBarOpacity,
                                onValueChange = { progressBarOpacity = it },
                                onValueChangeFinished = {
                                    viewModel.updateBandSetting(
                                        "EBOOK_PROGRESS_BAR_OPACITY",
                                        progressBarOpacity.toInt().toString()
                                    )
                                },
                                valueRange = 0f..100f,
                                steps = 99
                            )

                            var progressBarHeight by remember(settings.progressBarHeight) {
                                mutableFloatStateOf(
                                    settings.progressBarHeight.toFloat()
                                )
                            }
                            SettingSlider(
                                title = "进度条高度: ${progressBarHeight.toInt()}px",
                                value = progressBarHeight,
                                onValueChange = { progressBarHeight = it },
                                onValueChangeFinished = {
                                    viewModel.updateBandSetting(
                                        "EBOOK_PROGRESS_BAR_HEIGHT",
                                        progressBarHeight.toInt().toString()
                                    )
                                },
                                valueRange = 5f..50f,
                                steps = 44
                            )
                        }

                        SettingSwitch(
                            title = "屏幕亮度",
                            summary = if (settings.brightnessFollowSystem) "跟随系统" else "自定义: ${settings.brightness}",
                            checked = settings.brightnessFollowSystem,
                            onCheckedChange = {
                                viewModel.updateBandSetting(
                                    "EBOOK_BRIGHTNESS_FOLLOW_SYSTEM",
                                    it.toString()
                                )
                            }
                        )

                        if (!settings.brightnessFollowSystem) {
                            var brightness by remember(settings.brightness) {
                                mutableFloatStateOf(
                                    settings.brightness.toFloat()
                                )
                            }
                            SettingSlider(
                                title = "自定义亮度: ${brightness.toInt()}",
                                value = brightness,
                                onValueChange = { brightness = it },
                                onValueChangeFinished = {
                                    viewModel.updateBandSetting(
                                        "EBOOK_BRIGHTNESS",
                                        brightness.toInt().toString()
                                    )
                                },
                                valueRange = 10f..255f,
                                steps = 244
                            )
                        }

                        SettingSwitch(
                            title = "时间常驻",
                            summary = "在阅读页始终显示时间",
                            checked = settings.alwaysShowTime,
                            onCheckedChange = {
                                viewModel.updateBandSetting(
                                    "EBOOK_ALWAYS_SHOW_TIME",
                                    it.toString()
                                )
                            }
                        )

                        if (settings.alwaysShowTime && settings.readMode != "nostalgic") {
                            SettingSwitch(
                                title = "显示电量",
                                summary = "在时间常驻时显示电量",
                                checked = settings.alwaysShowBattery,
                                onCheckedChange = {
                                    viewModel.updateBandSetting(
                                        "EBOOK_ALWAYS_SHOW_BATTERY",
                                        it.toString()
                                    )
                                }
                            )

                            var timeSensitivity by remember(settings.alwaysShowTimeSensitivity) {
                                mutableFloatStateOf(
                                    settings.alwaysShowTimeSensitivity.toFloat()
                                )
                            }
                            SettingSlider(
                                title = "调整距离: ${timeSensitivity.toInt()}px",
                                value = timeSensitivity,
                                onValueChange = { timeSensitivity = it },
                                onValueChangeFinished = {
                                    viewModel.updateBandSetting(
                                        "EBOOK_ALWAYS_SHOW_TIME_SENSITIVITY",
                                        timeSensitivity.toInt().toString()
                                    )
                                },
                                valueRange = 0f..500f,
                                steps = 49
                            )
                        }
                    }
                }

                item {
                    SettingsSection(title = "额外内容") {
                        SettingSwitch(
                            title = "章节开头空行",
                            summary = "每章开头增加空行",
                            checked = settings.chapterStartEmptyLines,
                            onCheckedChange = {
                                viewModel.updateBandSetting(
                                    "EBOOK_CHAPTER_START_EMPTY_LINES",
                                    it.toString()
                                )
                            }
                        )
                        SettingSwitch(
                            title = "显示章节编号",
                            summary = "每章开头显示编号",
                            checked = settings.chapterStartNumber,
                            onCheckedChange = {
                                viewModel.updateBandSetting(
                                    "EBOOK_CHAPTER_START_NUMBER",
                                    it.toString()
                                )
                            }
                        )
                        SettingSwitch(
                            title = "显示章节名称",
                            summary = "每章开头显示名称",
                            checked = settings.chapterStartName,
                            onCheckedChange = {
                                viewModel.updateBandSetting(
                                    "EBOOK_CHAPTER_START_NAME",
                                    it.toString()
                                )
                            }
                        )
                        SettingSwitch(
                            title = "显示章节字数",
                            summary = "每章开头显示字数",
                            checked = settings.chapterStartWordCount,
                            onCheckedChange = {
                                viewModel.updateBandSetting(
                                    "EBOOK_CHAPTER_START_WORD_COUNT",
                                    it.toString()
                                )
                            }
                        )
                    }
                }

                item {
                    SettingsSection(title = "翻章设置") {
                        if (settings.readMode != "nostalgic") {
                            SettingDropdown(
                                title = "翻章方式",
                                options = listOf(
                                    "button" to "按钮",
                                    "boundary" to "越界",
                                    "swipe" to "滑动"
                                ),
                                selectedKey = settings.chapterSwitchStyle,
                                onSelected = {
                                    viewModel.updateBandSetting(
                                        "EBOOK_CHAPTER_SWITCH_STYLE",
                                        it
                                    )
                                }
                            )

                            if (settings.chapterSwitchStyle == "button") {
                                var switchHeight by remember(settings.chapterSwitchHeight) {
                                    mutableFloatStateOf(
                                        settings.chapterSwitchHeight.toFloat()
                                    )
                                }
                                SettingSlider(
                                    title = "按钮高度: ${switchHeight.toInt()}px",
                                    value = switchHeight,
                                    onValueChange = { switchHeight = it },
                                    onValueChangeFinished = {
                                        viewModel.updateBandSetting(
                                            "EBOOK_CHAPTER_SWITCH_HEIGHT",
                                            switchHeight.toInt().toString()
                                        )
                                    },
                                    valueRange = 40f..120f,
                                    steps = 7
                                )

                                SettingSwitch(
                                    title = "显示章节信息",
                                    summary = "按钮上显示章节信息",
                                    checked = settings.chapterSwitchShowInfo,
                                    onCheckedChange = {
                                        viewModel.updateBandSetting(
                                            "EBOOK_CHAPTER_SWITCH_SHOW_INFO",
                                            it.toString()
                                        )
                                    }
                                )
                            }

                            if (settings.chapterSwitchStyle == "boundary") {
                                var boundarySensitivity by remember(settings.chapterSwitchSensitivity) {
                                    mutableFloatStateOf(
                                        settings.chapterSwitchSensitivity.toFloat()
                                    )
                                }
                                SettingSlider(
                                    title = "越界灵敏度: ${boundarySensitivity.toInt()}",
                                    value = boundarySensitivity,
                                    onValueChange = { boundarySensitivity = it },
                                    onValueChangeFinished = {
                                        viewModel.updateBandSetting(
                                            "EBOOK_CHAPTER_SWITCH_SENSITIVITY",
                                            boundarySensitivity.toInt().toString()
                                        )
                                    },
                                    valueRange = 0f..100f,
                                    steps = 99
                                )
                            }

                            if (settings.chapterSwitchStyle == "swipe") {
                                var swipeSensitivity by remember(settings.swipeSensitivity) {
                                    mutableFloatStateOf(
                                        settings.swipeSensitivity.toFloat()
                                    )
                                }
                                SettingSlider(
                                    title = "滑动灵敏度: ${swipeSensitivity.toInt()}",
                                    value = swipeSensitivity,
                                    onValueChange = { swipeSensitivity = it },
                                    onValueChangeFinished = {
                                        viewModel.updateBandSetting(
                                            "EBOOK_SWIPE_SENSITIVITY",
                                            swipeSensitivity.toInt().toString()
                                        )
                                    },
                                    valueRange = 0f..100f,
                                    steps = 99
                                )
                            }
                        }
                    }
                }

                item {
                    SettingsSection(title = "滑动与翻页") {
                        if (settings.readMode != "nostalgic") {
                            SettingDropdown(
                                title = "滑动翻页",
                                options = listOf(
                                    "off" to "关闭",
                                    "column" to "上下滑动",
                                    "row" to "左右滑动"
                                ),
                                selectedKey = settings.swipe,
                                onSelected = { viewModel.updateBandSetting("EBOOK_SWIPE", it) }
                            )

                            SettingSwitch(
                                title = "自动翻页",
                                summary = if (settings.autoReadEnabled) "间隔: ${settings.autoReadSpeed}秒" else "长按文本以启动自动翻页",
                                checked = settings.autoReadEnabled,
                                onCheckedChange = {
                                    viewModel.updateAutoReadSetting(
                                        it,
                                        settings.autoReadSpeed
                                    )
                                }
                            )

                            if (settings.autoReadEnabled) {
                                var autoReadSpeed by remember(settings.autoReadSpeed) {
                                    mutableFloatStateOf(
                                        settings.autoReadSpeed.toFloat()
                                    )
                                }
                                SettingSlider(
                                    title = "间隔时间: ${autoReadSpeed.toInt()}秒",
                                    value = autoReadSpeed,
                                    onValueChange = { autoReadSpeed = it },
                                    onValueChangeFinished = {
                                        viewModel.updateAutoReadSetting(
                                            settings.autoReadEnabled,
                                            autoReadSpeed.toInt()
                                        )
                                    },
                                    valueRange = 1f..60f,
                                    steps = 58
                                )

                                var autoReadDistance by remember(settings.autoReadDistance) {
                                    mutableFloatStateOf(
                                        settings.autoReadDistance.toFloat()
                                    )
                                }
                                SettingSlider(
                                    title = "翻页距离: ${autoReadDistance.toInt()}px",
                                    value = autoReadDistance,
                                    onValueChange = { autoReadDistance = it },
                                    onValueChangeFinished = {
                                        viewModel.updateBandSetting(
                                            "EBOOK_AUTO_READ_DISTANCE",
                                            autoReadDistance.toInt().toString()
                                        )
                                    },
                                    valueRange = 0f..500f,
                                    steps = 49
                                )
                            }
                        }
                    }
                }

                item {
                    SettingsSection(title = "高级设置") {
                        var pageSize by remember(settings.txtSizePage) {
                            mutableFloatStateOf(
                                settings.txtSizePage.toFloat()
                            )
                        }
                        SettingSlider(
                            title = "分段大小: ${pageSize.toInt()} 字节",
                            value = pageSize,
                            onValueChange = { pageSize = it },
                            onValueChangeFinished = {
                                viewModel.updateBandSetting(
                                    "EBOOK_TXTSZPAGE",
                                    pageSize.toInt().toString()
                                )
                            },
                            valueRange = 400f..1200f,
                            steps = 7
                        )

                        SettingSwitch(
                            title = "段落防分割",
                            summary = "尽量保持段落完整",
                            checked = settings.preventParagraphSplitting,
                            onCheckedChange = {
                                viewModel.updateBandSetting(
                                    "EBOOK_PREVENT_PARAGRAPH_SPLITTING",
                                    it.toString()
                                )
                            }
                        )

                        SettingDropdown(
                            title = "进度保存模式",
                            options = listOf("exit" to "退出时保存", "periodic" to "定时保存"),
                            selectedKey = settings.progressSaveMode,
                            onSelected = {
                                viewModel.updateBandSetting(
                                    "EBOOK_PROGRESS_SAVE_MODE",
                                    it
                                )
                            }
                        )

                        if (settings.progressSaveMode == "periodic") {
                            var saveInterval by remember(settings.progressSaveInterval) {
                                mutableFloatStateOf(
                                    settings.progressSaveInterval.toFloat()
                                )
                            }
                            SettingSlider(
                                title = "保存间隔: ${saveInterval.toInt()}秒",
                                value = saveInterval,
                                onValueChange = { saveInterval = it },
                                onValueChangeFinished = {
                                    viewModel.updateBandSetting(
                                        "EBOOK_PROGRESS_SAVE_INTERVAL",
                                        saveInterval.toInt().toString()
                                    )
                                },
                                valueRange = 1f..60f,
                                steps = 58
                            )
                        }

                        SettingSwitch(
                            title = "书架跑马灯",
                            summary = "内容滚动显示",
                            checked = settings.shelfMarqueeEnabled,
                            onCheckedChange = {
                                viewModel.updateBandSetting(
                                    "EBOOK_SHELF_MARQUEE_ENABLED",
                                    it.toString()
                                )
                            }
                        )

                        SettingSwitch(
                            title = "书签列表跑马灯",
                            summary = "页面标题滚动",
                            checked = settings.bookmarkMarqueeEnabled,
                            onCheckedChange = {
                                viewModel.updateBandSetting(
                                    "EBOOK_BOOKMARK_MARQUEE_ENABLED",
                                    it.toString()
                                )
                            }
                        )

                        SettingSwitch(
                            title = "书籍详情跑马灯",
                            summary = "内容滚动显示",
                            checked = settings.bookinfoMarqueeEnabled,
                            onCheckedChange = {
                                viewModel.updateBandSetting(
                                    "EBOOK_BOOKINFO_MARQUEE_ENABLED",
                                    it.toString()
                                )
                            }
                        )

                        SettingSwitch(
                            title = "章节列表跑马灯",
                            summary = "页面标题滚动",
                            checked = settings.chapterListMarqueeEnabled,
                            onCheckedChange = {
                                viewModel.updateBandSetting(
                                    "EBOOK_CHAPTER_LIST_MARQUEE_ENABLED",
                                    it.toString()
                                )
                            }
                        )

                        SettingSwitch(
                            title = "书籍阅读器跑马灯",
                            summary = "页面标题滚动",
                            checked = settings.textReaderMarqueeEnabled,
                            onCheckedChange = {
                                viewModel.updateBandSetting(
                                    "EBOOK_TEXT_READER_MARQUEE_ENABLED",
                                    it.toString()
                                )
                            }
                        )

                        SettingSwitch(
                            title = "文本阅读页跑马灯",
                            summary = "页面标题滚动",
                            checked = settings.detailMarqueeEnabled,
                            onCheckedChange = {
                                viewModel.updateBandSetting(
                                    "EBOOK_DETAIL_MARQUEE_ENABLED",
                                    it.toString()
                                )
                            }
                        )

                        SettingSwitch(
                            title = "详情页进度跑马灯",
                            summary = "底部进度滚动",
                            checked = settings.detailProgressMarqueeEnabled,
                            onCheckedChange = {
                                viewModel.updateBandSetting(
                                    "EBOOK_DETAIL_PROGRESS_MARQUEE_ENABLED",
                                    it.toString()
                                )
                            }
                        )

                        SettingSwitch(
                            title = "快速退出应用",
                            summary = "连续点击三次屏幕退出",
                            checked = settings.teacherScreenEnabled,
                            onCheckedChange = {
                                viewModel.updateBandSetting(
                                    "EBOOK_TEACHER_SCREEN_ENABLED",
                                    it.toString()
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        SmallTitle(text = title)
        Card(modifier = Modifier.padding(horizontal = 12.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    summary: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    BasicComponent(
        title = title,
        summary = summary,
        endActions = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        },
        onClick = { onCheckedChange(!checked) }
    )
}

@Composable
private fun SettingSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = title,
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            steps = steps
        )
    }
}

@Composable
private fun SettingDropdown(
    title: String,
    summary: String? = null,
    options: List<Pair<String, String>>,
    selectedKey: String,
    onSelected: (String) -> Unit
) {
    val labels = options.map { it.second }
    val selectedIndex = options.indexOfFirst { it.first == selectedKey }.coerceAtLeast(0)

    SuperDropdown(
        title = title,
        summary = summary,
        items = labels,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { index ->
            onSelected(options[index].first)
        }
    )
}
