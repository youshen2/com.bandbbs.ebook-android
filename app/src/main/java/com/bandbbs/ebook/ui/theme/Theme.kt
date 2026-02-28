package com.bandbbs.ebook.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@Composable
fun EbookTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val controller = remember { ThemeController( if (darkTheme) ColorSchemeMode.Dark else ColorSchemeMode.Light) }
    MiuixTheme(
        controller = controller,
        content = content
    )
}
