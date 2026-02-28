package com.bandbbs.ebook.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.extra.SuperSwitch
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val PREFS_NAME = "reader_settings_prefs"
private const val KEY_FONT_SIZE = "font_size"
private const val KEY_LINE_HEIGHT = "line_height"
private const val KEY_TEXT_COLOR = "text_color"
private const val KEY_BG_COLOR = "bg_color"
private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
private const val KEY_AUTO_SCROLL_SPEED = "auto_scroll_speed"
private const val KEY_PAGE_TURN_MODE = "page_turn_mode"
private const val KEY_PAGE_TURN_SENSITIVITY = "page_turn_sensitivity"
private const val KEY_CUSTOM_BRIGHTNESS_ENABLED = "custom_brightness_enabled"
private const val KEY_CUSTOM_BRIGHTNESS = "custom_brightness"

private const val DEFAULT_FONT_SIZE = 18f
private const val DEFAULT_LINE_HEIGHT = 1.6f
private const val DEFAULT_TEXT_COLOR = 0xFF1F1F1F.toInt()
private const val DEFAULT_BG_COLOR = 0xFFFFFFFF.toInt()

enum class PageTurnMode(val label: String) {
    BUTTON("按钮"),
    SWIPE("滑动"),
    OVERSCROLL("越界")
}

data class ReaderSettings(
    val fontSize: Float = DEFAULT_FONT_SIZE,
    val lineHeight: Float = DEFAULT_LINE_HEIGHT,
    val textColor: Int = DEFAULT_TEXT_COLOR,
    val backgroundColor: Int = DEFAULT_BG_COLOR,
    val keepScreenOn: Boolean = false,
    val autoScrollSpeed: Int = 0,
    val pageTurnMode: PageTurnMode = PageTurnMode.BUTTON,
    val pageTurnSensitivity: Int = 5,
    val customBrightnessEnabled: Boolean = false,
    val customBrightness: Float = 0.5f
)

data class ReaderTheme(
    val name: String,
    val textColor: Int,
    val backgroundColor: Int
)

val ReaderThemes = listOf(
    ReaderTheme("白", 0xFF1F1F1F.toInt(), 0xFFFFFFFF.toInt()),
    ReaderTheme("黄", 0xFF3E3128.toInt(), 0xFFF4ECD8.toInt()),
    ReaderTheme("绿", 0xFF233325.toInt(), 0xFFC8E6C9.toInt()),
    ReaderTheme("黑", 0xFFB0B0B0.toInt(), 0xFF121212.toInt())
)

fun saveReaderSettings(context: Context, settings: ReaderSettings) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit()
        .putFloat(KEY_FONT_SIZE, settings.fontSize)
        .putFloat(KEY_LINE_HEIGHT, settings.lineHeight)
        .putInt(KEY_TEXT_COLOR, settings.textColor)
        .putInt(KEY_BG_COLOR, settings.backgroundColor)
        .putBoolean(KEY_KEEP_SCREEN_ON, settings.keepScreenOn)
        .putInt(KEY_AUTO_SCROLL_SPEED, settings.autoScrollSpeed)
        .putString(KEY_PAGE_TURN_MODE, settings.pageTurnMode.name)
        .putInt(KEY_PAGE_TURN_SENSITIVITY, settings.pageTurnSensitivity)
        .putBoolean(KEY_CUSTOM_BRIGHTNESS_ENABLED, settings.customBrightnessEnabled)
        .putFloat(KEY_CUSTOM_BRIGHTNESS, settings.customBrightness)
        .apply()
}

fun loadReaderSettings(context: Context): ReaderSettings {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val mode = runCatching {
        val modeName = prefs.getString(KEY_PAGE_TURN_MODE, PageTurnMode.BUTTON.name)
        PageTurnMode.valueOf(modeName ?: PageTurnMode.BUTTON.name)
    }.getOrDefault(PageTurnMode.BUTTON)
    return ReaderSettings(
        fontSize = prefs.getFloat(KEY_FONT_SIZE, DEFAULT_FONT_SIZE),
        lineHeight = prefs.getFloat(KEY_LINE_HEIGHT, DEFAULT_LINE_HEIGHT),
        textColor = prefs.getInt(KEY_TEXT_COLOR, DEFAULT_TEXT_COLOR),
        backgroundColor = prefs.getInt(KEY_BG_COLOR, DEFAULT_BG_COLOR),
        keepScreenOn = prefs.getBoolean(KEY_KEEP_SCREEN_ON, false),
        autoScrollSpeed = prefs.getInt(KEY_AUTO_SCROLL_SPEED, 0),
        pageTurnMode = mode,
        pageTurnSensitivity = prefs.getInt(KEY_PAGE_TURN_SENSITIVITY, 5).coerceIn(1, 10),
        customBrightnessEnabled = prefs.getBoolean(KEY_CUSTOM_BRIGHTNESS_ENABLED, false),
        customBrightness = prefs.getFloat(KEY_CUSTOM_BRIGHTNESS, 0.5f).coerceIn(0.01f, 1f)
    )
}

