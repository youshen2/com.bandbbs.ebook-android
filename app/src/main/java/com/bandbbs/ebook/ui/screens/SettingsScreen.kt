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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bandbbs.ebook.ui.components.AboutBottomSheet
import com.bandbbs.ebook.ui.components.UpdateCheckBottomSheet
import com.bandbbs.ebook.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

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
    val updateCheckState by viewModel.updateCheckState.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val quickEditCategoryEnabled by viewModel.quickEditCategoryEnabled.collectAsState()
    val autoMinimizeOnTransfer by viewModel.autoMinimizeOnTransfer.collectAsState()

    val scope = rememberCoroutineScope()
    val aboutSheetState = rememberModalBottomSheetState()
    val updateCheckSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 80.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))


            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 0.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "显示设置",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp),
                        color = MaterialTheme.colorScheme.primary
                    )

                    SettingItem(
                        title = "显示最近导入",
                        description = "在主页显示最近导入的书籍",
                        checked = showRecentImport,
                        onCheckedChange = { viewModel.setShowRecentImport(it) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    SettingItem(
                        title = "显示最近更新",
                        description = "在主页显示最近更新的书籍",
                        checked = showRecentUpdate,
                        onCheckedChange = { viewModel.setShowRecentUpdate(it) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    SettingItem(
                        title = "显示搜索栏",
                        description = "在主页显示搜索栏",
                        checked = showSearchBar,
                        onCheckedChange = { viewModel.setShowSearchBar(it) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    SettingItem(
                        title = "左滑快速修改分类",
                        description = "在书籍条目上左滑直接修改本地分类",
                        checked = quickEditCategoryEnabled,
                        onCheckedChange = { viewModel.setQuickEditCategory(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 0.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "传输设置",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp),
                        color = MaterialTheme.colorScheme.primary
                    )

                    SettingItem(
                        title = "开始传输后自动退出到后台",
                        description = "开始传输后自动将应用最小化到后台",
                        checked = autoMinimizeOnTransfer,
                        onCheckedChange = { viewModel.setAutoMinimizeOnTransfer(it) }
                    )

                    Column {
                        Text(
                            text = "主题设置",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp, top = 8.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.setThemeMode(MainViewModel.ThemeMode.LIGHT) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (themeMode == MainViewModel.ThemeMode.LIGHT) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (themeMode == MainViewModel.ThemeMode.LIGHT) 2.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Outlined.LightMode,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = if (themeMode == MainViewModel.ThemeMode.LIGHT) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "浅色",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (themeMode == MainViewModel.ThemeMode.LIGHT) FontWeight.Medium else FontWeight.Normal,
                                        color = if (themeMode == MainViewModel.ThemeMode.LIGHT) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }


                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.setThemeMode(MainViewModel.ThemeMode.DARK) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (themeMode == MainViewModel.ThemeMode.DARK) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (themeMode == MainViewModel.ThemeMode.DARK) 2.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Outlined.DarkMode,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = if (themeMode == MainViewModel.ThemeMode.DARK) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "深色",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (themeMode == MainViewModel.ThemeMode.DARK) FontWeight.Medium else FontWeight.Normal,
                                        color = if (themeMode == MainViewModel.ThemeMode.DARK) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }


                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.setThemeMode(MainViewModel.ThemeMode.SYSTEM) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (themeMode == MainViewModel.ThemeMode.SYSTEM) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (themeMode == MainViewModel.ThemeMode.SYSTEM) 2.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Outlined.SettingsBrightness,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = if (themeMode == MainViewModel.ThemeMode.SYSTEM) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "跟随系统",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (themeMode == MainViewModel.ThemeMode.SYSTEM) FontWeight.Medium else FontWeight.Normal,
                                        color = if (themeMode == MainViewModel.ThemeMode.SYSTEM) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))


            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 0.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "更新设置",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp),
                        color = MaterialTheme.colorScheme.primary
                    )

                    SettingItem(
                        title = "自动检查更新",
                        description = "应用启动时自动检查版本更新",
                        checked = autoCheckUpdates,
                        onCheckedChange = { viewModel.setAutoCheckUpdates(it) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    SettingItem(
                        title = "允许IP收集",
                        description = "允许收集设备IP用于版本更新检测",
                        checked = ipCollectionAllowed,
                        onCheckedChange = { viewModel.setIpCollectionAllowed(it) }
                    )

                    if (ipCollectionAllowed) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        androidx.compose.material3.ListItem(
                            headlineContent = {
                                Text(
                                    text = "检查更新",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = "手动检查应用版本更新",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = {
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            colors = androidx.compose.material3.ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.checkForUpdates()
                                }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))


            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 0.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "连接设置",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp),
                        color = MaterialTheme.colorScheme.primary
                    )

                    SettingItem(
                        title = "连接失败时弹出提示",
                        description = "连接失败时显示错误提示抽屉",
                        checked = showConnectionError,
                        onCheckedChange = { viewModel.setShowConnectionError(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))


            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 0.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "调试",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp),
                        color = MaterialTheme.colorScheme.primary
                    )

                    androidx.compose.material3.ListItem(
                        headlineContent = {
                            Text(
                                text = "删除所有阅读时长",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        supportingContent = {
                            Text(
                                text = "删除手机端本地所有阅读时长数据",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingContent = {
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        colors = androidx.compose.material3.ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showDeleteReadingTimeDialog = true
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))


            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 0.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "关于",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp),
                        color = MaterialTheme.colorScheme.primary
                    )

                    androidx.compose.material3.ListItem(
                        headlineContent = {
                            Text(
                                text = "关于",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supportingContent = {
                            Text(
                                text = "查看应用版本和开发者信息",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingContent = {
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = androidx.compose.material3.ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAboutSheet = true
                            }
                    )
                }
            }
        }
    }


    if (showDeleteReadingTimeDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteReadingTimeDialog = false },
            title = { Text("删除所有阅读时长") },
            text = { Text("确定要删除手机端本地所有阅读时长数据吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllReadingTimeData()
                        showDeleteReadingTimeDialog = false
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
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


    if (updateCheckState.showSheet) {
        LaunchedEffect(updateCheckState.showSheet) {
            updateCheckSheetState.show()
        }
        ModalBottomSheet(
            onDismissRequest = {
                scope.launch {
                    updateCheckSheetState.hide()
                    viewModel.dismissUpdateCheck()
                }
            },
            sheetState = updateCheckSheetState
        ) {
            UpdateCheckBottomSheet(
                isChecking = updateCheckState.isChecking,
                updateInfo = updateCheckState.updateInfo,
                updateInfoList = updateCheckState.updateInfoList,
                errorMessage = updateCheckState.errorMessage,
                deviceName = updateCheckState.deviceName,
                onDismiss = {
                    scope.launch {
                        updateCheckSheetState.hide()
                        viewModel.dismissUpdateCheck()
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    androidx.compose.material3.ListItem(
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        },
        supportingContent = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        },
        colors = androidx.compose.material3.ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    )
}

