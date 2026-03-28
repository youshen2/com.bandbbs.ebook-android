package com.bandbbs.ebook.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.bandbbs.ebook.database.AppDatabase
import com.bandbbs.ebook.ui.components.CategoryDialog
import com.bandbbs.ebook.ui.components.ConnectionErrorBottomSheet
import com.bandbbs.ebook.ui.components.EditBookInfoBottomSheet
import com.bandbbs.ebook.ui.components.ImportBookBottomSheet
import com.bandbbs.ebook.ui.components.ImportProgressDialog
import com.bandbbs.ebook.ui.components.ImportReportBottomSheet
import com.bandbbs.ebook.ui.components.OverwriteConfirmDialog
import com.bandbbs.ebook.ui.components.SyncReadingDataConfirmDialog
import com.bandbbs.ebook.ui.components.SyncReadingDataDialog
import com.bandbbs.ebook.ui.components.VersionIncompatibleDialog
import com.bandbbs.ebook.ui.components.cell.BookItem
import com.bandbbs.ebook.ui.viewmodel.MainViewModel
import com.bandbbs.ebook.ui.viewmodel.SyncMode
import com.bandbbs.ebook.utils.StorageUtils
import com.bandbbs.ebook.utils.bytesToReadable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.extra.SuperBottomSheet
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.Help
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Recent
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.icon.extended.Update
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.HorizontalDivider

private enum class ItemType {
    RECENT_HEADER, RECENT_BOOK, RECENT_UPDATE_HEADER, RECENT_UPDATE_BOOK, CATEGORY_HEADER, BOOK
}

