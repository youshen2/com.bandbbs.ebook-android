package com.bandbbs.ebook.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bandbbs.ebook.R
import com.bandbbs.ebook.ui.components.AboutBottomSheet
import com.bandbbs.ebook.ui.components.BookItem
import com.bandbbs.ebook.ui.components.PushBottomSheet
import com.bandbbs.ebook.ui.components.StatusCard
import com.bandbbs.ebook.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onImportClick: () -> Unit
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val books by viewModel.books.collectAsState()
    val pushState by viewModel.pushState.collectAsState()

    val scope = rememberCoroutineScope()
    val aboutSheetState = rememberModalBottomSheetState()
    var showAboutSheet by remember { mutableStateOf(false) }

    val pushSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (pushState.book != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.cancelPush() },
            sheetState = pushSheetState,
        ) {
            PushBottomSheet(
                pushState = pushState,
                onCancel = {
                    scope.launch {
                        pushSheetState.hide()
                        viewModel.cancelPush()
                    }
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


    Scaffold(
        topBar = {
            var showMenu by remember { mutableStateOf(false) }
            TopAppBar(
                title = { Text(stringResource(id = R.string.app_name)) },
                actions = {
                    IconButton(onClick = onImportClick) {
                        Icon(Icons.Default.Add, contentDescription = "导入书籍")
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("关于") },
                            onClick = {
                                showMenu = false
                                showAboutSheet = true
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            StatusCard(
                statusText = connectionState.statusText,
                descriptionText = connectionState.descriptionText,
                isConnected = connectionState.isConnected,
                onClick = { viewModel.reconnect() }
            )

            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(books, key = { it.path }) { book ->
                    AnimatedVisibility(
                        visible = true,
                        enter = scaleIn(),
                        exit = scaleOut()
                    ) {
                        BookItem(
                            book = book,
                            onDeleteClick = { viewModel.deleteBook(book) },
                            onSyncClick = { viewModel.startPush(book) },
                            isSyncEnabled = connectionState.isConnected
                        )
                    }
                }
            }
        }
    }
}
