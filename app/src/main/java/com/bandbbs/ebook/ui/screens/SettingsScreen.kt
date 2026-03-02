package com.bandbbs.ebook.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bandbbs.ebook.ui.components.AboutBottomSheet
import com.bandbbs.ebook.ui.viewmodel.MainViewModel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Show
import top.yukonga.miuix.kmp.icon.extended.Theme
import top.yukonga.miuix.kmp.icon.extended.Update
import top.yukonga.miuix.kmp.icon.extended.UploadCloud
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

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
    val ipCollectionAllowed by viewModel.ipCollectionAllowed.collectAsState()
    val showConnectionError by viewModel.showConnectionError.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val quickEditCategoryEnabled by viewModel.quickEditCategoryEnabled.collectAsState()
    val quickRenameCategoryEnabled by viewModel.quickRenameCategoryEnabled.collectAsState()
    val autoMinimizeOnTransfer by viewModel.autoMinimizeOnTransfer.collectAsState()
    val autoRetryOnTransferError by viewModel.autoRetryOnTransferError.collectAsState()
    val bandTransferEnabled by viewModel.bandTransferEnabled.collectAsState()

    val showAboutSheet = remember { mutableStateOf(false) }
    val showDeleteReadingTimeDialog = remember { mutableStateOf(false) }
    val showGetLatestVersionDialog = remember { mutableStateOf(false) }
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
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = 40.dp
            )
        ) {
            if (bandTransferEnabled) {
                item {
                    SmallTitle(text = "设备")
                    Card(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        insideMargin = PaddingValues(0.dp)
                    ) {
                        SuperArrow(
                            title = "手环端设置",
                            summary = "修改手环端的各项设置项",
                            startAction = { SettingsIcon(MiuixIcons.Settings) },
                            onClick = onBandSettingsClick
                        )
                    }
                }
            }

            item {
                SmallTitle(text = "显示与交互")
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    insideMargin = PaddingValues(0.dp)
                ) {
                    BasicComponent(
                        title = "显示最近导入",
                        summary = "在主页顶部显示最近导入的书籍",
                        startAction = { SettingsIcon(MiuixIcons.Show) },
                        endActions = {
                            Switch(
                                checked = showRecentImport,
                                onCheckedChange = { viewModel.setShowRecentImport(it) })
                        }
                    )
                    BasicComponent(
                        title = "显示最近更新",
                        summary = "在主页显示最近阅读或更新的书籍",
                        startAction = { SettingsIcon(MiuixIcons.Show) },
                        endActions = {
                            Switch(
                                checked = showRecentUpdate,
                                onCheckedChange = { viewModel.setShowRecentUpdate(it) })
                        }
                    )
                    BasicComponent(
                        title = "显示搜索栏",
                        summary = "在主页顶部显示搜索框",
                        startAction = { SettingsIcon(MiuixIcons.Show) },
                        endActions = {
                            Switch(
                                checked = showSearchBar,
                                onCheckedChange = { viewModel.setShowSearchBar(it) })
                        }
                    )
                    BasicComponent(
                        title = "左滑快速分类",
                        summary = "书籍条目左滑可直接修改分类",
                        startAction = { SettingsIcon(MiuixIcons.Edit) },
                        endActions = {
                            Switch(
                                checked = quickEditCategoryEnabled,
                                onCheckedChange = { viewModel.setQuickEditCategory(it) })
                        }
                    )
                    BasicComponent(
                        title = "长按分类改名",
                        summary = "长按分类标题栏可重命名分类",
                        startAction = { SettingsIcon(MiuixIcons.Edit) },
                        endActions = {
                            Switch(
                                checked = quickRenameCategoryEnabled,
                                onCheckedChange = { viewModel.setQuickRenameCategory(it) })
                        }
                    )
                }
            }

            item {
                SmallTitle(text = "外观")
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    insideMargin = PaddingValues(0.dp)
                ) {
                    Column(modifier = Modifier.padding(vertical = 12.dp)) {
                        BasicComponent(
                            title = "应用主题",
                            startAction = { SettingsIcon(MiuixIcons.Theme) },
                            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                        )

                        val themes = listOf("浅色", "深色", "跟随系统")
                        val selectedThemeIndex = when (themeMode) {
                            MainViewModel.ThemeMode.LIGHT -> 0
                            MainViewModel.ThemeMode.DARK -> 1
                            MainViewModel.ThemeMode.SYSTEM -> 2
                        }
                        TabRowWithContour(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            tabs = themes,
                            selectedTabIndex = selectedThemeIndex,
                            onTabSelected = { index ->
                                val mode = when (index) {
                                    0 -> MainViewModel.ThemeMode.LIGHT
                                    1 -> MainViewModel.ThemeMode.DARK
                                    else -> MainViewModel.ThemeMode.SYSTEM
                                }
                                viewModel.setThemeMode(mode)
                            }
                        )
                    }
                }
            }

            item {
                SmallTitle(text = "同步与连接")
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    insideMargin = PaddingValues(0.dp)
                ) {
                    BasicComponent(
                        title = "小米手环传输",
                        summary = "控制是否启用与小米手环的连接与传输功能",
                        startAction = { SettingsIcon(MiuixIcons.Refresh) },
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
                            startAction = { SettingsIcon(MiuixIcons.Refresh) },
                            endActions = {
                                Switch(
                                    checked = autoMinimizeOnTransfer,
                                    onCheckedChange = { viewModel.setAutoMinimizeOnTransfer(it) })
                            }
                        )
                        BasicComponent(
                            title = "自动重试中断",
                            summary = "传输中断时每5秒自动尝试重连",
                            startAction = { SettingsIcon(MiuixIcons.Refresh) },
                            endActions = {
                                Switch(
                                    checked = autoRetryOnTransferError,
                                    onCheckedChange = { viewModel.setAutoRetryOnTransferError(it) })
                            }
                        )
                        BasicComponent(
                            title = "连接失败提示",
                            summary = "连接手环失败时弹出详细提示",
                            startAction = { SettingsIcon(MiuixIcons.Info) },
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
                SmallTitle(text = "更新与隐私")
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    insideMargin = PaddingValues(0.dp)
                ) {
                    BasicComponent(
                        title = "自动检查更新",
                        summary = "应用启动时自动检测新版本",
                        startAction = { SettingsIcon(MiuixIcons.Update) },
                        endActions = {
                            Switch(
                                checked = autoCheckUpdates,
                                onCheckedChange = { viewModel.setAutoCheckUpdates(it) })
                        }
                    )
                    BasicComponent(
                        title = "允许联网",
                        summary = "允许应用联网以检查更新等功能",
                        startAction = { SettingsIcon(MiuixIcons.Lock) },
                        endActions = {
                            Switch(
                                checked = ipCollectionAllowed,
                                onCheckedChange = { viewModel.setIpCollectionAllowed(it) })
                        }
                    )
                    if (ipCollectionAllowed) {
                        SuperArrow(
                            title = "检查更新",
                            summary = "手动检查应用版本更新",
                            startAction = { SettingsIcon(MiuixIcons.Update) },
                            onClick = { viewModel.checkForUpdates() }
                        )
                    }
                    SuperArrow(
                        title = "获取最新版本",
                        summary = "跳转到所有版本的下载页面",
                        startAction = { SettingsIcon(MiuixIcons.Download) },
                        onClick = { showGetLatestVersionDialog.value = true }
                    )
                }
            }

            item {
                SmallTitle(text = "高级")
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    insideMargin = PaddingValues(0.dp)
                ) {
                    SuperArrow(
                        title = "导出数据",
                        summary = "备份阅读时长和阅读进度",
                        startAction = { SettingsIcon(MiuixIcons.UploadCloud) },
                        onClick = onBackupClick
                    )
                    SuperArrow(
                        title = "导入数据",
                        summary = "恢复备份的阅读数据",
                        startAction = { SettingsIcon(MiuixIcons.Download) },
                        onClick = onRestoreClick
                    )
                    SuperArrow(
                        title = "清除阅读记录",
                        summary = "删除本地所有阅读时长数据(不可恢复)",
                        titleColor = BasicComponentDefaults.titleColor(color = MiuixTheme.colorScheme.error),
                        startAction = {
                            SettingsIcon(
                                MiuixIcons.Delete,
                                tint = MiuixTheme.colorScheme.error
                            )
                        },
                        onClick = { showDeleteReadingTimeDialog.value = true }
                    )
                    SuperArrow(
                        title = "清理脏数据",
                        summary = "删除无效书籍记录及其阅读进度和阅读时长(不可恢复)",
                        titleColor = BasicComponentDefaults.titleColor(color = MiuixTheme.colorScheme.error),
                        startAction = {
                            SettingsIcon(
                                MiuixIcons.Delete,
                                tint = MiuixTheme.colorScheme.error
                            )
                        },
                        onClick = { showCleanDirtyDataDialog.value = true }
                    )
                }
            }

            item {
                SmallTitle(text = "其他")
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 16.dp),
                    insideMargin = PaddingValues(0.dp)
                ) {
                    SuperArrow(
                        title = "关于",
                        summary = "版本信息与开发者",
                        startAction = { SettingsIcon(MiuixIcons.Info) },
                        onClick = { showAboutSheet.value = true }
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

    SuperDialog(
        title = "各文件夹前缀介绍",
        summary = "MiBand和BandPro前缀是给小米手环8Pro、9Pro用的\nRW前缀是给REDMI Watch5、6用的\nBand9前缀是给小米手环9和9NFC用的\nBand10前缀是给小米手环10用的",
        show = showGetLatestVersionDialog,
        onDismissRequest = { showGetLatestVersionDialog.value = false }
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(
                text = "取消",
                onClick = { showGetLatestVersionDialog.value = false },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(20.dp))
            TextButton(
                text = "确定",
                onClick = {
                    try {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://pan.quark.cn/s/47b6d6447142")
                        )
                        context.startActivity(intent)
                    } catch (e: Exception) {
                    }
                    showGetLatestVersionDialog.value = false
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary()
            )
        }
    }

    AboutBottomSheet(show = showAboutSheet)
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