private data class ListItem(
    val type: ItemType,
    val category: String? = null,
    val book: com.bandbbs.ebook.ui.model.Book? = null
)

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onImportCoverClick: () -> Unit,
    onNavigateToSyncOptions: () -> Unit = {}
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val books by viewModel.books.collectAsState()
    val recentBook by viewModel.recentBook.collectAsState()
    val recentUpdatedBook by viewModel.recentUpdatedBook.collectAsState()
    val showRecentImport by viewModel.showRecentImport.collectAsState()
    val showRecentUpdate by viewModel.showRecentUpdate.collectAsState()
    val showSearchBar by viewModel.showSearchBar.collectAsState()
    val quickEditCategoryEnabled by viewModel.quickEditCategoryEnabled.collectAsState()
    val quickRenameCategoryEnabled by viewModel.quickRenameCategoryEnabled.collectAsState()
    val hasClickedTransferButton by viewModel.hasClickedTransferButton.collectAsState()
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsState()
    val selectedBooks by viewModel.selectedBooks.collectAsState()

    val importState by viewModel.importState.collectAsState()
    val importingState by viewModel.importingState.collectAsState()
    val importReportState by viewModel.importReportState.collectAsState()
    val bookToDelete by viewModel.bookToDelete.collectAsState()
    val booksToDelete by viewModel.booksToDelete.collectAsState()
    val syncOptionsState by viewModel.syncOptionsState.collectAsState()
    val overwriteConfirmState by viewModel.overwriteConfirmState.collectAsState()
    val largeChapterWarningState by viewModel.largeChapterWarningState.collectAsState()
    val connectionErrorState by viewModel.connectionErrorState.collectAsState()
    val connectionRequiredDialogState by viewModel.connectionRequiredDialogState.collectAsState()
    val categoryState by viewModel.categoryState.collectAsState()
    val editBookInfoState by viewModel.editBookInfoState.collectAsState()
    val syncReadingDataState by viewModel.syncReadingDataState.collectAsState()
    val syncResultState by viewModel.syncResultState.collectAsState()
    val versionIncompatibleState by viewModel.versionIncompatibleState.collectAsState()

    val bandStorageInfo by viewModel.bandStorageInfo.collectAsState()
    val bandTransferEnabled by viewModel.bandTransferEnabled.collectAsState()
    val expandedBookPath by viewModel.expandedBookPath.collectAsState()
    val expandedCategories by viewModel.expandedCategories.collectAsState()
    val updateCheckState by viewModel.updateCheckState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    val scrollState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lastChapterNames = remember { mutableStateMapOf<String, String>() }

    var pendingExportBook by remember { mutableStateOf<com.bandbbs.ebook.ui.model.Book?>(null) }
    val exportBookLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val book = pendingExportBook
        if (uri != null && book != null) {
            viewModel.exportBookToUri(book, uri)
        }
        pendingExportBook = null
    }

    LaunchedEffect(bandStorageInfo.totalStorage) {
        if (bandStorageInfo.totalStorage > 0 && !bandStorageInfo.isLoading) {
            delay(100)
            scrollState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(expandedBookPath) {
        if (expandedBookPath != null) {
            val book = books.find { it.path == expandedBookPath }
            if (book != null && !lastChapterNames.containsKey(book.path)) {
                val lastChapterName = withContext(Dispatchers.IO) {
                    val db = AppDatabase.getDatabase(context)
                    val bookEntity = db.bookDao().getBookByPath(book.path)
                    if (bookEntity != null) {
                        val chapters = db.chapterDao().getChapterInfoForBook(bookEntity.id)
                        if (chapters.isNotEmpty()) {
                            val lastChapter = chapters.maxByOrNull { it.index }
                            lastChapter?.name
                        } else null
                    } else null
                }
                lastChapterName?.let { lastChapterNames[book.path] = it }
            }
        }
    }

    val showDeleteDialog = remember { mutableStateOf(false) }
    LaunchedEffect(bookToDelete) { showDeleteDialog.value = bookToDelete != null }
    SuperDialog(
        title = "删除书籍",
        summary = "请选择删除方式：\n· 删除手机书籍：删除本机文件和所有章节数据，不影响手环端。\n· 删除手环书籍：仅删除手环端书籍数据，不删除手机端文件。",
        show = showDeleteDialog,
        onDismissRequest = { viewModel.cancelDeleteBook() }
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            if (bandTransferEnabled) {
                TextButton(
                    text = "删手环",
                    onClick = {
                        bookToDelete?.let { viewModel.deleteBandBook(it) }
                        Toast.makeText(context, "已发送手环删除指令", Toast.LENGTH_SHORT).show()
                        viewModel.cancelDeleteBook()
                    },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
            } else {
                TextButton(
                    text = "取消",
                    onClick = { viewModel.cancelDeleteBook() },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
            }
            TextButton(
                text = "删手机",
                colors = ButtonDefaults.textButtonColorsPrimary(),
                onClick = { viewModel.confirmDeleteBook() },
                modifier = Modifier.weight(1f)
            )
        }
    }

    val showBatchDeleteDialog = remember { mutableStateOf(false) }
    LaunchedEffect(booksToDelete) { showBatchDeleteDialog.value = booksToDelete.isNotEmpty() }
    SuperDialog(
        title = "批量删除书籍",
        summary = "确定要删除以下 ${booksToDelete.size} 本书籍吗？此操作将同时删除文件和所有章节数据，且不可恢复。\n" +
                booksToDelete.take(5).joinToString("\n") { "· ${it.name}" } +
                if (booksToDelete.size > 5) "\n... 还有 ${booksToDelete.size - 5} 本" else "",
        show = showBatchDeleteDialog,
        onDismissRequest = { viewModel.cancelDeleteSelectedBooks() }
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            TextButton(
                text = "取消",
                onClick = { viewModel.cancelDeleteSelectedBooks() },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(20.dp))
            TextButton(
                text = "删除",
                colors = ButtonDefaults.textButtonColorsPrimary(),
                onClick = { viewModel.confirmDeleteSelectedBooks() },
                modifier = Modifier.weight(1f)
            )
        }
    }

    val showSyncModeDialog = remember { mutableStateOf(false) }
    LaunchedEffect(syncReadingDataState.showModeDialog) {
        showSyncModeDialog.value = syncReadingDataState.showModeDialog
    }

    SuperDialog(
        title = "同步设置",
        show = showSyncModeDialog,
        onDismissRequest = { viewModel.dismissSyncModeDialog() }
    ) {
        var selectedProgressMode by remember { mutableStateOf(syncReadingDataState.progressSyncMode) }
        var selectedReadingTimeMode by remember { mutableStateOf(syncReadingDataState.readingTimeSyncMode) }
        var selectedBookmarkMode by remember { mutableStateOf(syncReadingDataState.bookmarkSyncMode) }
        val dialogScrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 450.dp)
                .verticalScroll(dialogScrollState)
        ) {
            SmallTitle(text = "选择要同步的书籍", insideMargin = PaddingValues(start = 4.dp, bottom = 8.dp))
            Card {
                val allSelected = syncReadingDataState.selectedBooksForSync.size == books.size && books.isNotEmpty()
                BasicComponent(
                    title = "全选",
                    endActions = {
                        Checkbox(
                            checked = allSelected,
                            onCheckedChange = { viewModel.selectAllBooksForSync(it) }
                        )
                    },
                    onClick = { viewModel.selectAllBooksForSync(!allSelected) }
                )
                if (books.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
                books.forEach { book ->
                    val isSelected = syncReadingDataState.selectedBooksForSync.contains(book.name)
                    BasicComponent(
                        title = book.name,
                        endActions = {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { viewModel.toggleBookForSync(book.name) }
                            )
                        },
                        onClick = { viewModel.toggleBookForSync(book.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SmallTitle(text = "阅读进度", insideMargin = PaddingValues(start = 4.dp, bottom = 8.dp))
            Card {
                BasicComponent(
                    title = "自动处理（默认）",
                    summary = "每本书的阅读进度以两端中最新的一个为准进行同步",
                    endActions = {
                        Checkbox(
                            checked = selectedProgressMode == SyncMode.AUTO,
                            onCheckedChange = { selectedProgressMode = SyncMode.AUTO })
                    },
                    onClick = { selectedProgressMode = SyncMode.AUTO }
                )
                BasicComponent(
                    title = "手环端", summary = "将手环端单向同步到手机端",
                    endActions = {
                        Checkbox(
                            checked = selectedProgressMode == SyncMode.BAND_ONLY,
                            onCheckedChange = { selectedProgressMode = SyncMode.BAND_ONLY })
                    },
                    onClick = { selectedProgressMode = SyncMode.BAND_ONLY }
                )
                BasicComponent(
                    title = "手机端", summary = "将手机端单向同步到手环端",
                    endActions = {
                        Checkbox(
                            checked = selectedProgressMode == SyncMode.PHONE_ONLY,
                            onCheckedChange = { selectedProgressMode = SyncMode.PHONE_ONLY })
                    },
                    onClick = { selectedProgressMode = SyncMode.PHONE_ONLY }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            SmallTitle(text = "书签", insideMargin = PaddingValues(start = 4.dp, bottom = 8.dp))
            Card {
                BasicComponent(
                    title = "自动处理（默认）", summary = "合并两端书签，优先保留手环端书签",
                    endActions = {
                        Checkbox(
                            checked = selectedBookmarkMode == SyncMode.AUTO,
                            onCheckedChange = { selectedBookmarkMode = SyncMode.AUTO })
                    },
                    onClick = { selectedBookmarkMode = SyncMode.AUTO }
                )
                BasicComponent(
                    title = "手环端", summary = "将手环端书签单向同步到手机端",
                    endActions = {
                        Checkbox(
                            checked = selectedBookmarkMode == SyncMode.BAND_ONLY,
                            onCheckedChange = { selectedBookmarkMode = SyncMode.BAND_ONLY })
                    },
                    onClick = { selectedBookmarkMode = SyncMode.BAND_ONLY }
                )
                BasicComponent(
                    title = "手机端", summary = "将手机端书签单向同步到手环端",
                    endActions = {
                        Checkbox(
                            checked = selectedBookmarkMode == SyncMode.PHONE_ONLY,
                            onCheckedChange = { selectedBookmarkMode = SyncMode.PHONE_ONLY })
                    },
                    onClick = { selectedBookmarkMode = SyncMode.PHONE_ONLY }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            SmallTitle(text = "阅读时长", insideMargin = PaddingValues(start = 4.dp, bottom = 8.dp))
            Card {
                BasicComponent(
                    title = "自动处理（默认）", summary = "按照阅读时长最长的一端为准进行同步",
                    endActions = {
                        Checkbox(
                            checked = selectedReadingTimeMode == SyncMode.AUTO,
                            onCheckedChange = { selectedReadingTimeMode = SyncMode.AUTO })
                    },
                    onClick = { selectedReadingTimeMode = SyncMode.AUTO }
                )
                BasicComponent(
                    title = "手环端", summary = "将手环端单向同步到手机端",
                    endActions = {
                        Checkbox(
                            checked = selectedReadingTimeMode == SyncMode.BAND_ONLY,
                            onCheckedChange = { selectedReadingTimeMode = SyncMode.BAND_ONLY })
                    },
                    onClick = { selectedReadingTimeMode = SyncMode.BAND_ONLY }
                )
                BasicComponent(
                    title = "手机端", summary = "将手机端单向同步到手环端",
                    endActions = {
                        Checkbox(
                            checked = selectedReadingTimeMode == SyncMode.PHONE_ONLY,
                            onCheckedChange = { selectedReadingTimeMode = SyncMode.PHONE_ONLY })
                    },
                    onClick = { selectedReadingTimeMode = SyncMode.PHONE_ONLY }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            TextButton(
                text = "取消",
                onClick = { viewModel.dismissSyncModeDialog() },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(20.dp))
            TextButton(
                text = "确定",
                colors = ButtonDefaults.textButtonColorsPrimary(),
                enabled = syncReadingDataState.selectedBooksForSync.isNotEmpty(),
                onClick = {
                    viewModel.setSyncModesAndStart(
                        selectedProgressMode,
                        selectedReadingTimeMode,
                        selectedBookmarkMode
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    }

    val showStorageHelpDialog = remember { mutableStateOf(false) }
    val showConnectionRequiredDialog = remember { mutableStateOf(false) }
    LaunchedEffect(connectionRequiredDialogState) {
        showConnectionRequiredDialog.value = connectionRequiredDialogState != null
    }

    SuperDialog(
        title = connectionRequiredDialogState?.title ?: "请先连接手环",
        summary = connectionRequiredDialogState?.message ?: "当前操作需要先连接手环后才能继续。",
        show = showConnectionRequiredDialog,
        onDismissRequest = { viewModel.dismissConnectionRequiredDialog() }
    ) {
        if (connectionRequiredDialogState?.showRetry == true) {
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    text = "取消",
                    onClick = { viewModel.dismissConnectionRequiredDialog() },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(20.dp))
                TextButton(
                    text = "重试",
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    onClick = { viewModel.retryConnectionOrStoragePreparation() },
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            TextButton(
                text = "知道了",
                onClick = { viewModel.dismissConnectionRequiredDialog() },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    SuperDialog(
        title = "预留空间说明",
        summary = "预留空间是为了系统更新而保留的存储空间。\n\n当前设备预留空间：${
            StorageUtils.getReservedStorageText(
                bandStorageInfo.product
            )
        }\n\n实际可用空间 = 总空间 - 预留空间 - 已用空间\n\n注意：预留空间大小是往大了标注的，实际可能没有这么多。",
        show = showStorageHelpDialog,
        onDismissRequest = { showStorageHelpDialog.value = false }
    ) {
        TextButton(
            text = "知道了",
            onClick = { showStorageHelpDialog.value = false },
            modifier = Modifier.fillMaxWidth()
        )
    }

    var categoryToRename by remember { mutableStateOf<String?>(null) }
    var newCategoryName by remember { mutableStateOf("") }
    val showRenameDialog = remember { mutableStateOf(false) }
    LaunchedEffect(categoryToRename) {
        showRenameDialog.value = categoryToRename != null
        if (categoryToRename != null) newCategoryName = categoryToRename!!
    }

    SuperDialog(
        title = "重命名分类",
        show = showRenameDialog,
        onDismissRequest = { categoryToRename = null }
    ) {
        TextField(
            value = newCategoryName,
            onValueChange = { newCategoryName = it },
            label = "分类名称",
            singleLine = true,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            TextButton(
                text = "取消",
                onClick = { categoryToRename = null },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(20.dp))
            TextButton(
                text = "确定",
                colors = ButtonDefaults.textButtonColorsPrimary(),
                onClick = {
                    if (newCategoryName.isNotBlank() && newCategoryName != categoryToRename) {
                        viewModel.renameCategory(categoryToRename!!, newCategoryName)
                        categoryToRename = null
                    }
                },
                enabled = newCategoryName.isNotBlank() && newCategoryName != categoryToRename,
                modifier = Modifier.weight(1f)
            )
        }
    }

    val showCreateBookDialog = remember { mutableStateOf(false) }
    var createBookName by remember { mutableStateOf("") }
    var createBookCategory by remember { mutableStateOf("") }

    SuperDialog(
        title = "创建书籍",
        show = showCreateBookDialog,
        onDismissRequest = {
            showCreateBookDialog.value = false
            createBookName = ""
            createBookCategory = ""
        }
    ) {
        TextField(
            value = createBookName,
            onValueChange = { createBookName = it },
            label = "书名",
            singleLine = true,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        TextField(
            value = createBookCategory,
            onValueChange = { createBookCategory = it },
            label = "分类（可选）",
            singleLine = true,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            TextButton(
                text = "取消",
                onClick = {
                    showCreateBookDialog.value = false
                    createBookName = ""
                    createBookCategory = ""
                },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(20.dp))
            TextButton(
                text = "创建",
                colors = ButtonDefaults.textButtonColorsPrimary(),
                onClick = {
                    val finalName = createBookName.trim()
                    if (finalName.isBlank()) {
                        Toast.makeText(context, "请输入书名", Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }
                    if (books.any { it.name == finalName }) {
                        Toast.makeText(context, "已存在同名书籍", Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }
                    viewModel.createBook(finalName, createBookCategory.trim().ifBlank { null })
                    showCreateBookDialog.value = false
                    createBookName = ""
                    createBookCategory = ""
                },
                modifier = Modifier.weight(1f)
            )
        }
    }

    val showMenuSheet = remember { mutableStateOf(false) }
    SuperBottomSheet(
        show = showMenuSheet,
        title = "更多选项",
        onDismissRequest = { showMenuSheet.value = false }
    ) {
        Card(
            modifier = Modifier.padding(bottom = 40.dp)
        ) {
            BasicComponent(
                title = "创建书籍",
                startAction = {
                    Icon(
                        MiuixIcons.Notes,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onSurface
                    )
                },
                onClick = {
                    showMenuSheet.value = false
                    showCreateBookDialog.value = true
                }
            )
            BasicComponent(
                title = "同步数据",
                startAction = {
                    Icon(
                        MiuixIcons.Update,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onSurface
                    )
                },
                onClick = {
                    showMenuSheet.value = false
                    viewModel.syncAllReadingData()
                },
                enabled = !syncReadingDataState.isSyncing
            )
            BasicComponent(
                title = "重新连接手环",
                startAction = {
                    Icon(
                        MiuixIcons.Refresh,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onSurface
                    )
                },
                onClick = {
                    showMenuSheet.value = false
                    viewModel.reconnect()
                }
            )
        }
    }

    val showSyncReadingDataSheet = remember { mutableStateOf(false) }
    LaunchedEffect(
        syncReadingDataState.isSyncing,
        syncReadingDataState.statusText,
        syncResultState
    ) {
        showSyncReadingDataSheet.value = syncReadingDataState.isSyncing ||
                syncReadingDataState.statusText.isNotEmpty() ||
                syncResultState != null
    }

    SyncReadingDataDialog(
        show = showSyncReadingDataSheet,
        state = syncReadingDataState,
        resultState = syncResultState,
        onDismiss = {
            if (syncReadingDataState.isSyncing) viewModel.cancelSyncReadingData()
            showSyncReadingDataSheet.value = false
            viewModel.clearSyncReadingDataState()
            viewModel.dismissSyncResult()
        }
    )

    val showVersionIncompatibleDialog = remember { mutableStateOf(false) }
    LaunchedEffect(versionIncompatibleState) {
        showVersionIncompatibleDialog.value = versionIncompatibleState != null
    }
    versionIncompatibleState?.let { state ->
        VersionIncompatibleDialog(
            show = showVersionIncompatibleDialog,
            currentVersion = state.currentVersion,
            requiredVersion = state.requiredVersion,
            onDismiss = { viewModel.dismissVersionIncompatible() },
            onOpenWebsite = {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://vs.lucky-e.top")
                )
                context.startActivity(intent)
            }
        )
    }

    LaunchedEffect(syncOptionsState) {
        if (syncOptionsState != null) {
            onNavigateToSyncOptions()
        }
    }

    val showImportSheet = remember { mutableStateOf(false) }
    LaunchedEffect(importState) { showImportSheet.value = importState != null }
    SuperBottomSheet(
        show = showImportSheet,
        onDismissRequest = { viewModel.cancelImport() }
    ) {
        importState?.let { state ->
            ImportBookBottomSheet(
                state = state,
                categories = viewModel.getCategories(),
                existingBookNames = books.map { it.name },
                onCancel = { viewModel.cancelImport() },
                onConfirm = { bookName, splitMethod, noSplit, wordsPerChapter, selectedCategory, enableChapterMerge, mergeMinWords, enableChapterRename, renamePattern, customRegex, selectedCustomRegexTemplateName, customRegexTemplates ->
                    viewModel.confirmImport(
                        bookName,
                        splitMethod,
                        noSplit,
                        wordsPerChapter,
                        selectedCategory,
                        enableChapterMerge,
                        mergeMinWords,
                        enableChapterRename,
                        renamePattern,
                        customRegex,
                        selectedCustomRegexTemplateName,
                        customRegexTemplates
                    )
                },
                onShowCategorySelector = { viewModel.showCategorySelector() }
            )
        }
    }

    val showCategoryDialog = remember { mutableStateOf(false) }
    LaunchedEffect(categoryState) {
        showCategoryDialog.value = categoryState != null
    }
    LaunchedEffect(showCategoryDialog.value) {
        if (!showCategoryDialog.value && categoryState != null) {
            viewModel.dismissCategorySelector()
        }
    }
    categoryState?.let { state ->
        CategoryDialog(
            show = showCategoryDialog,
            categories = state.categories,
            selectedCategory = state.selectedCategory,
            onCategorySelected = { category -> viewModel.selectCategory(category) },
            onCategoryCreated = { categoryName -> viewModel.createCategory(categoryName) },
            onCategoryDeleted = { categoryName -> viewModel.deleteCategory(categoryName) }
        )
    }

    val showImportProgressDialog = remember { mutableStateOf(false) }
    LaunchedEffect(importingState) {
        showImportProgressDialog.value = importingState != null
    }
    importingState?.let { state ->
        ImportProgressDialog(
            show = showImportProgressDialog,
            state = state,
            onDismiss = { viewModel.dismissImportProgress() }
        )
    }

    val showImportReportSheet = remember { mutableStateOf(false) }
    LaunchedEffect(importReportState) { showImportReportSheet.value = importReportState != null }
    importReportState?.let { state ->
        ImportReportBottomSheet(
            show = showImportReportSheet,
            state = state,
            onDismiss = { viewModel.dismissImportReport() }
        )
    }

    val showOverwriteConfirmDialog = remember { mutableStateOf(false) }
    LaunchedEffect(overwriteConfirmState) {
        showOverwriteConfirmDialog.value = overwriteConfirmState != null
    }
    overwriteConfirmState?.let { state ->
        OverwriteConfirmDialog(
            show = showOverwriteConfirmDialog,
            existingBook = state.existingBook,
            newBookName = state.newBookName,
            onCancel = {
                viewModel.cancelOverwriteConfirm()
            },
            onOverwrite = {
                viewModel.confirmOverwrite()
            }
        )
    }

    val showLargeChapterWarningDialog = remember { mutableStateOf(false) }
    LaunchedEffect(largeChapterWarningState) {
        showLargeChapterWarningDialog.value = largeChapterWarningState != null
    }
    largeChapterWarningState?.let { state ->
        SuperDialog(
            title = "存在过大章节",
            summary = state.message,
            show = showLargeChapterWarningDialog,
            onDismissRequest = { viewModel.dismissLargeChapterWarning() }
        ) {
            TextButton(
                text = "我知道了",
                colors = ButtonDefaults.textButtonColorsPrimary(),
                onClick = { viewModel.dismissLargeChapterWarning() },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    val showConnectionErrorSheet = remember { mutableStateOf(false) }
    LaunchedEffect(connectionErrorState) {
        showConnectionErrorSheet.value = connectionErrorState != null
    }
    SuperBottomSheet(
        show = showConnectionErrorSheet,
        onDismissRequest = { viewModel.dismissConnectionError() }
    ) {
        connectionErrorState?.let { state ->
            ConnectionErrorBottomSheet(
                deviceName = state.deviceName,
                isUnsupportedDevice = state.isUnsupportedDevice,
                onDismiss = { viewModel.dismissConnectionError() },
                onRetry = {
                    viewModel.dismissConnectionError()
                    scope.launch {
                        delay(300)
                        viewModel.reconnect()
                    }
                }
            )
        }
    }

    val showEditBookInfoSheet = remember { mutableStateOf(false) }
    LaunchedEffect(editBookInfoState) { showEditBookInfoSheet.value = editBookInfoState != null }
    SuperBottomSheet(
        show = showEditBookInfoSheet,
        onDismissRequest = { viewModel.dismissEditBookInfo() }
    ) {
        editBookInfoState?.let { state ->
            var editLocalCategory by remember(state.book) {
                mutableStateOf(
                    state.book.localCategory ?: ""
                )
            }

            LaunchedEffect(state.book.localCategory) {
                editLocalCategory = state.book.localCategory ?: ""
            }

            EditBookInfoBottomSheet(
                book = state.book,
                categories = viewModel.getCategories(),
                localCategory = editLocalCategory,
                onLocalCategoryChanged = { editLocalCategory = it },
                onCancel = { viewModel.dismissEditBookInfo() },
                onSave = { updatedBook -> viewModel.saveBookInfo(updatedBook) },
                onShowCategorySelector = {
                    viewModel.showCategorySelectorForEditBookInfo(
                        editLocalCategory.ifBlank { null }
                    ) { category ->
                        editLocalCategory = category ?: ""
                    }
                },
                onResyncInfo = {
                    val book = books.find { it.path == state.book.path }
                    if (book != null) viewModel.resyncBookInfo(book)
                },
                isResyncing = state.isResyncing,
                onSaveBeforeResync = { updatedBook ->
                    viewModel.saveBookInfoWithoutDismiss(
                        updatedBook
                    )
                }
            )
        }
    }

    val showSyncReadingDataConfirmDialog = remember { mutableStateOf(false) }
    LaunchedEffect(syncReadingDataState.showConfirmDialog) {
        showSyncReadingDataConfirmDialog.value = syncReadingDataState.showConfirmDialog
    }
    SyncReadingDataConfirmDialog(
        show = showSyncReadingDataConfirmDialog,
        onConfirm = { viewModel.confirmSyncReadingData() },
        onCancel = { viewModel.cancelSyncReadingDataConfirm() }
    )

    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())
    val actionAlpha =
        if (scrollBehavior.state.collapsedFraction > 0.5f) 1f else scrollBehavior.state.collapsedFraction * 2f

    val filteredBooks = remember(books, searchQuery) {
        if (searchQuery.isBlank()) books
        else books.filter {
            it.name.contains(
                searchQuery,
                ignoreCase = true
            ) || it.localCategory?.contains(searchQuery, ignoreCase = true) == true
        }
    }

    val booksByCategory = remember(filteredBooks) {
        filteredBooks.groupBy { it.localCategory ?: "未分类" }
            .toList()
            .sortedBy { if (it.first == "未分类") "\uFFFF" else it.first }
    }

    val listItems = remember(
        booksByCategory,
        expandedCategories,
        recentBook,
        recentUpdatedBook,
        searchQuery,
        showRecentImport,
        showRecentUpdate
    ) {
        buildList {
            if (showRecentImport && recentBook != null && searchQuery.isBlank()) {
                add(ListItem(ItemType.RECENT_HEADER))
                add(ListItem(ItemType.RECENT_BOOK, book = recentBook))
            }
            if (showRecentUpdate && recentUpdatedBook != null && searchQuery.isBlank() && recentUpdatedBook != recentBook) {
                add(ListItem(ItemType.RECENT_UPDATE_HEADER))
                add(ListItem(ItemType.RECENT_UPDATE_BOOK, book = recentUpdatedBook))
            }
            booksByCategory.forEach { (category, categoryBooks) ->
                add(ListItem(ItemType.CATEGORY_HEADER, category = category))
                categoryBooks.forEach { book ->
                    add(ListItem(ItemType.BOOK, book = book, category = category))
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            popupHost = {},
            topBar = {
                TopAppBar(
                    title = if (isMultiSelectMode) "已选择 ${selectedBooks.size} 本" else "弦电子书",
                    largeTitle = if (isMultiSelectMode) "已选择 ${selectedBooks.size} 本" else "弦电子书",
                    actions = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.alpha(actionAlpha)
                        ) {
                            if (isMultiSelectMode) {
                                if (selectedBooks.isNotEmpty()) {
                                    TextButton(
                                        text = "删除(${selectedBooks.size})",
                                        colors = ButtonDefaults.textButtonColorsPrimary(),
                                        onClick = { viewModel.requestDeleteSelectedBooks() }
                                    )
                                }
                                TextButton(
                                    text = "取消",
                                    onClick = { viewModel.exitMultiSelectMode() }
                                )
                            } else {
                                if (bandTransferEnabled) {
                                    IconButton(
                                        onClick = { showMenuSheet.value = true },
                                        modifier = Modifier.padding(end = 6.dp)
                                    ) {
                                        Icon(MiuixIcons.More, contentDescription = "更多选项")
                                    }
                                }
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            }
        ) { paddingValues ->
            LazyColumn(
                state = scrollState,
                modifier = Modifier
                    .fillMaxSize()
                    .overScrollVertical()
                    .scrollEndHaptic()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .padding(paddingValues),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 8.dp,
                    end = 16.dp,
                    bottom = 120.dp
                ),
            ) {
                val updatesToShow = if (updateCheckState.updateInfoList.isNotEmpty()) updateCheckState.updateInfoList else listOfNotNull(updateCheckState.updateInfo)
                val hasUpdate = updatesToShow.any { it.hasUpdate }

                if (hasUpdate) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            onClick = { viewModel.showUpdateDialog() },
                            colors = CardDefaults.defaultColors(
                                color = MiuixTheme.colorScheme.errorContainer
                            ),
                            showIndication = true,
                            pressFeedbackType = PressFeedbackType.Tilt
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.Update,
                                    contentDescription = null,
                                    tint = MiuixTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                Column {
                                    Text(
                                        text = "发现新版本可用",
                                        color = MiuixTheme.colorScheme.onErrorContainer,
                                        style = MiuixTheme.textStyles.body1,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "点击查看更新日志并下载",
                                        color = MiuixTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                        style = MiuixTheme.textStyles.body2
                                    )
                                }
                            }
                        }
                    }
                }

                if (bandTransferEnabled) {
                    item {
                        if (connectionState.isConnected) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Max)
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    onClick = {
                                        if (!isMultiSelectMode) {
                                            showMenuSheet.value = true
                                        }
                                    },
                                    showIndication = true,
                                    pressFeedbackType = PressFeedbackType.Tilt
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .offset(38.dp, 45.dp),
                                            contentAlignment = Alignment.BottomEnd
                                        ) {
                                            Icon(
                                                modifier = Modifier.size(170.dp),
                                                imageVector = MiuixIcons.Ok,
                                                tint = MiuixTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                contentDescription = null
                                            )
                                        }
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(16.dp)
                                        ) {
                                            Text(
                                                modifier = Modifier.fillMaxWidth(),
                                                text = connectionState.statusText,
                                                style = MiuixTheme.textStyles.title3,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                modifier = Modifier.fillMaxWidth(),
                                                text = connectionState.descriptionText,
                                                style = MiuixTheme.textStyles.body2,
                                                fontWeight = FontWeight.Medium,
                                                color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }

                                if (bandStorageInfo.hasValidData && !bandStorageInfo.isLoading) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f),
                                            insideMargin = PaddingValues(16.dp),
                                            onClick = { showStorageHelpDialog.value = true },
                                            showIndication = true,
                                            pressFeedbackType = PressFeedbackType.Tilt,
                                            colors = CardDefaults.defaultColors(
                                                color = if (bandStorageInfo.showWarning) MiuixTheme.colorScheme.errorContainer else MiuixTheme.colorScheme.surfaceContainer
                                            )
                                        ) {
                                            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                                                Text(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    text = "实际可用",
                                                    fontWeight = FontWeight.Medium,
                                                    style = MiuixTheme.textStyles.body2,
                                                    color = if (bandStorageInfo.showWarning) MiuixTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f) else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                                )
                                                Text(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    text = bytesToReadable(bandStorageInfo.actualAvailable),
                                                    style = MiuixTheme.textStyles.title2,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (bandStorageInfo.showWarning) MiuixTheme.colorScheme.onErrorContainer else MiuixTheme.colorScheme.onSurface,
                                                )
                                            }
                                        }

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f),
                                            insideMargin = PaddingValues(16.dp),
                                            onClick = { showStorageHelpDialog.value = true },
                                            showIndication = true,
                                            pressFeedbackType = PressFeedbackType.Tilt,
                                            colors = CardDefaults.defaultColors(
                                                color = if (bandStorageInfo.showWarning) MiuixTheme.colorScheme.errorContainer else MiuixTheme.colorScheme.surfaceContainer
                                            )
                                        ) {
                                            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                                                Text(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    text = "已用空间",
                                                    fontWeight = FontWeight.Medium,
                                                    style = MiuixTheme.textStyles.body2,
                                                    color = if (bandStorageInfo.showWarning) MiuixTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f) else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                                )
                                                val usableSpace = bandStorageInfo.totalStorage - bandStorageInfo.reservedStorage
                                                val progress = if (usableSpace > 0) {
                                                    (bandStorageInfo.usedStorage.toFloat() / usableSpace.toFloat()).coerceIn(0f, 1f)
                                                } else 0f
                                                Text(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    text = "${(progress * 100).toInt()}%",
                                                    style = MiuixTheme.textStyles.title2,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (bandStorageInfo.showWarning) MiuixTheme.colorScheme.onErrorContainer else MiuixTheme.colorScheme.onSurface,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                onClick = {
                                    viewModel.reconnect()
                                },
                                showIndication = true,
                                pressFeedbackType = PressFeedbackType.Sink,
                                colors = CardDefaults.defaultColors(
                                    color = MiuixTheme.colorScheme.errorContainer
                                )
                            ) {
                                BasicComponent(
                                    title = connectionState.statusText,
                                    titleColor = BasicComponentDefaults.titleColor(color = MiuixTheme.colorScheme.onErrorContainer),
                                    summary = connectionState.descriptionText,
                                    summaryColor = BasicComponentDefaults.summaryColor(color = MiuixTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)),
                                    startAction = {
                                        Icon(
                                            imageVector = MiuixIcons.Info,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 16.dp),
                                            tint = MiuixTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                if (!hasClickedTransferButton && books.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.defaultColors(
                                color = MiuixTheme.colorScheme.primaryContainer
                            ),
                            showIndication = true,
                            pressFeedbackType = PressFeedbackType.Sink
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.Update,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MiuixTheme.colorScheme.onPrimaryContainer
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "开始传输书籍",
                                        style = MiuixTheme.textStyles.title3,
                                        fontWeight = FontWeight.Bold,
                                        color = MiuixTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "点击书籍卡片，在弹出的对话框中点击“传输书籍”按钮将书籍传输到小米手环。",
                                        style = MiuixTheme.textStyles.body2,
                                        color = MiuixTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }

                if (books.isNotEmpty() && showSearchBar) {
                    item {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            label = "搜索书籍...",
                            leadingIcon = {
                                Icon(
                                    MiuixIcons.Search,
                                    contentDescription = "搜索",
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                            },
                            trailingIcon = if (searchQuery.isNotEmpty()) {
                                {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.padding(end = 12.dp)
                                    ) {
                                        Icon(MiuixIcons.Close, contentDescription = "清除")
                                    }
                                }
                            } else null,
                            singleLine = true
                        )
                    }
                }

                if (filteredBooks.isEmpty() && books.isNotEmpty()) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp)
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Search,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MiuixTheme.colorScheme.onSurfaceVariantActions
                            )
                            Text(
                                text = "未找到匹配的书籍",
                                style = MiuixTheme.textStyles.title2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                            Text(
                                text = "请尝试其他关键词",
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else if (books.isEmpty()) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp)
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Notes,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MiuixTheme.colorScheme.onSurfaceVariantActions
                            )
                            Text(
                                text = "还没有书籍",
                                style = MiuixTheme.textStyles.title2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                            Text(
                                text = "点击右下角的 ➕ 按钮导入书籍",
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else if (filteredBooks.isNotEmpty()) {
                    itemsIndexed(
                        items = listItems,
                        key = { _, item ->
                            when (item.type) {
                                ItemType.RECENT_HEADER -> "header_recent"
                                ItemType.RECENT_BOOK -> "recent_${item.book!!.id}"
                                ItemType.RECENT_UPDATE_HEADER -> "header_recent_update"
                                ItemType.RECENT_UPDATE_BOOK -> "recent_update_${item.book!!.id}"
                                ItemType.CATEGORY_HEADER -> "category_${item.category}"
                                ItemType.BOOK -> "${item.category}_${item.book!!.id}"
                            }
                        }
                    ) { index, item ->
                        val isLastItem = index == listItems.size - 1
                        val prevItem = if (index > 0) listItems[index - 1] else null
                        val prevItemVisible = when (prevItem?.type) {
                            ItemType.BOOK -> prevItem.category != null && expandedCategories.contains(
                                prevItem.category
                            )

                            null -> false
                            else -> true
                        }
                        val shouldAddTopSpacing = when {
                            index == 0 -> false
                            prevItem?.type == ItemType.CATEGORY_HEADER -> false
                            prevItem?.type == ItemType.RECENT_HEADER -> false
                            prevItem?.type == ItemType.RECENT_UPDATE_HEADER -> false
                            prevItem?.type == ItemType.BOOK && !prevItemVisible -> false
                            else -> true
                        }

                        when (item.type) {
                            ItemType.RECENT_HEADER -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = MiuixIcons.Recent,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MiuixTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Text(
                                        text = "最近导入",
                                        style = MiuixTheme.textStyles.title4,
                                        color = MiuixTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            ItemType.RECENT_UPDATE_HEADER -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = MiuixIcons.Update,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MiuixTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Text(
                                        text = "最近更新",
                                        style = MiuixTheme.textStyles.title4,
                                        color = MiuixTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            ItemType.RECENT_BOOK, ItemType.RECENT_UPDATE_BOOK -> {
                                val book = item.book!!
                                AnimatedVisibility(
                                    visible = true,
                                    enter = scaleIn() + expandVertically(),
                                    exit = scaleOut()
                                ) {
                                    Box(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)) {
                                        BookItem(
                                            book = book,
                                            onDeleteClick = { viewModel.requestDeleteBook(book) },
                                            onSyncClick = { viewModel.startPush(book) },
                                            onChapterListClick = { viewModel.showChapterList(book) },
                                            onContinueReadingClick = {
                                                viewModel.continueReading(
                                                    book
                                                )
                                            },
                                            onImportCoverClick = {
                                                viewModel.requestImportCover(book)
                                                onImportCoverClick()
                                            },
                                            onEditInfoClick = { viewModel.showEditBookInfo(book) },
                                            onExportClick = {
                                                val exportFormat = book.format.lowercase()
                                                if (exportFormat in setOf("txt", "epub", "nvb")) {
                                                    pendingExportBook = book
                                                    exportBookLauncher.launch("${book.name}.$exportFormat")
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "仅支持导出 TXT、EPUB、NVB",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            },
                                            isSyncEnabled = true,
                                            lastChapterName = lastChapterNames[book.path],
                                            showSyncButton = bandTransferEnabled
                                        )
                                    }
                                }
                            }

                            ItemType.BOOK -> {
                                val book = item.book!!
                                val category = item.category!!
                                val isCategoryExpanded = expandedCategories.contains(category)

                                var swipeOffsetX by remember(book.path) { mutableStateOf(0f) }
                                val animatedSwipeOffsetX by animateFloatAsState(
                                    targetValue = swipeOffsetX,
                                    animationSpec = tween(durationMillis = 180),
                                    label = "bookSwipeOffset"
                                )
                                val maxSwipeDistance = -450f
                                val swipeTriggerDistance = -80f

                                AnimatedVisibility(
                                    visible = isCategoryExpanded,
                                    enter = fadeIn(animationSpec = tween(300)) + expandVertically(
                                        expandFrom = Alignment.Top,
                                        animationSpec = tween(300)
                                    ),
                                    exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(
                                        shrinkTowards = Alignment.Top,
                                        animationSpec = tween(300)
                                    )
                                ) {
                                    var boxModifier = Modifier
                                        .padding(
                                            top = if (shouldAddTopSpacing) 8.dp else 0.dp,
                                            bottom = if (isLastItem) 16.dp else 0.dp
                                        )
                                        .pointerInput(book.path) {
                                            detectTapGestures(
                                                onLongPress = {
                                                    if (quickEditCategoryEnabled) {
                                                        viewModel.showCategorySelector(book)
                                                    } else {
                                                        viewModel.enterMultiSelectMode()
                                                        viewModel.selectBook(book.path)
                                                    }
                                                }
                                            )
                                        }
                                    if (quickEditCategoryEnabled) {
                                        boxModifier = boxModifier.pointerInput(book.path) {
                                            detectDragGestures(
                                                onDragEnd = {
                                                    if (swipeOffsetX <= swipeTriggerDistance) {
                                                        swipeOffsetX = 0f
                                                        viewModel.showCategorySelector(book)
                                                    } else {
                                                        swipeOffsetX = 0f
                                                    }
                                                },
                                                onDragCancel = {
                                                    swipeOffsetX = 0f
                                                }
                                            ) { change, dragAmount ->
                                                val (dragX, _) = dragAmount
                                                val newOffset = (swipeOffsetX + dragX).coerceIn(
                                                    maxSwipeDistance,
                                                    0f
                                                )
                                                swipeOffsetX = newOffset
                                                change.consume()
                                            }
                                        }
                                    }

                                    Box(
                                        modifier = boxModifier
                                    ) {
                                        if (quickEditCategoryEnabled) {
                                            Row(
                                                modifier = Modifier
                                                    .matchParentSize()
                                                    .padding(horizontal = 8.dp),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Card(
                                                    colors = CardDefaults.defaultColors(
                                                        color = MiuixTheme.colorScheme.primary
                                                    ),
                                                    insideMargin = PaddingValues(
                                                        horizontal = 12.dp,
                                                        vertical = 8.dp
                                                    ),
                                                    pressFeedbackType = PressFeedbackType.Sink,
                                                    showIndication = false
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(
                                                            6.dp
                                                        )
                                                    ) {
                                                        Icon(
                                                            imageVector = MiuixIcons.Edit,
                                                            contentDescription = null,
                                                            tint = MiuixTheme.colorScheme.onPrimary
                                                        )
                                                        Text(
                                                            text = "切换分类",
                                                            style = MiuixTheme.textStyles.body2,
                                                            color = MiuixTheme.colorScheme.onPrimary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        BookItem(
                                            book = book,
                                            modifier = Modifier.offset {
                                                IntOffset(
                                                    animatedSwipeOffsetX.roundToInt(),
                                                    0
                                                )
                                            },
                                            isSelected = selectedBooks.contains(book.path),
                                            onDeleteClick = { viewModel.requestDeleteBook(book) },
                                            onSyncClick = { viewModel.startPush(book) },
                                            onChapterListClick = { viewModel.showChapterList(book) },
                                            onContinueReadingClick = {
                                                viewModel.continueReading(
                                                    book
                                                )
                                            },
                                            onImportCoverClick = {
                                                viewModel.requestImportCover(book)
                                                onImportCoverClick()
                                            },
                                            onEditInfoClick = { viewModel.showEditBookInfo(book) },
                                            onExportClick = {
                                                val exportFormat = book.format.lowercase()
                                                if (exportFormat in setOf("txt", "epub", "nvb")) {
                                                    pendingExportBook = book
                                                    exportBookLauncher.launch("${book.name}.$exportFormat")
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "仅支持导出 TXT、EPUB、NVB",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            },
                                            isSyncEnabled = connectionState.isConnected,
                                            lastChapterName = lastChapterNames[book.path],
                                            showSyncButton = bandTransferEnabled
                                        )
                                    }
                                }
                            }

                            ItemType.CATEGORY_HEADER -> {
                                val category = item.category!!
                                val categoryBooks =
                                    booksByCategory.first { it.first == category }.second
                                val isExpanded = expandedCategories.contains(category)

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            top = 8.dp,
                                            bottom = if (isExpanded) 8.dp else 0.dp
                                        ),
                                    insideMargin = PaddingValues(
                                        horizontal = 16.dp,
                                        vertical = 12.dp
                                    ),
                                    colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
                                    pressFeedbackType = PressFeedbackType.Sink,
                                    showIndication = true,
                                    onClick = {
                                        viewModel.toggleCategoryExpansion(
                                            category
                                        )
                                    },
                                    onLongPress = {
                                        if (quickRenameCategoryEnabled) categoryToRename = category
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = category,
                                                style = MiuixTheme.textStyles.title4,
                                                color = MiuixTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "(${categoryBooks.size})",
                                                style = MiuixTheme.textStyles.body2,
                                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

