package com.bandbbs.ebook.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private const val PREFS_NAME = "reader_settings_prefs"
private const val KEY_FONT_SIZE = "font_size"
private const val KEY_LINE_HEIGHT = "line_height"
private const val KEY_TEXT_COLOR = "text_color"
private const val KEY_BG_COLOR = "bg_color"
private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
private const val KEY_AUTO_SCROLL_SPEED = "auto_scroll_speed"

private const val DEFAULT_FONT_SIZE = 18f
private const val DEFAULT_LINE_HEIGHT = 1.6f

private const val DEFAULT_TEXT_COLOR = 0xFF1F1F1F.toInt()
private const val DEFAULT_BG_COLOR = 0xFFFFFFFF.toInt()

data class ReaderSettings(
    val fontSize: Float = DEFAULT_FONT_SIZE,
    val lineHeight: Float = DEFAULT_LINE_HEIGHT,
    val textColor: Int = DEFAULT_TEXT_COLOR,
    val backgroundColor: Int = DEFAULT_BG_COLOR,
    val keepScreenOn: Boolean = false,
    val autoScrollSpeed: Int = 0 
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
        .apply()
}

fun loadReaderSettings(context: Context): ReaderSettings {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return ReaderSettings(
        fontSize = prefs.getFloat(KEY_FONT_SIZE, DEFAULT_FONT_SIZE),
        lineHeight = prefs.getFloat(KEY_LINE_HEIGHT, DEFAULT_LINE_HEIGHT),
        textColor = prefs.getInt(KEY_TEXT_COLOR, DEFAULT_TEXT_COLOR),
        backgroundColor = prefs.getInt(KEY_BG_COLOR, DEFAULT_BG_COLOR),
        keepScreenOn = prefs.getBoolean(KEY_KEEP_SCREEN_ON, false),
        autoScrollSpeed = prefs.getInt(KEY_AUTO_SCROLL_SPEED, 0)
    )
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

    fun updateSettings(
        fs: Float = fontSize,
        lh: Float = lineHeight,
        tc: Int = textColor,
        bc: Int = backgroundColor,
        kso: Boolean = keepScreenOn,
        ass: Int = autoScrollSpeed
    ) {
        val newSettings = ReaderSettings(fs, lh, tc, bc, kso, ass)
        saveReaderSettings(context, newSettings)
        onSettingsChanged(newSettings)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = "阅读设置",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        
        Text(
            text = "背景主题",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ReaderThemes.forEach { theme ->
                val isSelected = theme.backgroundColor == backgroundColor
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(theme.backgroundColor))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                            shape = CircleShape
                        )
                        .clickable {
                            textColor = theme.textColor
                            backgroundColor = theme.backgroundColor
                            updateSettings(tc = theme.textColor, bc = theme.backgroundColor)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(theme.textColor),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        
        Text(
            text = "字体大小",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "12",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(end = 16.dp)
            )
            Slider(
                value = fontSize,
                onValueChange = { newValue ->
                    fontSize = newValue
                    updateSettings(fs = newValue)
                },
                valueRange = 12f..32f,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "32",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
        Text(
            text = "${fontSize.toInt()}sp",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        
        Text(
            text = "行间距",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "1.0",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(end = 16.dp)
            )
            Slider(
                value = lineHeight,
                onValueChange = { newValue ->
                    lineHeight = newValue
                    updateSettings(lh = newValue)
                },
                valueRange = 1.0f..2.5f,
                steps = 14,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "2.5",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
        Text(
            text = String.format("%.1f", lineHeight),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp)
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "屏幕常亮",
                style = MaterialTheme.typography.titleMedium
            )
            Switch(
                checked = keepScreenOn,
                onCheckedChange = {
                    keepScreenOn = it
                    updateSettings(kso = it)
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "自动滚动",
                    style = MaterialTheme.typography.titleMedium
                )
                if (autoScrollSpeed > 0) {
                    Text(
                        text = "速度: $autoScrollSpeed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Switch(
                checked = autoScrollSpeed > 0,
                onCheckedChange = {
                    autoScrollSpeed = if (it) 20 else 0
                    updateSettings(ass = autoScrollSpeed)
                }
            )
        }

        if (autoScrollSpeed > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "慢",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(end = 16.dp)
                )
                Slider(
                    value = autoScrollSpeed.toFloat(),
                    onValueChange = {
                        autoScrollSpeed = it.toInt()
                        updateSettings(ass = it.toInt())
                    },
                    valueRange = 5f..100f,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "快",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}
