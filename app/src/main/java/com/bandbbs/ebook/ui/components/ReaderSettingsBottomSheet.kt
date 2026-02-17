package com.bandbbs.ebook.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.pow

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

enum class PageTurnMode {
    BUTTON,
    SWIPE,
    OVERSCROLL
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

private data class ReaderSheetColors(
    val sheetBg: Color,
    val groupBg: Color,
    val onBg: Color,
    val secondary: Color,
    val outline: Color,
    val accent: Color,
    val disabled: Color
)

private fun relativeLuminance(color: Color): Float {
    val argb = color.toArgb()
    val r = ((argb shr 16) and 0xFF) / 255f
    val g = ((argb shr 8) and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f

    fun lin(c: Float): Float = if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)

    val rl = lin(r)
    val gl = lin(g)
    val bl = lin(b)
    return 0.2126f * rl + 0.7152f * gl + 0.0722f * bl
}

@Composable
private fun rememberReaderSheetColors(settings: ReaderSettings): ReaderSheetColors {
    val bg = Color(settings.backgroundColor)
    val onBg = Color(settings.textColor)
    val isDark = relativeLuminance(bg) < 0.5f
    val groupBg = if (isDark) lerp(bg, Color.White, 0.08f) else lerp(bg, Color.Black, 0.05f)
    val outline = onBg.copy(alpha = if (isDark) 0.18f else 0.14f)
    val secondary = onBg.copy(alpha = if (isDark) 0.70f else 0.66f)
    val disabled = onBg.copy(alpha = 0.35f)
    val accent = onBg.copy(alpha = 0.92f)
    return remember(settings.textColor, settings.backgroundColor) {
        ReaderSheetColors(
            sheetBg = bg,
            groupBg = groupBg,
            onBg = onBg,
            secondary = secondary,
            outline = outline,
            accent = accent,
            disabled = disabled
        )
    }
}

@Composable
private fun ReaderSettingsGroup(
    title: String? = null,
    colors: ReaderSheetColors,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = colors.accent,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.groupBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun ReaderSwitchTile(
    title: String,
    description: String? = null,
    checked: Boolean,
    colors: ReaderSheetColors,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = colors.onBg
            )
        },
        supportingContent = description?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.secondary
                )
            }
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = colors.accent.copy(alpha = 0.35f),
                    checkedThumbColor = colors.accent,
                    checkedBorderColor = Color.Transparent,
                    uncheckedTrackColor = colors.outline.copy(alpha = 0.35f),
                    uncheckedThumbColor = colors.onBg.copy(alpha = 0.70f),
                    uncheckedBorderColor = Color.Transparent
                )
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    )
}

@Composable
private fun ReaderValueSlider(
    title: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    colors: ReaderSheetColors,
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
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = colors.onBg
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.secondary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                activeTrackColor = colors.accent,
                inactiveTrackColor = colors.outline.copy(alpha = 0.35f),
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
                thumbColor = colors.accent
            )
        )
    }
}

