package com.bandbbs.ebook.ui.screens

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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bandbbs.ebook.ui.components.AboutBottomSheet
import com.bandbbs.ebook.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBackClick: () -> Unit
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

    val aboutSheetState = rememberModalBottomSheetState()
    var showAboutSheet by remember { mutableStateOf(false) }
    var showDeleteReadingTimeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                top = 8.dp,
                end = 16.dp,
                bottom = 100.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Group: Display & Interaction
            item {
                SettingsGroup(title = "显示与交互") {
                    SettingsTile(
                        icon = Icons.Outlined.Visibility,
                        title = "显示最近导入",
                        description = "在主页顶部显示最近导入的书籍",
                        checked = showRecentImport,
                        onCheckedChange = { viewModel.setShowRecentImport(it) }
                    )
                    SettingsTile(
                        icon = Icons.Outlined.Visibility,
                        title = "显示最近更新",
                        description = "在主页显示最近阅读或更新的书籍",
                        checked = showRecentUpdate,
                        onCheckedChange = { viewModel.setShowRecentUpdate(it) }
                    )
                    SettingsTile(
                        icon = Icons.Outlined.Visibility,
                        title = "显示搜索栏",
                        description = "在主页顶部显示搜索框",
                        checked = showSearchBar,
                        onCheckedChange = { viewModel.setShowSearchBar(it) }
                    )
                    SettingsTile(
                        icon = Icons.Outlined.Gesture,
                        title = "左滑快速分类",
                        description = "书籍条目左滑可直接修改分类",
                        checked = quickEditCategoryEnabled,
                        onCheckedChange = { viewModel.setQuickEditCategory(it) }
                    )
                    SettingsTile(
                        icon = Icons.Outlined.Gesture,
                        title = "长按分类改名",
                        description = "长按分类标题栏可重命名分类",
                        checked = quickRenameCategoryEnabled,
                        onCheckedChange = { viewModel.setQuickRenameCategory(it) }
                    )
                }
            }

            // Group: Appearance
            item {
                SettingsGroup(title = "外观") {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "应用主题",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeSelectionChip(
                                selected = themeMode == MainViewModel.ThemeMode.LIGHT,
                                label = "浅色",
                                icon = Icons.Outlined.LightMode,
                                onClick = { viewModel.setThemeMode(MainViewModel.ThemeMode.LIGHT) },
                                modifier = Modifier.weight(1f)
                            )
                            ThemeSelectionChip(
                                selected = themeMode == MainViewModel.ThemeMode.DARK,
                                label = "深色",
                                icon = Icons.Outlined.DarkMode,
                                onClick = { viewModel.setThemeMode(MainViewModel.ThemeMode.DARK) },
                                modifier = Modifier.weight(1f)
                            )
                            ThemeSelectionChip(
                                selected = themeMode == MainViewModel.ThemeMode.SYSTEM,
                                label = "跟随系统",
                                icon = Icons.Outlined.SettingsBrightness,
                                onClick = { viewModel.setThemeMode(MainViewModel.ThemeMode.SYSTEM) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Group: Sync & Connection
            item {
                SettingsGroup(title = "同步与连接") {
                    SettingsTile(
                        icon = Icons.Outlined.Sync,
                        title = "传输后自动后台",
                        description = "开始传输后自动将应用最小化",
                        checked = autoMinimizeOnTransfer,
                        onCheckedChange = { viewModel.setAutoMinimizeOnTransfer(it) }
                    )
                    SettingsTile(
                        icon = Icons.Outlined.Sync,
                        title = "自动重试中断",
                        description = "传输中断时每5秒自动尝试重连",
                        checked = autoRetryOnTransferError,
                        onCheckedChange = { viewModel.setAutoRetryOnTransferError(it) }
                    )
                    SettingsTile(
                        icon = Icons.Outlined.Warning,
                        title = "连接失败提示",
                        description = "连接手环失败时弹出详细提示",
                        checked = showConnectionError,
                        onCheckedChange = { viewModel.setShowConnectionError(it) }
                    )
                }
            }

            // Group: Updates & Privacy
            item {
                SettingsGroup(title = "更新与隐私") {
                    SettingsTile(
                        icon = Icons.Outlined.SystemUpdate,
                        title = "自动检查更新",
                        description = "应用启动时自动检测新版本",
                        checked = autoCheckUpdates,
                        onCheckedChange = { viewModel.setAutoCheckUpdates(it) }
                    )
                    SettingsTile(
                        icon = Icons.Outlined.Security,
                        title = "允许IP收集",
                        description = "允许收集IP用于更新检测统计",
                        checked = ipCollectionAllowed,
                        onCheckedChange = { viewModel.setIpCollectionAllowed(it) }
                    )
                    if (ipCollectionAllowed) {
                        SettingsActionTile(
                            icon = Icons.Outlined.SystemUpdate,
                            title = "检查更新",
                            description = "手动检查应用版本更新",
                            onClick = { viewModel.checkForUpdates() }
                        )
                    }
                }
            }

            // Group: Advanced
            item {
                SettingsGroup(title = "高级") {
                    SettingsActionTile(
                        icon = Icons.Outlined.DeleteForever,
                        title = "清除阅读记录",
                        description = "删除本地所有阅读时长数据(不可恢复)",
                        onClick = { showDeleteReadingTimeDialog = true },
                        isDestructive = true
                    )
                }
            }

            // Group: About
            item {
                SettingsGroup {
                    SettingsActionTile(
                        icon = Icons.Outlined.Info,
                        title = "关于",
                        description = "版本信息与开发者",
                        onClick = { showAboutSheet = true }
                    )
                }
            }
        }
    }

    if (showDeleteReadingTimeDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteReadingTimeDialog = false },
            icon = { Icon(Icons.Outlined.Build, contentDescription = null) },
            title = { Text("删除所有阅读时长") },
            text = { Text("确定要删除手机端本地所有阅读时长数据吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllReadingTimeData()
                        showDeleteReadingTimeDialog = false
                    },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteReadingTimeDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showAboutSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAboutSheet = false },
            sheetState = aboutSheetState
        ) {
            AboutBottomSheet()
        }
    }
}

@Composable
fun SettingsGroup(
    title: String? = null,
    content: @Composable () -> Unit
) {
    Column {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsTile(
    icon: ImageVector,
    title: String,
    description: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        },
        supportingContent = description?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    )
}

@Composable
fun SettingsActionTile(
    icon: ImageVector,
    title: String,
    description: String? = null,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    val contentColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    val titleColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface

    ListItem(
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = titleColor
            )
        },
        supportingContent = description?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun ThemeSelectionChip(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
