package com.bandbbs.ebook.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bandbbs.ebook.R
import com.bandbbs.ebook.ui.components.AboutBottomSheet
import com.bandbbs.ebook.ui.components.BookItem
import com.bandbbs.ebook.ui.components.CategoryBottomSheet
import com.bandbbs.ebook.ui.components.ChapterListBottomSheet
import com.bandbbs.ebook.ui.components.ChapterPreviewBottomSheet
import com.bandbbs.ebook.ui.components.ConnectionErrorBottomSheet
import com.bandbbs.ebook.ui.components.ImportBookBottomSheet
import com.bandbbs.ebook.ui.components.ImportProgressBottomSheet
import com.bandbbs.ebook.ui.components.ImportReportBottomSheet
import com.bandbbs.ebook.ui.components.OverwriteConfirmBottomSheet
import com.bandbbs.ebook.ui.components.PushBottomSheet
import com.bandbbs.ebook.ui.components.StatusCard
import com.bandbbs.ebook.ui.components.SyncOptionsBottomSheet
import com.bandbbs.ebook.ui.components.FirstSyncConfirmBottomSheet
import com.bandbbs.ebook.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onImportClick: () -> Unit,
    onImportCoverClick: () -> Unit
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val books by viewModel.books.collectAsState()
    val pushState by viewModel.pushState.collectAsState()
    val importState by viewModel.importState.collectAsState()
    val importingState by viewModel.importingState.collectAsState()
    val importReportState by viewModel.importReportState.collectAsState()
    val selectedBookForChapters by viewModel.selectedBookForChapters.collectAsState()
    val chapters by viewModel.chaptersForSelectedBook.collectAsState()
    val chapterToPreview by viewModel.chapterToPreview.collectAsState()
    val bookToDelete by viewModel.bookToDelete.collectAsState()
    val syncOptionsState by viewModel.syncOptionsState.collectAsState()
    val overwriteConfirmState by viewModel.overwriteConfirmState.collectAsState()
    val bookForCoverImport by viewModel.bookForCoverImport.collectAsState()
    val connectionErrorState by viewModel.connectionErrorState.collectAsState()
    val categoryState by viewModel.categoryState.collectAsState()
    val firstSyncConfirmState by viewModel.firstSyncConfirmState.collectAsState()

    val scope = rememberCoroutineScope()
    val aboutSheetState = rememberModalBottomSheetState()
    var showAboutSheet by remember { mutableStateOf(false) }
    val categorySheetState = rememberModalBottomSheetState()
    val firstSyncConfirmSheetState = rememberModalBottomSheetState()

    val pushSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val importSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val chapterListSheetState = rememberModalBottomSheetState()
    val chapterPreviewSheetState = rememberModalBottomSheetState()
    val importProgressSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val importReportSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val syncOptionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val overwriteConfirmSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val connectionErrorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    bookToDelete?.let { book ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelDeleteBook() },
            title = { Text("删除书籍") },
            text = { Text("确定要删除《${book.name}》吗？此操作将同时删除文件和所有章节数据，且不可恢复。") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDeleteBook() }) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDeleteBook() }) {
                    Text("取消")
                }
            }
        )
    }

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

    syncOptionsState?.let {
        ModalBottomSheet(
            onDismissRequest = { viewModel.cancelPush() },
            sheetState = syncOptionsSheetState
        ) {
            SyncOptionsBottomSheet(
                state = it,
                onCancel = {
                    scope.launch {
                        syncOptionsSheetState.hide()
                        viewModel.cancelPush()
                    }
                },
                onConfirm = { selectedChapters, syncCover ->
                    scope.launch {
                        syncOptionsSheetState.hide()
                        viewModel.confirmPush(it.book, selectedChapters, syncCover)
                    }
                },
                onResyncCoverOnly = {
                    scope.launch {
                        syncOptionsSheetState.hide()
                        viewModel.cancelPush()
                        viewModel.syncCoverOnly(it.book)
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

    firstSyncConfirmState?.let { book ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.cancelFirstSyncConfirm() },
            sheetState = firstSyncConfirmSheetState
        ) {
            FirstSyncConfirmBottomSheet(
                onConfirm = {
                    scope.launch {
                        firstSyncConfirmSheetState.hide()
                        viewModel.confirmFirstSync()
                    }
                },
                onCancel = {
                    scope.launch {
                        firstSyncConfirmSheetState.hide()
                        viewModel.cancelFirstSyncConfirm()
                    }
                }
            )
        }
    }

    importState?.let {
        ModalBottomSheet(
            onDismissRequest = { viewModel.cancelImport() },
            sheetState = importSheetState
        ) {
            ImportBookBottomSheet(
                state = it,
                categories = viewModel.getCategories(),
                onCancel = {
                    scope.launch {
                        importSheetState.hide()
                        viewModel.cancelImport()
                    }
                },
                onConfirm = { bookName, splitMethod, noSplit, wordsPerChapter, selectedCategory, enableChapterMerge, mergeMinWords, enableChapterRename, renamePattern ->
                    scope.launch {
                        importSheetState.hide()
                        viewModel.confirmImport(bookName, splitMethod, noSplit, wordsPerChapter, selectedCategory, enableChapterMerge, mergeMinWords, enableChapterRename, renamePattern)
                    }
                },
                onShowCategorySelector = {
                    viewModel.showCategorySelector()
                }
            )
        }
    }

    categoryState?.let { state ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissCategorySelector() },
            sheetState = categorySheetState
        ) {
            CategoryBottomSheet(
                categories = state.categories,
                selectedCategory = state.selectedCategory,
                onCategorySelected = { category ->
                    scope.launch {
                        categorySheetState.hide()
                        viewModel.selectCategory(category)
                    }
                },
                onCategoryCreated = { categoryName ->
                    viewModel.createCategory(categoryName)
                },
                onCategoryDeleted = { categoryName ->
                    viewModel.deleteCategory(categoryName)
                },
                onDismiss = {
                    scope.launch {
                        categorySheetState.hide()
                        viewModel.dismissCategorySelector()
                    }
                }
            )
        }
    }

    importingState?.let {
        ModalBottomSheet(
            onDismissRequest = { },
            sheetState = importProgressSheetState,
            dragHandle = null
        ) {
            ImportProgressBottomSheet(state = it)
        }
    }

    importReportState?.let {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissImportReport() },
            sheetState = importReportSheetState
        ) {
            ImportReportBottomSheet(
                state = it,
                onDismiss = {
                    scope.launch {
                        importReportSheetState.hide()
                        viewModel.dismissImportReport()
                    }
                }
            )
        }
    }

    selectedBookForChapters?.let { book ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeChapterList() },
            sheetState = chapterListSheetState
        ) {
            ChapterListBottomSheet(
                book = book,
                chapters = chapters,
                onChapterClick = { chapterId ->
                    viewModel.showChapterPreview(chapterId)
                }
            )
        }
    }

    chapterToPreview?.let { chapter ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeChapterPreview() },
            sheetState = chapterPreviewSheetState
        ) {
            ChapterPreviewBottomSheet(chapter = chapter)
        }
    }

    overwriteConfirmState?.let { state ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.cancelOverwriteConfirm() },
            sheetState = overwriteConfirmSheetState
        ) {
            OverwriteConfirmBottomSheet(
                existingBook = state.existingBook,
                newBookName = state.newBookName,
                onCancel = {
                    scope.launch {
                        overwriteConfirmSheetState.hide()
                        viewModel.cancelOverwriteConfirm()
                    }
                },
                onOverwrite = {
                    scope.launch {
                        overwriteConfirmSheetState.hide()
                        viewModel.confirmOverwrite()
                    }
                }
            )
        }
    }

    connectionErrorState?.let { state ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissConnectionError() },
            sheetState = connectionErrorSheetState
        ) {
            ConnectionErrorBottomSheet(
                deviceName = state.deviceName,
                isUnsupportedDevice = state.isUnsupportedDevice,
                onDismiss = {
                    scope.launch {
                        connectionErrorSheetState.hide()
                        viewModel.dismissConnectionError()
                    }
                },
                onRetry = {
                    scope.launch {
                        connectionErrorSheetState.hide()
                        viewModel.dismissConnectionError()
                        // 等待sheet完全隐藏后再重连，避免状态冲突
                        kotlinx.coroutines.delay(300)
                        viewModel.reconnect()
                    }
                }
            )
        }
    }


    Scaffold(
        topBar = {
            var showMenu by remember { mutableStateOf(false) }
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = stringResource(id = R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = connectionState.statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多选项")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("重新连接手环") },
                            onClick = {
                                viewModel.reconnect()
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.Refresh, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("关于") },
                            onClick = {
                                showMenu = false
                                showAboutSheet = true
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.Info, contentDescription = null)
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onImportClick) {
                Icon(Icons.Default.Add, contentDescription = "导入书籍")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
//                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
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
                            onDeleteClick = { viewModel.requestDeleteBook(book) },
                            onSyncClick = { viewModel.startPush(book) },
                            onCardClick = { viewModel.showChapterList(book) },
                            onImportCoverClick = { 
                                viewModel.requestImportCover(book)
                                onImportCoverClick()
                            },
                            onCategoryClick = {
                                viewModel.showCategorySelector(book)
                            },
                            isSyncEnabled = connectionState.isConnected
                        )
                    }
                }
            }
        }
    }
}