@Composable
private fun ReaderSelectionChip(
    selected: Boolean,
    label: String,
    colors: ReaderSheetColors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val container = if (selected) colors.accent.copy(alpha = 0.14f) else colors.sheetBg.copy(alpha = 0.10f)
    val border = if (selected) colors.accent.copy(alpha = 0.55f) else colors.outline
    val content = if (selected) colors.onBg else colors.secondary
    androidx.compose.material3.Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = container,
        border = androidx.compose.foundation.BorderStroke(1.dp, border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = content,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun ReaderThemeChip(
    theme: ReaderTheme,
    selected: Boolean,
    sheetColors: ReaderSheetColors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val border = if (selected) sheetColors.accent.copy(alpha = 0.60f) else sheetColors.outline
    val container = if (selected) sheetColors.accent.copy(alpha = 0.12f) else Color.Transparent
    androidx.compose.material3.Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = container,
        border = androidx.compose.foundation.BorderStroke(1.dp, border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(theme.backgroundColor))
                    .border(1.dp, sheetColors.outline, CircleShape)
            )
            Spacer(modifier = Modifier.size(10.dp))
            Text(
                text = theme.name,
                style = MaterialTheme.typography.bodyMedium,
                color = sheetColors.onBg,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = sheetColors.accent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ReaderSettingsBottomSheet(
    currentSettings: ReaderSettings,
    onSettingsChanged: (ReaderSettings) -> Unit
) {
    val context = LocalContext.current
    var fontSize by remember { mutableFloatStateOf(currentSettings.fontSize) }
    var lineHeight by remember { mutableFloatStateOf(currentSettings.lineHeight) }
    var textColor by remember { mutableIntStateOf(currentSettings.textColor) }
    var backgroundColor by remember { mutableIntStateOf(currentSettings.backgroundColor) }
    var keepScreenOn by remember { mutableStateOf(currentSettings.keepScreenOn) }
    var autoScrollSpeed by remember { mutableIntStateOf(currentSettings.autoScrollSpeed) }
    var pageTurnMode by remember { mutableStateOf(currentSettings.pageTurnMode) }
    var pageTurnSensitivity by remember { mutableIntStateOf(currentSettings.pageTurnSensitivity) }
    var customBrightnessEnabled by remember { mutableStateOf(currentSettings.customBrightnessEnabled) }
    var customBrightness by remember { mutableFloatStateOf(currentSettings.customBrightness) }
    val sheetColors = rememberReaderSheetColors(
        ReaderSettings(
            fontSize = fontSize,
            lineHeight = lineHeight,
            textColor = textColor,
            backgroundColor = backgroundColor,
            keepScreenOn = keepScreenOn,
            autoScrollSpeed = autoScrollSpeed,
            pageTurnMode = pageTurnMode,
            pageTurnSensitivity = pageTurnSensitivity,
            customBrightnessEnabled = customBrightnessEnabled,
            customBrightness = customBrightness
        )
    )

    fun updateSettings(
        fs: Float = fontSize,
        lh: Float = lineHeight,
        tc: Int = textColor,
        bc: Int = backgroundColor,
        kso: Boolean = keepScreenOn,
        ass: Int = autoScrollSpeed,
        ptm: PageTurnMode = pageTurnMode,
        pts: Int = pageTurnSensitivity,
        cbe: Boolean = customBrightnessEnabled,
        cb: Float = customBrightness
    ) {
        val newSettings = ReaderSettings(fs, lh, tc, bc, kso, ass, ptm, pts, cbe, cb)
        saveReaderSettings(context, newSettings)
        onSettingsChanged(newSettings)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(sheetColors.sheetBg),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                Text(
                    text = "阅读设置",
                    style = MaterialTheme.typography.titleLarge,
                    color = sheetColors.onBg
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "与当前阅读背景主题保持一致",
                    style = MaterialTheme.typography.bodySmall,
                    color = sheetColors.secondary
                )
            }
        }

        item {
            ReaderSettingsGroup(title = "外观", colors = sheetColors) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "背景主题",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = sheetColors.onBg
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ReaderThemes.chunked(2).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                row.forEach { theme ->
                                    val selected = theme.backgroundColor == backgroundColor
                                    ReaderThemeChip(
                                        theme = theme,
                                        selected = selected,
                                        sheetColors = sheetColors,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            textColor = theme.textColor
                                            backgroundColor = theme.backgroundColor
                                            updateSettings(tc = theme.textColor, bc = theme.backgroundColor)
                                        }
                                    )
                                }
                                if (row.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(sheetColors.sheetBg.copy(alpha = 0.10f))
                            .border(1.dp, sheetColors.outline, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "示例：这是一段预览文本，用于感受当前配色与字号行距。",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = fontSize.sp,
                                lineHeight = (fontSize * lineHeight).sp
                            ),
                            color = sheetColors.onBg
                        )
                    }
                }
            }
        }

        item {
            ReaderSettingsGroup(title = "字体与排版", colors = sheetColors) {
                ReaderValueSlider(
                    title = "字体大小",
                    valueText = "${fontSize.toInt()}sp",
                    value = fontSize,
                    valueRange = 12f..32f,
                    steps = 0,
                    colors = sheetColors,
                    onValueChange = { newValue ->
                        fontSize = newValue
                        updateSettings(fs = newValue)
                    }
                )
                ReaderValueSlider(
                    title = "行间距",
                    valueText = String.format("%.1f", lineHeight),
                    value = lineHeight,
                    valueRange = 1.0f..2.5f,
                    steps = 14,
                    colors = sheetColors,
                    onValueChange = { newValue ->
                        lineHeight = newValue
                        updateSettings(lh = newValue)
                    }
                )
            }
        }

        item {
            ReaderSettingsGroup(title = "翻章", colors = sheetColors) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "翻章方式",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = sheetColors.onBg
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReaderSelectionChip(
                            selected = pageTurnMode == PageTurnMode.BUTTON,
                            label = "按钮",
                            colors = sheetColors,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                pageTurnMode = PageTurnMode.BUTTON
                                updateSettings(ptm = PageTurnMode.BUTTON)
                            }
                        )
                        ReaderSelectionChip(
                            selected = pageTurnMode == PageTurnMode.SWIPE,
                            label = "滑动",
                            colors = sheetColors,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                pageTurnMode = PageTurnMode.SWIPE
                                updateSettings(ptm = PageTurnMode.SWIPE)
                            }
                        )
                        ReaderSelectionChip(
                            selected = pageTurnMode == PageTurnMode.OVERSCROLL,
                            label = "越界",
                            colors = sheetColors,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                pageTurnMode = PageTurnMode.OVERSCROLL
                                updateSettings(ptm = PageTurnMode.OVERSCROLL)
                            }
                        )
                    }
                }

                if (pageTurnMode != PageTurnMode.BUTTON) {
                    ReaderValueSlider(
                        title = "灵敏度",
                        valueText = pageTurnSensitivity.toString(),
                        value = pageTurnSensitivity.toFloat(),
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = sheetColors,
                        onValueChange = {
                            pageTurnSensitivity = it.toInt().coerceIn(1, 10)
                            updateSettings(pts = pageTurnSensitivity)
                        }
                    )
                }
            }
        }

        item {
            ReaderSettingsGroup(title = "屏幕", colors = sheetColors) {
                ReaderSwitchTile(
                    title = "自定义亮度",
                    description = if (customBrightnessEnabled) "${(customBrightness * 100).toInt()}%" else "跟随系统",
                    checked = customBrightnessEnabled,
                    colors = sheetColors,
                    onCheckedChange = {
                        customBrightnessEnabled = it
                        updateSettings(cbe = it)
                    }
                )
                if (customBrightnessEnabled) {
                    ReaderValueSlider(
                        title = "亮度",
                        valueText = "${(customBrightness * 100).toInt()}%",
                        value = customBrightness,
                        valueRange = 0.01f..1f,
                        steps = 0,
                        colors = sheetColors,
                        onValueChange = {
                            customBrightness = it.coerceIn(0.01f, 1f)
                            updateSettings(cb = customBrightness)
                        }
                    )
                }
                ReaderSwitchTile(
                    title = "屏幕常亮",
                    description = "阅读时保持屏幕不自动熄灭",
                    checked = keepScreenOn,
                    colors = sheetColors,
                    onCheckedChange = {
                        keepScreenOn = it
                        updateSettings(kso = it)
                    }
                )
            }
        }

        item {
            ReaderSettingsGroup(title = "自动滚动", colors = sheetColors) {
                ReaderSwitchTile(
                    title = "启用自动滚动",
                    description = if (autoScrollSpeed > 0) "速度：$autoScrollSpeed" else "隐藏控制栏后开始滚动",
                    checked = autoScrollSpeed > 0,
                    colors = sheetColors,
                    onCheckedChange = {
                        autoScrollSpeed = if (it) 20 else 0
                        updateSettings(ass = autoScrollSpeed)
                    }
                )
                if (autoScrollSpeed > 0) {
                    ReaderValueSlider(
                        title = "滚动速度",
                        valueText = autoScrollSpeed.toString(),
                        value = autoScrollSpeed.toFloat(),
                        valueRange = 5f..100f,
                        steps = 0,
                        colors = sheetColors,
                        onValueChange = {
                            autoScrollSpeed = it.toInt().coerceIn(5, 100)
                            updateSettings(ass = autoScrollSpeed)
                        }
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            val defaultTheme = ReaderThemes.first()
                            fontSize = DEFAULT_FONT_SIZE
                            lineHeight = DEFAULT_LINE_HEIGHT
                            textColor = defaultTheme.textColor
                            backgroundColor = defaultTheme.backgroundColor
                            keepScreenOn = false
                            autoScrollSpeed = 0
                            pageTurnMode = PageTurnMode.BUTTON
                            pageTurnSensitivity = 5
                            customBrightnessEnabled = false
                            customBrightness = 0.5f
                            updateSettings(
                                fs = fontSize,
                                lh = lineHeight,
                                tc = textColor,
                                bc = backgroundColor,
                                kso = keepScreenOn,
                                ass = autoScrollSpeed,
                                ptm = pageTurnMode,
                                pts = pageTurnSensitivity,
                                cbe = customBrightnessEnabled,
                                cb = customBrightness
                            )
                        }
                    ) {
                        Text("恢复默认", color = sheetColors.secondary)
                    }
                }
            }
        }
    }
}
