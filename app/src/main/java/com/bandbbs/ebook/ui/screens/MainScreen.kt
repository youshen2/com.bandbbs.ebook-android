package com.bandbbs.ebook.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bandbbs.ebook.R
import com.bandbbs.ebook.ui.components.AboutBottomSheet
import com.bandbbs.ebook.ui.components.BookItem
import com.bandbbs.ebook.ui.components.CategoryBottomSheet
import com.bandbbs.ebook.ui.components.ConnectionErrorBottomSheet
import com.bandbbs.ebook.ui.components.EditBookInfoBottomSheet
import com.bandbbs.ebook.ui.components.FirstSyncConfirmBottomSheet
import com.bandbbs.ebook.ui.components.ImportBookBottomSheet
import com.bandbbs.ebook.ui.components.ImportProgressBottomSheet
import com.bandbbs.ebook.ui.components.ImportReportBottomSheet
import com.bandbbs.ebook.ui.components.OverwriteConfirmBottomSheet
import com.bandbbs.ebook.ui.components.PushBottomSheet
import com.bandbbs.ebook.ui.components.SyncOptionsBottomSheet
import com.bandbbs.ebook.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch


private enum class ItemType {
    RECENT_HEADER,
    RECENT_BOOK,
    CATEGORY_HEADER,
    BOOK
}


private data class ListItem(
    val type: ItemType,
    val category: String? = null,
    val book: com.bandbbs.ebook.ui.model.Book? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onImportClick: () -> Unit,
    onImportCoverClick: () -> Unit
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val books by viewModel.books.collectAsState()
    val recentBook by viewModel.recentBook.collectAsState()
    val pushState by viewModel.pushState.collectAsState()
    val importState by viewModel.importState.collectAsState()
    val importingState by viewModel.importingState.collectAsState()
    val importReportState by viewModel.importReportState.collectAsState()

    val bookToDelete by viewModel.bookToDelete.collectAsState()
    val syncOptionsState by viewModel.syncOptionsState.collectAsState()
    val overwriteConfirmState by viewModel.overwriteConfirmState.collectAsState()
    val bookForCoverImport by viewModel.bookForCoverImport.collectAsState()
    val connectionErrorState by viewModel.connectionErrorState.collectAsState()
    val categoryState by viewModel.categoryState.collectAsState()
    val firstSyncConfirmState by viewModel.firstSyncConfirmState.collectAsState()
    val editBookInfoState by viewModel.editBookInfoState.collectAsState()


    val expandedBookPath by viewModel.expandedBookPath.collectAsState()
    val expandedCategories by viewModel.expandedCategories.collectAsState()

    val scope = rememberCoroutineScope()
    val aboutSheetState = rememberModalBottomSheetState()
    var showAboutSheet by remember { mutableStateOf(false) }
    val categorySheetState = rememberModalBottomSheetState()
    val firstSyncConfirmSheetState = rememberModalBottomSheetState()

    val pushSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val importSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val importProgressSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val importReportSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val syncOptionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val overwriteConfirmSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val connectionErrorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editBookInfoSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
        var editLocalCategory by remember(state.book) { mutableStateOf(state.book.localCategory ?: "") }
        
        
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
                isResyncing = state.isResyncing
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
            ) {
                if (books.isEmpty()) {
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
                } else {

                    val booksByCategory = remember(books) {
                        books.groupBy { it.localCategory ?: "未分类" }
                            .toList()
                            .sortedBy { if (it.first == "未分类") "\uFFFF" else it.first }
                    }

                    val listItems = remember(booksByCategory, expandedCategories, recentBook) {
                        buildList {
                            if (recentBook != null) {
                                add(ListItem(ItemType.RECENT_HEADER))
                                add(ListItem(ItemType.RECENT_BOOK, book = recentBook))
                            }

                            booksByCategory.forEach { (category, categoryBooks) ->
                                add(ListItem(ItemType.CATEGORY_HEADER, category = category))
                                if (expandedCategories.contains(category)) {
                                    categoryBooks.forEach { book ->
                                        add(ListItem(ItemType.BOOK, book = book))
                                    }
                                }
                            }
                        }
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = listItems,
                            key = { item ->
                                when (item.type) {
                                    ItemType.RECENT_HEADER -> "header_recent"
                                    ItemType.RECENT_BOOK -> "recent_${item.book!!.id}"
                                    ItemType.CATEGORY_HEADER -> "category_${item.category}"
                                    ItemType.BOOK -> item.book!!.path
                                }
                            }
                        ) { item ->
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
                                    }
                                }

                                ItemType.RECENT_BOOK, ItemType.BOOK -> {
                                    val book = item.book!!
                                    AnimatedVisibility(
                                        visible = true,
                                        enter = scaleIn() + expandVertically(),
                                        exit = scaleOut()
                                    ) {
                                        BookItem(
                                            book = book,
                                            isExpanded = expandedBookPath == book.path,
                                            onExpandClick = {
                                                viewModel.setExpandedBook(if (expandedBookPath == book.path) null else book.path)
                                            },
                                            onDeleteClick = { viewModel.requestDeleteBook(book) },
                                            onSyncClick = { viewModel.startPush(book) },
                                            onChapterListClick = { viewModel.showChapterList(book) },
                                            onContinueReadingClick = { viewModel.continueReading(book) },
                                            onImportCoverClick = {
                                                viewModel.requestImportCover(book)
                                                onImportCoverClick()
                                            },
                                            onEditInfoClick = {
                                                viewModel.showEditBookInfo(book)
                                            },
                                            isSyncEnabled = connectionState.isConnected
                                        )
                                    }
                                }

                                ItemType.CATEGORY_HEADER -> {
                                    val category = item.category!!
                                    val categoryBooks =
                                        booksByCategory.first { it.first == category }.second
                                    val isExpanded = expandedCategories.contains(category)

                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.toggleCategoryExpansion(category)
                                            },
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