@Composable
private fun ReaderValueSlider(
    title: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurface
            )
            Text(
                text = valueText,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantActions
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
    }
}

@Composable
fun ReaderSettingsBottomSheet(
    currentSettings: ReaderSettings,
    onSettingsChanged: (ReaderSettings) -> Unit
) {
    val context = LocalContext.current

    var current by remember { mutableStateOf(currentSettings) }

    fun update(updateFn: ReaderSettings.() -> ReaderSettings) {
        current = current.updateFn()
        saveReaderSettings(context, current)
        onSettingsChanged(current)
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            SmallTitle(text = "背景主题")
            Card {
                Column {
                    val themeNames = ReaderThemes.map { it.name }
                    val selectedThemeIndex =
                        ReaderThemes.indexOfFirst { it.backgroundColor == current.backgroundColor }
                            .coerceAtLeast(0)

                    TabRowWithContour(
                        tabs = themeNames,
                        selectedTabIndex = selectedThemeIndex,
                        onTabSelected = { index ->
                            val theme = ReaderThemes[index]
                            update {
                                copy(
                                    textColor = theme.textColor,
                                    backgroundColor = theme.backgroundColor
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(current.backgroundColor))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "示例：这是一段预览文本，用于感受当前配色与字号行距。",
                            style = MiuixTheme.textStyles.body1.copy(
                                fontSize = current.fontSize.sp,
                                lineHeight = (current.fontSize * current.lineHeight).sp,
                                color = Color(current.textColor)
                            )
                        )
                    }
                }
            }
        }

        item {
            SmallTitle(text = "字体与排版")
            Card(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column {
                    ReaderValueSlider(
                        title = "字体大小",
                        valueText = "${current.fontSize.toInt()}sp",
                        value = current.fontSize,
                        valueRange = 12f..32f,
                        onValueChange = { update { copy(fontSize = it) } }
                    )
                    ReaderValueSlider(
                        title = "行间距",
                        valueText = String.format("%.1f", current.lineHeight),
                        value = current.lineHeight,
                        valueRange = 1.0f..2.5f,
                        steps = 14,
                        onValueChange = { update { copy(lineHeight = it) } }
                    )
                }
            }
        }

        item {
            SmallTitle(text = "翻章")
            Card(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "翻章方式",
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val modes = PageTurnMode.values()
                    TabRowWithContour(
                        tabs = modes.map { it.label },
                        selectedTabIndex = modes.indexOf(current.pageTurnMode),
                        onTabSelected = { update { copy(pageTurnMode = modes[it]) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (current.pageTurnMode != PageTurnMode.BUTTON) {
                    ReaderValueSlider(
                        title = "灵敏度",
                        valueText = current.pageTurnSensitivity.toString(),
                        value = current.pageTurnSensitivity.toFloat(),
                        valueRange = 1f..10f,
                        steps = 8,
                        onValueChange = { update { copy(pageTurnSensitivity = it.toInt()) } }
                    )
                }
            }
        }

        item {
            SmallTitle(text = "屏幕")
            Card(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column {
                    SuperSwitch(
                        title = "自定义亮度",
                        summary = if (current.customBrightnessEnabled) "${(current.customBrightness * 100).toInt()}%" else "跟随系统",
                        checked = current.customBrightnessEnabled,
                        onCheckedChange = { update { copy(customBrightnessEnabled = it) } }
                    )
                    if (current.customBrightnessEnabled) {
                        ReaderValueSlider(
                            title = "亮度",
                            valueText = "${(current.customBrightness * 100).toInt()}%",
                            value = current.customBrightness,
                            valueRange = 0.01f..1f,
                            onValueChange = { update { copy(customBrightness = it) } }
                        )
                    }
                    SuperSwitch(
                        title = "屏幕常亮",
                        summary = "阅读时保持屏幕不自动熄灭",
                        checked = current.keepScreenOn,
                        onCheckedChange = { update { copy(keepScreenOn = it) } }
                    )
                }
            }
        }

        item {
            SmallTitle(text = "自动滚动")
            Card(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column {
                    val isAutoScrollEnabled = current.autoScrollSpeed > 0
                    SuperSwitch(
                        title = "启用自动滚动",
                        summary = if (isAutoScrollEnabled) "速度：${current.autoScrollSpeed}" else "隐藏控制栏后开始滚动",
                        checked = isAutoScrollEnabled,
                        onCheckedChange = { enabled ->
                            update { copy(autoScrollSpeed = if (enabled) 20 else 0) }
                        }
                    )
                    if (isAutoScrollEnabled) {
                        ReaderValueSlider(
                            title = "滚动速度",
                            valueText = current.autoScrollSpeed.toString(),
                            value = current.autoScrollSpeed.toFloat(),
                            valueRange = 5f..100f,
                            onValueChange = { update { copy(autoScrollSpeed = it.toInt()) } }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(
                    text = "恢复默认",
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    onClick = {
                        update { ReaderSettings() }
                    }
                )
            }
        }
    }
}
