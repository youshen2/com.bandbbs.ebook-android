package com.bandbbs.ebook.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
    
    val scope = rememberCoroutineScope()
    val aboutSheetState = rememberModalBottomSheetState()
    val updateCheckSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAboutSheet by remember { mutableStateOf(false) }

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
                .padding(bottom = 80.dp) 
                .verticalScroll(rememberScrollState())
        ) {
            
            Text(
                text = "显示设置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                color = MaterialTheme.colorScheme.primary
            )

            SettingItem(
                title = "显示最近导入",
                description = "在主页显示最近导入的书籍",
                checked = showRecentImport,
                onCheckedChange = { viewModel.setShowRecentImport(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

            SettingItem(
                title = "显示最近更新",
                description = "在主页显示最近更新的书籍",
                checked = showRecentUpdate,
                onCheckedChange = { viewModel.setShowRecentUpdate(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

            SettingItem(
                title = "显示搜索栏",
                description = "在主页显示搜索栏",
                checked = showSearchBar,
                onCheckedChange = { viewModel.setShowSearchBar(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

            
            Text(
                text = "更新设置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                color = MaterialTheme.colorScheme.primary
            )

            SettingItem(
                title = "自动检查更新",
                description = "应用启动时自动检查版本更新",
                checked = autoCheckUpdates,
                onCheckedChange = { viewModel.setAutoCheckUpdates(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

            SettingItem(
                title = "允许IP收集",
                description = "允许收集设备IP用于版本更新检测",
                checked = ipCollectionAllowed,
                onCheckedChange = { viewModel.setIpCollectionAllowed(it) }
            )

            
            if (ipCollectionAllowed) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

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
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.checkForUpdates()
                        }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

            
            Text(
                text = "连接设置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                color = MaterialTheme.colorScheme.primary
            )

            SettingItem(
                title = "连接失败时弹出提示",
                description = "连接失败时显示错误提示抽屉",
                checked = showConnectionError,
                onCheckedChange = { viewModel.setShowConnectionError(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

            
            Text(
                text = "关于",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showAboutSheet = true
                    }
            )
        }
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
        modifier = Modifier.fillMaxWidth()
    )
}

