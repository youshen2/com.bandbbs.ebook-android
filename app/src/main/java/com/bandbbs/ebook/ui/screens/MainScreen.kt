package com.bandbbs.ebook.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bandbbs.ebook.ui.components.BookItem
import com.bandbbs.ebook.ui.components.CategoryBottomSheet
import com.bandbbs.ebook.ui.components.ConnectionErrorBottomSheet
import com.bandbbs.ebook.ui.components.EditBookInfoBottomSheet
import com.bandbbs.ebook.ui.components.FirstSyncConfirmDialog
import com.bandbbs.ebook.ui.components.SyncReadingDataConfirmDialog
import com.bandbbs.ebook.ui.components.ImportBookBottomSheet
import com.bandbbs.ebook.ui.components.ImportProgressBottomSheet
import com.bandbbs.ebook.ui.components.ImportReportBottomSheet
import com.bandbbs.ebook.ui.components.OverwriteConfirmBottomSheet
import com.bandbbs.ebook.ui.components.PushBottomSheet
import com.bandbbs.ebook.ui.components.SyncOptionsBottomSheet
import com.bandbbs.ebook.ui.components.SyncReadingDataBottomSheet
import com.bandbbs.ebook.ui.components.VersionIncompatibleBottomSheet
import com.bandbbs.ebook.ui.viewmodel.MainViewModel
import com.bandbbs.ebook.ui.viewmodel.SyncMode
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.bandbbs.ebook.database.AppDatabase
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember


private enum class ItemType {
    RECENT_HEADER,
    RECENT_BOOK,
    RECENT_UPDATE_HEADER,
    RECENT_UPDATE_BOOK,
    CATEGORY_HEADER,
    BOOK
}


