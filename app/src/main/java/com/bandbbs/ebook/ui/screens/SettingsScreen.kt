package com.bandbbs.ebook.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bandbbs.ebook.ui.components.AboutDialog
import com.bandbbs.ebook.ui.viewmodel.MainViewModel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.extra.SuperDropdown
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    onBackupClick: () -> Unit = {},
    onRestoreClick: () -> Unit = {},
    onBandSettingsClick: () -> Unit = {}
) {
    val showRecentImport by viewModel.showRecentImport.collectAsState()
    val showRecentUpdate by viewModel.showRecentUpdate.collectAsState()
    val showSearchBar by viewModel.showSearchBar.collectAsState()
    val autoCheckUpdates by viewModel.autoCheckUpdates.collectAsState()
    val showConnectionError by viewModel.showConnectionError.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val quickEditCategoryEnabled by viewModel.quickEditCategoryEnabled.collectAsState()
    val quickRenameCategoryEnabled by viewModel.quickRenameCategoryEnabled.collectAsState()
    val autoMinimizeOnTransfer by viewModel.autoMinimizeOnTransfer.collectAsState()
    val autoRetryOnTransferError by viewModel.autoRetryOnTransferError.collectAsState()
    val bandTransferEnabled by viewModel.bandTransferEnabled.collectAsState()
    val useFloatingNavigationBar by viewModel.useFloatingNavigationBar.collectAsState()
    val useBuiltinFileManager by viewModel.useBuiltinFileManager.collectAsState()
    val autoDeleteSourceAfterImport by viewModel.autoDeleteSourceAfterImport.collectAsState()
    val showDonateCard by viewModel.showDonateCard.collectAsState()

    val showAboutDialog = remember { mutableStateOf(false) }
    val showDeleteReadingTimeDialog = remember { mutableStateOf(false) }
    val showCleanDirtyDataDialog = remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            TopAppBar(
                title = "设置",
                largeTitle = "设置",
                scrollBehavior = scrollBehavior
            )
        },
        popupHost = {}
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .overScrollVertical()
                .scrollEndHaptic()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = 40.dp
            )
        ) {
            if (showDonateCard) {
                item {
                    Card(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.primaryContainer),
                        onClick = {
                            viewModel.dismissDonateCard()
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.luoxe.cn/docs/donate/"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "无法打开浏览器", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Favorites,
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "支持开发者",
                                    style = MiuixTheme.textStyles.title4,
                                    fontWeight = FontWeight.Bold,
                                    color = MiuixTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "弦电子书完全免费，如果你觉得好用，可以考虑请开发者喝杯奶茶或咖啡。",
                                    style = MiuixTheme.textStyles.footnote1,
                                    color = MiuixTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            if (bandTransferEnabled) {
                item {
                    SmallTitle(text = "设备", modifier = Modifier.padding(top = 12.dp))
                    Card(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        insideMargin = PaddingValues(0.dp)
                    ) {
                        SuperArrow(
                            title = "手环端设置",
                            summary = "修改手环端的各项设置项",
                            onClick = onBandSettingsClick
                        )
                    }
                }
            }

            item {
                SmallTitle(text = "功能", modifier = Modifier.padding(top = 12.dp))
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    insideMargin = PaddingValues(0.dp)
                ) {
                    BasicComponent(
                        title = "显示最近导入",
                        summary = "在主页顶部显示最近导入的书籍",
                        endActions = {
                            Switch(
                                checked = showRecentImport,
                                onCheckedChange = { viewModel.setShowRecentImport(it) })
                        }
                    )
                    BasicComponent(
                        title = "显示最近更新",
                        summary = "在主页显示最近阅读或更新的书籍",
                        endActions = {
                            Switch(
                                checked = showRecentUpdate,
                                onCheckedChange = { viewModel.setShowRecentUpdate(it) })
                        }
                    )
                    BasicComponent(
                        title = "显示搜索栏",
                        summary = "在主页顶部显示搜索框",
                        endActions = {
                            Switch(
                                checked = showSearchBar,
                                onCheckedChange = { viewModel.setShowSearchBar(it) })
                        }
                    )
                    BasicComponent(
                        title = "使用内置文件管理器",
                        summary = "开启后使用应用内文件浏览器；关闭后调用系统文件选择器",
                        endActions = {
                            Switch(
                                checked = useBuiltinFileManager,
                                onCheckedChange = { viewModel.setUseBuiltinFileManager(it) })
                        }
                    )
                    BasicComponent(
                        title = "导入后自动删除源文件",
                        summary = "导入成功后自动删除来源文件",
                        endActions = {
                            Switch(
                                checked = autoDeleteSourceAfterImport,
                                onCheckedChange = { viewModel.setAutoDeleteSourceAfterImport(it) })
                        }
                    )
                    BasicComponent(
                        title = "左滑快速分类",
                        summary = "书籍条目左滑可直接修改分类",
                        endActions = {
                            Switch(
                                checked = quickEditCategoryEnabled,
                                onCheckedChange = { viewModel.setQuickEditCategory(it) })
                        }
                    )
                    BasicComponent(
                        title = "长按分类改名",
                        summary = "长按分类标题栏可重命名分类",
                        endActions = {
                            Switch(
                                checked = quickRenameCategoryEnabled,
                                onCheckedChange = { viewModel.setQuickRenameCategory(it) })
                        }
                    )
                }
            }

            item {
                SmallTitle(text = "外观", modifier = Modifier.padding(top = 12.dp))
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    insideMargin = PaddingValues(0.dp)
                ) {
                    Column {
                        val themes = listOf("浅色", "深色", "跟随系统")
                        val selectedThemeIndex = when (themeMode) {
                            MainViewModel.ThemeMode.LIGHT -> 0
                            MainViewModel.ThemeMode.DARK -> 1
                            MainViewModel.ThemeMode.SYSTEM -> 2
                        }
                        SuperDropdown(
                            title = "应用主题",
                            summary = "选择应用主题",
                            items = themes,
                            selectedIndex = selectedThemeIndex,
                            onSelectedIndexChange = { index ->
                                val mode = when (index) {
                                    0 -> MainViewModel.ThemeMode.LIGHT
                                    1 -> MainViewModel.ThemeMode.DARK
                                    else -> MainViewModel.ThemeMode.SYSTEM
                                }
                                viewModel.setThemeMode(mode)
                            }
                        )
                        BasicComponent(
                            title = "悬浮导航栏",
                            summary = "启用后底部导航栏悬浮显示",
                            endActions = {
                                Switch(
                                    checked = useFloatingNavigationBar,
                                    onCheckedChange = { viewModel.setUseFloatingNavigationBar(it) })
                            }
                        )
                    }
                }
            }

            item {
                SmallTitle(text = "同步与连接", modifier = Modifier.padding(top = 12.dp))
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    insideMargin = PaddingValues(0.dp)
                ) {
                    BasicComponent(
                        title = "小米手环传输",
                        summary = "启用与小米手环的连接与传输功能",
                        endActions = {
                            Switch(
                                checked = bandTransferEnabled,
                                onCheckedChange = { viewModel.setBandTransferEnabled(it) })
                        }
                    )
                    if (bandTransferEnabled) {
                        BasicComponent(
                            title = "传输后自动后台",
                            summary = "开始传输后自动将应用最小化",
                            endActions = {
                                Switch(
                                    checked = autoMinimizeOnTransfer,
                                    onCheckedChange = { viewModel.setAutoMinimizeOnTransfer(it) })
                            }
                        )
                        BasicComponent(
                            title = "自动重试中断",
                            summary = "传输中断时每5秒自动尝试重连",
                            endActions = {
                                Switch(
                                    checked = autoRetryOnTransferError,
                                    onCheckedChange = { viewModel.setAutoRetryOnTransferError(it) })
                            }
                        )
                        BasicComponent(
                            title = "连接失败提示",
                            summary = "连接手环失败时弹出详细提示",
                            endActions = {
                                Switch(
                                    checked = showConnectionError,
                                    onCheckedChange = { viewModel.setShowConnectionError(it) })
                            }
                        )
                    }
                }
            }

            item {
                SmallTitle(text = "更新与隐私", modifier = Modifier.padding(top = 12.dp))
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    insideMargin = PaddingValues(0.dp)
                ) {
                    BasicComponent(
                        title = "自动检查更新",
                        summary = "应用启动时自动检测新版本",
                        endActions = {
                            Switch(
                                checked = autoCheckUpdates,
                                onCheckedChange = { viewModel.setAutoCheckUpdates(it) })
                        }
                    )
                    SuperArrow(
                        title = "检查更新",
                        summary = "手动检查应用版本更新",
                        onClick = { viewModel.checkForUpdates() }
                    )
                }
            }

            item {
                SmallTitle(text = "教程与支持", modifier = Modifier.padding(top = 12.dp))
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    insideMargin = PaddingValues(0.dp)
                ) {
                    SuperArrow(
                        title = "打开疑难杂症文档",
                        summary = "查看常见问题解决方案",
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.luoxe.cn/docs/sine/book/question/"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "无法打开浏览器", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    SuperArrow(
                        title = "捐赠",
                        summary = "支持开发者",
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.luoxe.cn/docs/donate/"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "无法打开浏览器", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            item {
                SmallTitle(text = "高级", modifier = Modifier.padding(top = 12.dp))
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    insideMargin = PaddingValues(0.dp)
                ) {
                    SuperArrow(
                        title = "导出数据",
                        summary = "备份阅读时长和阅读进度",
                        onClick = onBackupClick
                    )
                    SuperArrow(
                        title = "导入数据",
                        summary = "恢复备份的阅读数据",
                        onClick = onRestoreClick
                    )
                    SuperArrow(
                        title = "清理脏数据",
                        summary = "删除无效书籍记录及其阅读进度和阅读时长(不可恢复)",
                        onClick = { showCleanDirtyDataDialog.value = true }
                    )
                    SuperArrow(
                        title = "清除阅读记录",
                        summary = "删除本地所有阅读时长数据(不可恢复)",
                        titleColor = BasicComponentDefaults.titleColor(color = MiuixTheme.colorScheme.error),
                        onClick = { showDeleteReadingTimeDialog.value = true }
                    )
                }
            }

            item {
                SmallTitle(text = "其他", modifier = Modifier.padding(top = 12.dp))
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 16.dp),
                    insideMargin = PaddingValues(0.dp)
                ) {
                    SuperArrow(
                        title = "关于",
                        summary = "版本信息与开发者",
                        onClick = { showAboutDialog.value = true }
                    )
                }
            }
        }
    }

    SuperDialog(
        title = "删除所有阅读时长",
        summary = "确定要删除手机端本地所有阅读时长数据吗？此操作不可恢复。",
        show = showDeleteReadingTimeDialog,
        onDismissRequest = { showDeleteReadingTimeDialog.value = false }
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(
                text = "取消",
                onClick = { showDeleteReadingTimeDialog.value = false },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(20.dp))
            TextButton(
                text = "删除",
                onClick = {
                    viewModel.clearAllReadingTimeData()
                    showDeleteReadingTimeDialog.value = false
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColors(
                    color = MiuixTheme.colorScheme.error,
                    textColor = MiuixTheme.colorScheme.onError
                )
            )
        }
    }

    SuperDialog(
        title = "清理脏数据",
        summary = "将删除数据库中无效的书籍记录，以及不存在书籍的阅读进度和阅读时长。此操作不可恢复，确定继续？",
        show = showCleanDirtyDataDialog,
        onDismissRequest = { showCleanDirtyDataDialog.value = false }
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(
                text = "取消",
                onClick = { showCleanDirtyDataDialog.value = false },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(20.dp))
            TextButton(
                text = "清理",
                onClick = {
                    viewModel.cleanDirtyData()
                    showCleanDirtyDataDialog.value = false
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColors(
                    color = MiuixTheme.colorScheme.error,
                    textColor = MiuixTheme.colorScheme.onError
                )
            )
        }
    }


    AboutDialog(showDialog = showAboutDialog)
}

@Composable
private fun SettingsIcon(
    imageVector: ImageVector,
    tint: androidx.compose.ui.graphics.Color = MiuixTheme.colorScheme.onSurfaceVariantActions
) {
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.padding(end = 16.dp)
    )
}