private data class ListItem(
    val type: ItemType,
    val category: String? = null,
    val book: com.bandbbs.ebook.ui.model.Book? = null
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onImportCoverClick: () -> Unit
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
    val pushState by viewModel.pushState.collectAsState()
    val importState by viewModel.importState.collectAsState()
    val importingState by viewModel.importingState.collectAsState()
    val importReportState by viewModel.importReportState.collectAsState()

    val bookToDelete by viewModel.bookToDelete.collectAsState()
    val booksToDelete by viewModel.booksToDelete.collectAsState()
    val syncOptionsState by viewModel.syncOptionsState.collectAsState()
    val overwriteConfirmState by viewModel.overwriteConfirmState.collectAsState()
    val bookForCoverImport by viewModel.bookForCoverImport.collectAsState()
    val connectionErrorState by viewModel.connectionErrorState.collectAsState()
    val categoryState by viewModel.categoryState.collectAsState()
    val firstSyncConfirmState by viewModel.firstSyncConfirmState.collectAsState()
    val editBookInfoState by viewModel.editBookInfoState.collectAsState()
    val syncReadingDataState by viewModel.syncReadingDataState.collectAsState()
    val versionIncompatibleState by viewModel.versionIncompatibleState.collectAsState()

    val expandedBookPath by viewModel.expandedBookPath.collectAsState()
    val expandedCategories by viewModel.expandedCategories.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lastChapterNames = remember { mutableStateMapOf<String, String>() }
    
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
                lastChapterName?.let {
                    lastChapterNames[book.path] = it
                }
            }
        }
    }
    
    val categorySheetState = rememberModalBottomSheetState()

    val pushSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val importSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val importProgressSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val importReportSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val syncOptionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val overwriteConfirmSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val connectionErrorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editBookInfoSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val versionIncompatibleSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val syncReadingDataSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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

    if (booksToDelete.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDeleteSelectedBooks() },
            title = { Text("批量删除书籍") },
            text = { 
                Column {
                    Text("确定要删除以下 ${booksToDelete.size} 本书籍吗？此操作将同时删除文件和所有章节数据，且不可恢复。")
                    Spacer(modifier = Modifier.height(8.dp))
                    booksToDelete.take(5).forEach { book ->
                        Text("· ${book.name}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (booksToDelete.size > 5) {
                        Text("... 还有 ${booksToDelete.size - 5} 本", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDeleteSelectedBooks() },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDeleteSelectedBooks() }) {
                    Text("取消")
                }
            }
        )
    }


    if (syncReadingDataState.showConfirmDialog) {
        SyncReadingDataConfirmDialog(
            onConfirm = { viewModel.confirmSyncReadingData() },
            onCancel = { viewModel.cancelSyncReadingDataConfirm() }
        )
    }

    if (syncReadingDataState.showModeDialog) {
        var selectedProgressMode by remember { mutableStateOf(syncReadingDataState.progressSyncMode) }
        var selectedReadingTimeMode by remember { mutableStateOf(syncReadingDataState.readingTimeSyncMode) }

        AlertDialog(
            onDismissRequest = {
                viewModel.dismissSyncModeDialog()
            },
            title = { Text("选择同步方式") },
            text = {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(scrollState)
                ) {
                    Text("请选择阅读数据的同步方式：")
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "阅读进度",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedProgressMode = SyncMode.AUTO }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedProgressMode == SyncMode.AUTO,
                            onClick = { selectedProgressMode = SyncMode.AUTO }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "自动处理（默认）",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "每本书的阅读进度以两端中最新的一个为准进行同步",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedProgressMode = SyncMode.BAND_ONLY }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedProgressMode == SyncMode.BAND_ONLY,
                            onClick = { selectedProgressMode = SyncMode.BAND_ONLY }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "手环端",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "将手环端单向同步到手机端",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedProgressMode = SyncMode.PHONE_ONLY }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedProgressMode == SyncMode.PHONE_ONLY,
                            onClick = { selectedProgressMode = SyncMode.PHONE_ONLY }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "手机端",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "将手机端单向同步到手环端",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "阅读时长",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReadingTimeMode = SyncMode.AUTO }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReadingTimeMode == SyncMode.AUTO,
                            onClick = { selectedReadingTimeMode = SyncMode.AUTO }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "自动处理（默认）",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "按照阅读时长最长的一端为准进行同步",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReadingTimeMode = SyncMode.BAND_ONLY }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReadingTimeMode == SyncMode.BAND_ONLY,
                            onClick = { selectedReadingTimeMode = SyncMode.BAND_ONLY }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "手环端",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "将手环端单向同步到手机端",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReadingTimeMode = SyncMode.PHONE_ONLY }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReadingTimeMode == SyncMode.PHONE_ONLY,
                            onClick = { selectedReadingTimeMode = SyncMode.PHONE_ONLY }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "手机端",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "将手机端单向同步到手环端",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setSyncModesAndStart(
                            selectedProgressMode,
                            selectedReadingTimeMode
                        )
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissSyncModeDialog()
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }


    if (syncReadingDataState.isSyncing || (syncReadingDataState.statusText.isNotEmpty() && !syncReadingDataState.isSyncing)) {
        LaunchedEffect(syncReadingDataState.isSyncing, syncReadingDataState.statusText) {
            if (syncReadingDataState.isSyncing || syncReadingDataState.statusText.isNotEmpty()) {
                syncReadingDataSheetState.show()
            }
        }
        ModalBottomSheet(
            onDismissRequest = {
                if (syncReadingDataState.isSyncing) {

                    viewModel.cancelSyncReadingData()
                }
                scope.launch {
                    syncReadingDataSheetState.hide()
                    viewModel.clearSyncReadingDataState()
                }
            },
            sheetState = syncReadingDataSheetState
        ) {
            SyncReadingDataBottomSheet(
                state = syncReadingDataState,
                onDismiss = {
                    if (syncReadingDataState.isSyncing) {

                        viewModel.cancelSyncReadingData()
                    }
                    scope.launch {
                        syncReadingDataSheetState.hide()
                        viewModel.clearSyncReadingDataState()
                    }
                }
            )
        }
    }

    versionIncompatibleState?.let { state ->
        LaunchedEffect(state) {
            versionIncompatibleSheetState.show()
        }
        ModalBottomSheet(
            onDismissRequest = {
                scope.launch {
                    versionIncompatibleSheetState.hide()
                    viewModel.dismissVersionIncompatible()
                }
            },
            sheetState = versionIncompatibleSheetState
        ) {
            VersionIncompatibleBottomSheet(
                currentVersion = state.currentVersion,
                requiredVersion = state.requiredVersion,
                onDismiss = {
                    scope.launch {
                        versionIncompatibleSheetState.hide()
                        viewModel.dismissVersionIncompatible()
                    }
                }
            )
        }
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
                },
                onDeleteChapters = { chapterIndices ->
                    viewModel.deleteBandChapters(it.book, chapterIndices)
                }
            )
        }
    }

    firstSyncConfirmState?.let { book ->
        FirstSyncConfirmDialog(
            onConfirm = {
                viewModel.confirmFirstSync()
            },
            onCancel = {
                viewModel.cancelFirstSyncConfirm()
            }
        )
    }

    importState?.let {
        ModalBottomSheet(
            onDismissRequest = { viewModel.cancelImport() },
            sheetState = importSheetState
        ) {
            ImportBookBottomSheet(
                state = it,
                categories = viewModel.getCategories(),
                existingBookNames = books.map { book -> book.name },
                onCancel = {
                    scope.launch {
                        importSheetState.hide()
                        viewModel.cancelImport()
                    }
                },
                onConfirm = { bookName, splitMethod, noSplit, wordsPerChapter, selectedCategory, enableChapterMerge, mergeMinWords, enableChapterRename, renamePattern, customRegex ->
                    scope.launch {
                        importSheetState.hide()
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
                            customRegex
                        )
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
                        kotlinx.coroutines.delay(300)
                        viewModel.reconnect()
                    }
                }
            )
        }
    }

    editBookInfoState?.let { state ->
        var editLocalCategory by remember(state.book) {
            mutableStateOf(
                state.book.localCategory ?: ""
            )
        }


        androidx.compose.runtime.LaunchedEffect(state.book.localCategory) {
            editLocalCategory = state.book.localCategory ?: ""
        }

        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissEditBookInfo() },
            sheetState = editBookInfoSheetState
        ) {
            EditBookInfoBottomSheet(
                book = state.book,
                categories = viewModel.getCategories(),
                localCategory = editLocalCategory,
                onLocalCategoryChanged = { editLocalCategory = it },
                onCancel = {
                    scope.launch {
                        editBookInfoSheetState.hide()
                        viewModel.dismissEditBookInfo()
                    }
                },
                onSave = { updatedBook ->
                    scope.launch {
                        editBookInfoSheetState.hide()
                        viewModel.saveBookInfo(updatedBook)
                    }
                },
                onShowCategorySelector = {
                    viewModel.showCategorySelectorForEditBookInfo(
                        state.book,
                        onCategorySelected = { category ->
                            editLocalCategory = category ?: ""
                        }
                    )
                },
                onResyncInfo = {
                    val book = books.find { it.path == state.book.path }
                    if (book != null) {
                        viewModel.resyncBookInfo(book)
                    }
                },
                isResyncing = state.isResyncing,
                onSaveBeforeResync = { updatedBook ->
                    scope.launch {
                        viewModel.saveBookInfoWithoutDismiss(updatedBook)
                    }
                }
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                var showMenu by remember { mutableStateOf(false) }
                TopAppBar(
                    title = {
                        if (isMultiSelectMode) {
                            Text(
                                text = "已选择 ${selectedBooks.size} 本",
                                style = MaterialTheme.typography.titleLarge,
                            )
                        } else {
                            Column {
                                Text(
                                    text = "弦电子书",
                                    style = MaterialTheme.typography.titleLarge,
                                )
                                Text(
                                    text = connectionState.statusText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        if (isMultiSelectMode) {
                            if (selectedBooks.isNotEmpty()) {
                                TextButton(
                                    onClick = { viewModel.requestDeleteSelectedBooks() },
                                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("删除(${selectedBooks.size})")
                                }
                            }
                            TextButton(onClick = { viewModel.exitMultiSelectMode() }) {
                                Text("取消")
                            }
                        } else {
                            val syncReadingDataState by viewModel.syncReadingDataState.collectAsState()
                            val isEnabled =
                                connectionState.isConnected && !syncReadingDataState.isSyncing
                            FilledTonalButton(
                                onClick = { viewModel.syncAllReadingData() },
                                enabled = isEnabled,
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Sync,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("同步数据")
                            }
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
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.surface
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (!hasClickedTransferButton && books.isNotEmpty()) {
                    var showTipCard by remember { mutableStateOf(true) }
                    AnimatedVisibility(
                        visible = showTipCard,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 0.dp
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Sync,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "开始传输书籍",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "点击书籍卡片展开，然后点击传输书籍按钮进行传输",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }

                if (books.isNotEmpty() && showSearchBar) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("搜索书籍...") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Search, contentDescription = "搜索")
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Outlined.Close, contentDescription = "清除")
                                }
                            }
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                val filteredBooks = remember(books, searchQuery) {
                    if (searchQuery.isBlank()) {
                        books
                    } else {
                        books.filter { book ->
                            book.name.contains(searchQuery, ignoreCase = true) ||
                                    book.localCategory?.contains(
                                        searchQuery,
                                        ignoreCase = true
                                    ) == true
                        }
                    }
                }

                if (filteredBooks.isEmpty() && books.isNotEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Search,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "未找到匹配的书籍",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "请尝试其他关键词",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else if (books.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                Icons.Outlined.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "还没有书籍",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "点击右下角的 ➕ 按钮导入书籍",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else if (filteredBooks.isNotEmpty()) {

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

                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            top = 8.dp,
                            end = 16.dp,
                            bottom = 80.dp
                        )
                    ) {
                        itemsIndexed(
                            items = listItems,
                            key = { _, item ->
                                when (item.type) {
                                    ItemType.RECENT_HEADER -> "header_recent"
                                    ItemType.RECENT_BOOK -> "recent_${item.book!!.id}"
                                    ItemType.RECENT_UPDATE_HEADER -> "header_recent_update"
                                    ItemType.RECENT_UPDATE_BOOK -> "recent_update_${item.book!!.id}"
                                    ItemType.CATEGORY_HEADER -> "category_${item.category}"
                                    ItemType.BOOK -> "${item.category}_${item.book!!.path}"
                                }
                            }
                        ) { index, item ->
                            val isLastItem = index == listItems.size - 1
                            val prevItem = if (index > 0) listItems[index - 1] else null

                            val prevItemVisible = when (prevItem?.type) {
                                ItemType.BOOK -> {
                                    val prevCategory = prevItem.category
                                    prevCategory != null && expandedCategories.contains(prevCategory)
                                }

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
                                            Icons.Default.History,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.size(8.dp))
                                        Text(
                                            text = "最近导入",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.size(8.dp))
                                    }
                                }

                                ItemType.RECENT_UPDATE_HEADER -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Update,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.size(8.dp))
                                        Text(
                                            text = "最近更新",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary,
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
                                        Box(
                                            modifier = Modifier.padding(
                                                top = 8.dp,
                                                bottom = 8.dp
                                            )
                                        ) {
                                            val dismissState = rememberDismissState(confirmStateChange = { newState ->
                                                if (newState == DismissValue.DismissedToStart) {
                                                    if (quickEditCategoryEnabled) {
                                                        viewModel.showCategorySelector(book)
                                                    } else {
                                                        viewModel.enterMultiSelectMode()
                                                        viewModel.selectBook(book.path)
                                                    }
                                                    false
                                                } else {
                                                    true
                                                }
                                            })

                                            SwipeToDismiss(
                                                state = dismissState,
                                                directions = setOf(DismissDirection.EndToStart),
                                                background = {
                                                    val direction = dismissState.dismissDirection ?: return@SwipeToDismiss
                                                    val color = when (direction) {
                                                        DismissDirection.EndToStart -> MaterialTheme.colorScheme.primaryContainer
                                                        else -> MaterialTheme.colorScheme.surface
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(16.dp),
                                                        contentAlignment = Alignment.CenterEnd
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            Icon(
                                                                if (quickEditCategoryEnabled) Icons.Outlined.Edit else Icons.Outlined.CheckCircle,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                            )
                                                            Text(
                                                                if (quickEditCategoryEnabled) "修改分类" else "多选",
                                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                                style = MaterialTheme.typography.bodyLarge
                                                            )
                                                        }
                                                    }
                                                }
                                            ) {
                                                BookItem(
                                                    book = book,
                                                    isExpanded = expandedBookPath == book.path,
                                                    onExpandClick = {
                                                        if (isMultiSelectMode) {
                                                            viewModel.selectBook(book.path)
                                                        } else {
                                                            viewModel.setExpandedBook(if (expandedBookPath == book.path) null else book.path)
                                                        }
                                                    },
                                                    onDeleteClick = { viewModel.requestDeleteBook(book) },
                                                    onSyncClick = { viewModel.startPush(book) },
                                                    onChapterListClick = {
                                                        viewModel.showChapterList(
                                                            book
                                                        )
                                                    },
                                                    onContinueReadingClick = {
                                                        viewModel.continueReading(
                                                            book
                                                        )
                                                    },
                                                    onImportCoverClick = {
                                                        viewModel.requestImportCover(book)
                                                        onImportCoverClick()
                                                    },
                                                    onEditInfoClick = {
                                                        viewModel.showEditBookInfo(book)
                                                    },
                                                    isSyncEnabled = connectionState.isConnected,
                                                    lastChapterName = lastChapterNames[book.path]
                                                )
                                            }
                                        }
                                    }
                                }

                                ItemType.BOOK -> {
                                    val book = item.book!!
                                    val category = item.category!!
                                    val isCategoryExpanded = expandedCategories.contains(category)
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
                                        Box(
                                            modifier = Modifier
                                                .padding(
                                                    top = if (shouldAddTopSpacing) 8.dp else 0.dp,
                                                    bottom = if (isLastItem) 16.dp else 0.dp
                                                )
                                        ) {
                                            val dismissState = rememberDismissState(confirmStateChange = { newState ->
                                                if (newState == DismissValue.DismissedToEnd) {
                                                    if (quickEditCategoryEnabled) {
                                                        viewModel.showCategorySelector(book)
                                                    } else {
                                                        viewModel.enterMultiSelectMode()
                                                        viewModel.selectBook(book.path)
                                                    }
                                                    false
                                                } else {
                                                    true
                                                }
                                            })

                                            SwipeToDismiss(
                                                state = dismissState,
                                                directions = setOf(DismissDirection.StartToEnd),
                                                background = {
                                                    val direction = dismissState.dismissDirection ?: return@SwipeToDismiss
                                                    val color = when (direction) {
                                                        DismissDirection.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                                                        else -> MaterialTheme.colorScheme.surface
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(16.dp),
                                                        contentAlignment = Alignment.CenterStart
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            Icon(
                                                                if (quickEditCategoryEnabled) Icons.Outlined.Edit else Icons.Outlined.CheckCircle,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                            )
                                                            Text(
                                                                if (quickEditCategoryEnabled) "修改分类" else "选择",
                                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                                style = MaterialTheme.typography.bodyLarge
                                                            )
                                                        }
                                                    }
                                                }
                                            ) {
                                                BookItem(
                                                    book = book,
                                                    isExpanded = expandedBookPath == book.path,
                                                    isSelected = selectedBooks.contains(book.path),
                                                    onExpandClick = {
                                                        if (isMultiSelectMode) {
                                                            viewModel.selectBook(book.path)
                                                        } else {
                                                            viewModel.setExpandedBook(if (expandedBookPath == book.path) null else book.path)
                                                        }
                                                    },
                                                    onDeleteClick = { viewModel.requestDeleteBook(book) },
                                                    onSyncClick = { viewModel.startPush(book) },
                                                    onChapterListClick = {
                                                        viewModel.showChapterList(
                                                            book
                                                        )
                                                    },
                                                    onContinueReadingClick = {
                                                        viewModel.continueReading(
                                                            book
                                                        )
                                                    },
                                                    onImportCoverClick = {
                                                        viewModel.requestImportCover(book)
                                                        onImportCoverClick()
                                                    },
                                                    onEditInfoClick = {
                                                        viewModel.showEditBookInfo(book)
                                                    },
                                                    isSyncEnabled = connectionState.isConnected,
                                                    lastChapterName = lastChapterNames[book.path]
                                                )
                                            }
                                        }
                                    }
                                }

                                ItemType.CATEGORY_HEADER -> {
                                    val category = item.category!!
                                    val categoryBooks =
                                        booksByCategory.first { it.first == category }.second
                                    val isExpanded = expandedCategories.contains(category)

                                    var showRenameDialog by remember { mutableStateOf(false) }
                                    var newCategoryName by remember(category) { mutableStateOf(category) }

                                    if (showRenameDialog) {
                                        AlertDialog(
                                            onDismissRequest = { showRenameDialog = false },
                                            title = { Text("重命名分类") },
                                            text = {
                                                TextField(
                                                    value = newCategoryName,
                                                    onValueChange = { newCategoryName = it },
                                                    label = { Text("分类名称") },
                                                    singleLine = true
                                                )
                                            },
                                            confirmButton = {
                                                TextButton(
                                                    onClick = {
                                                        if (newCategoryName.isNotBlank() && newCategoryName != category) {
                                                            viewModel.renameCategory(category, newCategoryName)
                                                            showRenameDialog = false
                                                        }
                                                    },
                                                    enabled = newCategoryName.isNotBlank() && newCategoryName != category
                                                ) {
                                                    Text("确定")
                                                }
                                            },
                                            dismissButton = {
                                                TextButton(onClick = { showRenameDialog = false }) {
                                                    Text("取消")
                                                }
                                            }
                                        )
                                    }

                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .then(
                                                if (quickRenameCategoryEnabled) {
                                                    Modifier.pointerInput(category) {
                                                        detectTapGestures(
                                                            onTap = {
                                                                viewModel.toggleCategoryExpansion(category)
                                                            },
                                                            onLongPress = {
                                                                showRenameDialog = true
                                                            }
                                                        )
                                                    }
                                                } else {
                                                    Modifier.clickable {
                                                        viewModel.toggleCategoryExpansion(category)
                                                    }
                                                }
                                            )
                                            .padding(
                                                top = 8.dp,
                                                bottom = if (isExpanded) 8.dp else 0.dp
                                            ),
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = category,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "(${categoryBooks.size})",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Icon(
                                                imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                                contentDescription = if (isExpanded) "折叠" else "展开",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
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
