package com.bandbbs.ebook.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.bandbbs.ebook.database.AppDatabase
import com.bandbbs.ebook.database.ChapterInfo
import com.bandbbs.ebook.logic.InterHandshake
import com.bandbbs.ebook.ui.model.Book
import com.bandbbs.ebook.ui.model.ChapterEditContent
import com.bandbbs.ebook.ui.model.ChapterSegment
import com.bandbbs.ebook.ui.viewmodel.handlers.CategoryHandler
import com.bandbbs.ebook.ui.viewmodel.handlers.ConnectionHandler
import com.bandbbs.ebook.ui.viewmodel.handlers.ImportHandler
import com.bandbbs.ebook.ui.viewmodel.handlers.LibraryHandler
import com.bandbbs.ebook.ui.viewmodel.handlers.PushHandler
import com.bandbbs.ebook.utils.BookInfoParser
import com.bandbbs.ebook.utils.ChapterContentManager
import com.bandbbs.ebook.utils.DataBackupManager
import com.bandbbs.ebook.utils.EpubParser
import com.bandbbs.ebook.utils.NvbParser
import com.bandbbs.ebook.utils.VersionChecker
import com.bandbbs.ebook.database.BookmarkEntity
import com.bandbbs.ebook.utils.BookmarkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.File

data class BandSettingsState(
    val fontSize: Int = 30,
    val opacity: Int = 100,
    val boldEnabled: Boolean = true,
    val verticalMargin: Int = 10,
    val timeFormat: String = "24h",
    val readMode: String = "scroll",
    val txtSizePage: Int = 400,
    val showProgressBar: Boolean = true,
    val showProgressBarPercent: Boolean = false,
    val progressBarOpacity: Int = 100,
    val progressBarHeight: Int = 8,
    val preloadChapter: Boolean = false,
    val preventParagraphSplitting: Boolean = false,
    val brightness: Int = 128,
    val brightnessFollowSystem: Boolean = true,
    val alwaysShowTime: Boolean = false,
    val alwaysShowBattery: Boolean = true,
    val alwaysShowTimeSensitivity: Int = 200,
    val chapterStartEmptyLines: Boolean = false,
    val chapterStartNumber: Boolean = false,
    val chapterStartName: Boolean = false,
    val chapterStartWordCount: Boolean = false,
    val chapterSwitchStyle: String = "button",
    val chapterSwitchHeight: Int = 80,
    val chapterSwitchSensitivity: Int = 50,
    val chapterSwitchShowInfo: Boolean = false,
    val swipeSensitivity: Int = 80,
    val swipe: String = "column",
    val autoReadEnabled: Boolean = false,
    val autoReadSpeed: Int = 10,
    val autoReadDistance: Int = 100,
    val gesture: String = "single",
    val progressSaveMode: String = "exit",
    val progressSaveInterval: Int = 10,
    val shelfMarqueeEnabled: Boolean = false,
    val bookmarkMarqueeEnabled: Boolean = false,
    val bookinfoMarqueeEnabled: Boolean = true,
    val chapterListMarqueeEnabled: Boolean = false,
    val textReaderMarqueeEnabled: Boolean = false,
    val detailMarqueeEnabled: Boolean = false,
    val detailProgressMarqueeEnabled: Boolean = false,
    val nostalgicPageTurnMode: String = "topBottomClick",
    val teacherScreenEnabled: Boolean = false
)

data class GlobalLoadingState(
    val isLoading: Boolean = false,
    val message: String = ""
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val booksDir = File(application.filesDir, "books").apply { mkdirs() }
    private val db = AppDatabase.getDatabase(application)
    private val prefs: SharedPreferences =
        application.getSharedPreferences("ebook_prefs", Context.MODE_PRIVATE)
    private val readerPrefs: SharedPreferences =
        application.getSharedPreferences("chapter_reader_prefs", Context.MODE_PRIVATE)
    private val FIRST_SYNC_CONFIRMED_KEY = "first_sync_confirmed"
    private val FIRST_SYNC_READING_DATA_CONFIRMED_KEY = "first_sync_reading_data_confirmed"

    private val _connectionState = MutableStateFlow(ConnectionState())
    val connectionState = _connectionState.asStateFlow()

    private val _books = MutableStateFlow<List<Book>>(emptyList())
    val books = _books.asStateFlow()

    private val _recentBook = MutableStateFlow<Book?>(null)
    val recentBook = _recentBook.asStateFlow()

    private val _recentUpdatedBook = MutableStateFlow<Book?>(null)
    val recentUpdatedBook = _recentUpdatedBook.asStateFlow()


    private val _expandedBookPath = MutableStateFlow<String?>(null)
    val expandedBookPath = _expandedBookPath.asStateFlow()

    private val _expandedCategories = MutableStateFlow<Set<String>>(emptySet())
    val expandedCategories = _expandedCategories.asStateFlow()

    private val _pushState = MutableStateFlow(PushState())
    val pushState = _pushState.asStateFlow()

    private val _importState = MutableStateFlow<ImportState?>(null)
    val importState = _importState.asStateFlow()

    private val _importingState = MutableStateFlow<ImportingState?>(null)
    val importingState = _importingState.asStateFlow()

    private val _importReportState = MutableStateFlow<ImportReportState?>(null)
    val importReportState = _importReportState.asStateFlow()

    private val _selectedBookForChapters = MutableStateFlow<Book?>(null)
    val selectedBookForChapters = _selectedBookForChapters.asStateFlow()

    private val _chaptersForSelectedBook = MutableStateFlow<List<ChapterInfo>>(emptyList())
    val chaptersForSelectedBook = _chaptersForSelectedBook.asStateFlow()

    private val _chapterToPreview =
        MutableStateFlow<com.bandbbs.ebook.ui.model.ChapterWithContent?>(null)
    val chapterToPreview = _chapterToPreview.asStateFlow()

    private val _chaptersForPreview = MutableStateFlow<List<ChapterInfo>>(emptyList())
    val chaptersForPreview = _chaptersForPreview.asStateFlow()

    private val _chapterEditorContent = MutableStateFlow<ChapterEditContent?>(null)
    val chapterEditorContent = _chapterEditorContent.asStateFlow()

    private val _bookToDelete = MutableStateFlow<Book?>(null)
    val bookToDelete = _bookToDelete.asStateFlow()

    private val _booksToDelete = MutableStateFlow<List<Book>>(emptyList())
    val booksToDelete = _booksToDelete.asStateFlow()

    private val _syncOptionsState = MutableStateFlow<SyncOptionsState?>(null)
    val syncOptionsState = _syncOptionsState.asStateFlow()

    private val _overwriteConfirmState = MutableStateFlow<OverwriteConfirmState?>(null)
    val overwriteConfirmState = _overwriteConfirmState.asStateFlow()

    private val _bookForCoverImport = MutableStateFlow<Book?>(null)
    val bookForCoverImport = _bookForCoverImport.asStateFlow()

    private val _connectionErrorState = MutableStateFlow<ConnectionErrorState?>(null)
    val connectionErrorState = _connectionErrorState.asStateFlow()

    private val _categoryState = MutableStateFlow<CategoryState?>(null)
    val categoryState = _categoryState.asStateFlow()

    private val _firstSyncConfirmState = MutableStateFlow<Book?>(null)
    val firstSyncConfirmState = _firstSyncConfirmState.asStateFlow()

    private val _editBookInfoState = MutableStateFlow<EditBookInfoState?>(null)
    val editBookInfoState = _editBookInfoState.asStateFlow()

    private val _syncReadingDataState = MutableStateFlow(SyncReadingDataState())
    val syncReadingDataState = _syncReadingDataState.asStateFlow()

    private val _syncResultState = MutableStateFlow<SyncResultState?>(null)
    val syncResultState = _syncResultState.asStateFlow()

    private var syncReadingDataJob: Job? = null

    private val _versionIncompatibleState = MutableStateFlow<VersionIncompatibleState?>(null)
    val versionIncompatibleState = _versionIncompatibleState.asStateFlow()

    private val _updateCheckState = MutableStateFlow(UpdateCheckState())
    val updateCheckState = _updateCheckState.asStateFlow()

    private val _ipCollectionPermissionState = MutableStateFlow(IpCollectionPermissionState())
    val ipCollectionPermissionState = _ipCollectionPermissionState.asStateFlow()

    private val _bandStorageInfo = MutableStateFlow(BandStorageInfo(isLoading = false))
    val bandStorageInfo = _bandStorageInfo.asStateFlow()

    private val _backupRestoreState = MutableStateFlow<BackupRestoreResult?>(null)
    val backupRestoreState = _backupRestoreState.asStateFlow()

    private val _bandSettingsState = MutableStateFlow<BandSettingsState?>(null)
    val bandSettingsState = _bandSettingsState.asStateFlow()

    private val _globalLoadingState = MutableStateFlow(GlobalLoadingState())
    val globalLoadingState = _globalLoadingState.asStateFlow()

    private val IP_COLLECTION_PERMISSION_KEY = "ip_collection_permission"
    private val IP_COLLECTION_PERMISSION_ASKED_KEY = "ip_collection_permission_asked"
    private val SHOW_RECENT_IMPORT_KEY = "show_recent_import"
    private val SHOW_RECENT_UPDATE_KEY = "show_recent_update"
    private val AUTO_CHECK_UPDATES_KEY = "auto_check_updates"
    private val SHOW_CONNECTION_ERROR_KEY = "show_connection_error"
    private val SHOW_SEARCH_BAR_KEY = "show_search_bar"
    private val THEME_MODE_KEY = "theme_mode"
    private val QUICK_EDIT_CATEGORY_KEY = "quick_edit_category"
    private val AUTO_MINIMIZE_ON_TRANSFER_KEY = "auto_minimize_on_transfer"
    private val AUTO_RETRY_ON_TRANSFER_ERROR_KEY = "auto_retry_on_transfer_error"
    private val HAS_CLICKED_TRANSFER_BUTTON_KEY = "has_clicked_transfer_button"
    private val QUICK_RENAME_CATEGORY_KEY = "quick_rename_category"
    private val LAST_SPLIT_METHOD_KEY = "last_split_method"

    private var FIRST_AUTO_CHECK = true


    private val _showRecentImport = MutableStateFlow(prefs.getBoolean(SHOW_RECENT_IMPORT_KEY, true))
    val showRecentImport = _showRecentImport.asStateFlow()

    private val _showRecentUpdate = MutableStateFlow(prefs.getBoolean(SHOW_RECENT_UPDATE_KEY, true))
    val showRecentUpdate = _showRecentUpdate.asStateFlow()

    private val _autoCheckUpdates = MutableStateFlow(prefs.getBoolean(AUTO_CHECK_UPDATES_KEY, true))
    val autoCheckUpdates = _autoCheckUpdates.asStateFlow()

    private val _ipCollectionAllowed =
        MutableStateFlow(prefs.getBoolean(IP_COLLECTION_PERMISSION_KEY, false))
    val ipCollectionAllowed = _ipCollectionAllowed.asStateFlow()

    private val _showConnectionError =
        MutableStateFlow(prefs.getBoolean(SHOW_CONNECTION_ERROR_KEY, true))
    val showConnectionError = _showConnectionError.asStateFlow()

    private val _showSearchBar = MutableStateFlow(prefs.getBoolean(SHOW_SEARCH_BAR_KEY, true))
    val showSearchBar = _showSearchBar.asStateFlow()

    private val _quickEditCategoryEnabled =
        MutableStateFlow(prefs.getBoolean(QUICK_EDIT_CATEGORY_KEY, false))
    val quickEditCategoryEnabled = _quickEditCategoryEnabled.asStateFlow()

    private val _autoMinimizeOnTransfer =
        MutableStateFlow(prefs.getBoolean(AUTO_MINIMIZE_ON_TRANSFER_KEY, false))
    val autoMinimizeOnTransfer = _autoMinimizeOnTransfer.asStateFlow()

    private val _autoRetryOnTransferError =
        MutableStateFlow(prefs.getBoolean(AUTO_RETRY_ON_TRANSFER_ERROR_KEY, false))
    val autoRetryOnTransferError = _autoRetryOnTransferError.asStateFlow()

    private val _hasClickedTransferButton =
        MutableStateFlow(prefs.getBoolean(HAS_CLICKED_TRANSFER_BUTTON_KEY, false))
    val hasClickedTransferButton = _hasClickedTransferButton.asStateFlow()

    private val _quickRenameCategoryEnabled =
        MutableStateFlow(prefs.getBoolean(QUICK_RENAME_CATEGORY_KEY, false))
    val quickRenameCategoryEnabled = _quickRenameCategoryEnabled.asStateFlow()

    private val _isMultiSelectMode = MutableStateFlow(false)
    val isMultiSelectMode = _isMultiSelectMode.asStateFlow()

    private val _selectedBooks = MutableStateFlow<Set<String>>(emptySet())
    val selectedBooks = _selectedBooks.asStateFlow()

    enum class ThemeMode {
        LIGHT, DARK, SYSTEM
    }

    private val _themeMode = MutableStateFlow(
        ThemeMode.valueOf(
            prefs.getString(THEME_MODE_KEY, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        )
    )
    val themeMode = _themeMode.asStateFlow()

    private val connectionHandler = ConnectionHandler(
        scope = viewModelScope,
        connectionState = _connectionState,
        connectionErrorState = _connectionErrorState,
        showConnectionError = _showConnectionError,
        versionIncompatibleState = _versionIncompatibleState
    ).apply {
        onBandConnected = { deviceName ->
            if (FIRST_AUTO_CHECK) autoCheckUpdates()
            refreshBandStorageInfo()
        }
        onBandVersionReceived = { bandVersion ->
            checkBandUpdateOnly(bandVersion)
        }
    }

    private val categoryHandler = CategoryHandler(
        prefs = prefs,
        db = db,
        scope = viewModelScope,
        categoryState = _categoryState,
        importState = _importState,
        onBooksChanged = { loadBooks() }
    )

    private val importHandler = ImportHandler(
        application = application,
        db = db,
        booksDir = booksDir,
        scope = viewModelScope,
        booksState = _books,
        importState = _importState,
        importingState = _importingState,
        importReportState = _importReportState,
        overwriteConfirmState = _overwriteConfirmState,
        onBooksChanged = { loadBooks() }
    )

    private val pushHandler = PushHandler(
        db = db,
        prefs = prefs,
        scope = viewModelScope,
        pushState = _pushState,
        syncOptionsState = _syncOptionsState,
        firstSyncConfirmState = _firstSyncConfirmState,
        connectionHandler = connectionHandler,
        firstSyncConfirmedKey = FIRST_SYNC_CONFIRMED_KEY,
        appContext = application.applicationContext,
        autoRetryOnTransferError = _autoRetryOnTransferError
    )

    private val libraryHandler = LibraryHandler(
        application = application,
        db = db,
        booksDir = booksDir,
        scope = viewModelScope,
        bookToDelete = _bookToDelete,
        selectedBookForChapters = _selectedBookForChapters,
        chaptersForSelectedBook = _chaptersForSelectedBook,
        chapterToPreview = _chapterToPreview,
        chaptersForPreview = _chaptersForPreview,
        bookForCoverImport = _bookForCoverImport,
        chapterEditorContent = _chapterEditorContent,
        onBooksChanged = { loadBooks() }
    )

    init {
        loadBooks()
        performInitialUpdateCheck()
    }

    private fun performInitialUpdateCheck() {
        val autoCheckEnabled = prefs.getBoolean(AUTO_CHECK_UPDATES_KEY, true)
        if (!autoCheckEnabled) {
            return
        }

        val ipCollectionAllowed = prefs.getBoolean(IP_COLLECTION_PERMISSION_KEY, false)
        val hasAskedBefore = prefs.getBoolean(IP_COLLECTION_PERMISSION_ASKED_KEY, false)

        if (!hasAskedBefore) {
            _ipCollectionPermissionState.value = IpCollectionPermissionState(
                showSheet = true,
                isFirstTime = true
            )
            return
        }

        if (hasAskedBefore && ipCollectionAllowed) {
            performUpdateCheck(isAutoCheck = true)
        }
    }


    fun setExpandedBook(path: String?) {
        _expandedBookPath.value = path
    }

    fun toggleCategoryExpansion(category: String) {
        val current = _expandedCategories.value
        if (current.contains(category)) {
            _expandedCategories.value = current - category
        } else {
            _expandedCategories.value = current + category
        }
    }

    fun getCategories(): List<String> = categoryHandler.getCategories()

    fun showCategorySelector(book: Book? = null) = categoryHandler.showCategorySelector(book)

    fun showCategorySelectorForEditBookInfo(
        book: com.bandbbs.ebook.database.BookEntity,
        onCategorySelected: (String?) -> Unit
    ) {
        categoryHandler.showCategorySelectorForEdit(book.localCategory, onCategorySelected)
    }

    fun createCategory(categoryName: String) = categoryHandler.createCategory(categoryName)

    fun deleteCategory(categoryName: String) = categoryHandler.deleteCategory(categoryName)

    fun selectCategory(category: String?) = categoryHandler.selectCategory(category)

    fun dismissCategorySelector() = categoryHandler.dismissCategorySelector()

    fun enterMultiSelectMode() {
        _isMultiSelectMode.value = true
        _selectedBooks.value = emptySet()
    }

    fun exitMultiSelectMode() {
        _isMultiSelectMode.value = false
        _selectedBooks.value = emptySet()
    }

    fun selectBook(bookPath: String) {
        val current = _selectedBooks.value.toMutableSet()
        if (current.contains(bookPath)) {
            current.remove(bookPath)
        } else {
            current.add(bookPath)
        }
        _selectedBooks.value = current
    }

    fun requestDeleteSelectedBooks() {
        val selectedPaths = _selectedBooks.value
        if (selectedPaths.isEmpty()) return

        val booksToDelete = _books.value.filter { it.path in selectedPaths }
        if (booksToDelete.isEmpty()) return

        _booksToDelete.value = booksToDelete
    }

    fun cancelDeleteSelectedBooks() {
        _booksToDelete.value = emptyList()
    }

    fun confirmDeleteSelectedBooks() {
        val booksToDelete = _booksToDelete.value
        if (booksToDelete.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            booksToDelete.forEach { book ->
                File(book.path).delete()
                val bookEntity = db.bookDao().getBookByPath(book.path)
                if (bookEntity != null) {
                    val context = application.applicationContext
                    ChapterContentManager.deleteBookChapters(context, bookEntity.id)
                    db.chapterDao().deleteChaptersByBookId(bookEntity.id)
                    db.bookDao().delete(bookEntity)
                }
            }
            withContext(Dispatchers.Main) {
                _booksToDelete.value = emptyList()
                loadBooks()
                exitMultiSelectMode()
            }
        }
    }

    fun setConnection(connection: InterHandshake) = connectionHandler.setConnection(connection)

    fun reconnect() = connectionHandler.reconnect()

    fun refreshBandStorageInfo() {
        if (!connectionHandler.isConnected()) {
            _bandStorageInfo.value = BandStorageInfo(isLoading = false)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _bandStorageInfo.value = _bandStorageInfo.value.copy(isLoading = true)
                val fileConnection = connectionHandler.getFileConnection()
                fileConnection.onStorageInfo = { storageInfo ->
                    _bandStorageInfo.value = BandStorageInfo(
                        product = storageInfo.product,
                        totalStorage = storageInfo.totalStorage,
                        availableStorage = storageInfo.availableStorage,
                        reservedStorage = storageInfo.reservedStorage,
                        usedStorage = storageInfo.usedStorage,
                        actualAvailable = storageInfo.actualAvailable,
                        isLoading = false
                    )
                }
                fileConnection.getStorageInfo()
            } catch (e: Exception) {
                _bandStorageInfo.value = BandStorageInfo(isLoading = false)
            }
        }
    }

    fun dismissConnectionError() = connectionHandler.dismissConnectionError()

    fun startImport(uri: android.net.Uri) = importHandler.startImport(uri)

    fun startImportBatch(uris: List<android.net.Uri>) = importHandler.startImportBatch(uris)

    fun cancelImport() = importHandler.cancelImport()

    fun confirmImport(
        bookName: String,
        splitMethod: String,
        noSplit: Boolean,
        wordsPerChapter: Int,
        selectedCategory: String? = null,
        enableChapterMerge: Boolean = false,
        mergeMinWords: Int = 500,
        enableChapterRename: Boolean = false,
        renamePattern: String = "",
        customRegex: String = ""
    ) = importHandler.confirmImport(
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

    fun cancelOverwriteConfirm() = importHandler.cancelOverwriteConfirm()

    fun confirmOverwrite() = importHandler.confirmOverwrite()

    fun dismissImportReport() = importHandler.dismissImportReport()

    fun requestDeleteBook(book: Book) = libraryHandler.requestDeleteBook(book)

    fun confirmDeleteBook() = libraryHandler.confirmDeleteBook()

    fun cancelDeleteBook() = libraryHandler.cancelDeleteBook()

    fun startPush(book: Book) {
        if (!_hasClickedTransferButton.value) {
            prefs.edit().putBoolean(HAS_CLICKED_TRANSFER_BUTTON_KEY, true).apply()
            _hasClickedTransferButton.value = true
        }
        pushHandler.startPush(book)
    }

    fun syncCoverOnly(book: Book) = pushHandler.syncCoverOnly(book)

    fun confirmPush(book: Book, selectedChapterIndices: Set<Int>, syncCover: Boolean = false) =
        pushHandler.confirmPush(book, selectedChapterIndices, syncCover)

    fun confirmFirstSync() = pushHandler.confirmFirstSync()

    fun cancelFirstSyncConfirm() = pushHandler.cancelFirstSyncConfirm()

    fun cancelPush() = pushHandler.cancelPush()

    fun resetPushState() = pushHandler.resetPushState()

    fun deleteBandChapters(book: Book, chapterIndices: Set<Int>) {
        val fileConn = runCatching { connectionHandler.getFileConnection() }.getOrElse {
            Log.e("MainViewModel", "Cannot get file connection")
            return
        }

        if (fileConn.busy) {
            Log.w("MainViewModel", "File connection is busy")
            return
        }

        _pushState.value = PushState(
            book = book,
            progress = 0.0,
            preview = "准备删除章节...",
            transferLog = listOf("准备删除 ${chapterIndices.size} 个章节..."),
            statusText = "准备删除...",
            isTransferring = true
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val success = fileConn.deleteChapters(
                    bookName = book.name,
                    chapterIndices = chapterIndices.toList(),
                    onProgress = { progress, message ->
                        viewModelScope.launch(Dispatchers.Main) {
                            val currentState = _pushState.value
                            val newLog = (currentState.transferLog + message).takeLast(100)
                            _pushState.value = currentState.copy(
                                progress = progress,
                                preview = message,
                                statusText = message,
                                transferLog = newLog
                            )
                        }
                    },
                    onSuccess = { message ->
                        viewModelScope.launch(Dispatchers.Main) {
                            val currentState = _pushState.value
                            val newLog = (currentState.transferLog + message).takeLast(100)
                            _pushState.value = currentState.copy(
                                progress = 1.0,
                                statusText = message,
                                isFinished = true,
                                isSuccess = true,
                                isTransferring = false,
                                transferLog = newLog
                            )
                            pushHandler.refreshBookStatus(book)
                        }
                    },
                    onError = { errorMessage ->
                        viewModelScope.launch(Dispatchers.Main) {
                            val currentState = _pushState.value
                            val newLog = (currentState.transferLog + "错误: $errorMessage").takeLast(100)
                            _pushState.value = currentState.copy(
                                statusText = errorMessage,
                                isFinished = true,
                                isSuccess = false,
                                isTransferring = false,
                                transferLog = newLog
                            )
                        }
                    }
                )

                if (!success) {
                    withContext(Dispatchers.Main) {
                        val currentState = _pushState.value
                        _pushState.value = currentState.copy(
                            statusText = "删除失败",
                            isFinished = true,
                            isSuccess = false,
                            isTransferring = false
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error deleting chapters from band", e)
                withContext(Dispatchers.Main) {
                    val currentState = _pushState.value
                    _pushState.value = currentState.copy(
                        statusText = "删除失败: ${e.message}",
                        isFinished = true,
                        isSuccess = false,
                        isTransferring = false
                    )
                }
            }
        }
    }

    fun showChapterList(book: Book) = libraryHandler.showChapterList(book)

    fun closeChapterList() = libraryHandler.closeChapterList()

    fun showChapterPreview(chapterId: Int) = libraryHandler.showChapterPreview(chapterId)

    fun continueReading(book: Book) = libraryHandler.continueReading(book)

    fun closeChapterPreview() {
        libraryHandler.closeChapterPreview()
        loadBooks()
    }

    fun renameChapter(chapterId: Int, newTitle: String) =
        libraryHandler.renameChapter(chapterId, newTitle)

    fun moveChapter(chapterId: Int, direction: Int) =
        libraryHandler.moveChapter(chapterId, direction)

    fun reorderChapter(chapterId: Int, targetIndex: Int) =
        libraryHandler.reorderChapter(chapterId, targetIndex)

    fun openChapterEditor(chapterId: Int) = libraryHandler.openChapterEditor(chapterId)

    fun closeChapterEditor() = libraryHandler.closeChapterEditor()

    fun saveChapterContent(chapterId: Int, title: String, content: String) =
        libraryHandler.saveChapterContent(chapterId, title, content)

    suspend fun loadChapterContent(chapterId: Int): String {
        return withContext(Dispatchers.IO) {
            val chapter = db.chapterDao().getChapterById(chapterId)
            if (chapter != null) {
                ChapterContentManager.readChapterContent(chapter.contentFilePath)
            } else {
                ""
            }
        }
    }

    fun addChapter(insertIndex: Int, title: String, content: String) =
        libraryHandler.addChapter(insertIndex, title, content)

    fun batchRenameChapters(
        chapterIds: List<Int>,
        prefix: String,
        suffix: String,
        startNumber: Int,
        padding: Int
    ) = libraryHandler.batchRenameChapters(chapterIds, prefix, suffix, startNumber, padding)

    fun mergeChapters(chapterIds: List<Int>, mergedTitle: String, insertBlankLine: Boolean) =
        libraryHandler.mergeChapters(chapterIds, mergedTitle, insertBlankLine)

    fun splitChapter(chapterId: Int, segments: List<ChapterSegment>) =
        libraryHandler.splitChapter(chapterId, segments)

    fun requestImportCover(book: Book) = libraryHandler.requestImportCover(book)

    fun cancelImportCover() = libraryHandler.cancelImportCover()

    fun importCoverForBook(uri: Uri) = libraryHandler.importCoverForBook(uri)

    fun showEditBookInfo(book: Book) {
        viewModelScope.launch(Dispatchers.IO) {
            val bookEntity = db.bookDao().getBookByPath(book.path)
            if (bookEntity != null) {
                _editBookInfoState.value = EditBookInfoState(bookEntity, isResyncing = false)
            }
        }
    }

    fun dismissEditBookInfo() {
        _editBookInfoState.value = null
    }

    fun saveBookInfo(bookEntity: com.bandbbs.ebook.database.BookEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            db.bookDao().update(bookEntity)
            loadBooks()
            withContext(Dispatchers.Main) {
                _editBookInfoState.value = null
            }
        }
    }

    suspend fun saveBookInfoWithoutDismiss(bookEntity: com.bandbbs.ebook.database.BookEntity) {
        withContext(Dispatchers.IO) {
            db.bookDao().update(bookEntity)
            loadBooks()
        }

        withContext(Dispatchers.Main) {
            _editBookInfoState.value?.let { currentState ->
                _editBookInfoState.value = currentState.copy(book = bookEntity)
            }
        }
    }

    fun resyncBookCategory(book: Book) {
        viewModelScope.launch(Dispatchers.IO) {
            val bookEntity = db.bookDao().getBookByPath(book.path)
            if (bookEntity != null) {
                try {
                    val context = getApplication<Application>().applicationContext
                    val fileUri = Uri.fromFile(File(book.path))

                    when (bookEntity.format) {
                        "nvb" -> {
                            val nvbBook = NvbParser.parse(context, fileUri)
                            val updatedEntity = bookEntity.copy(
                                category = nvbBook.metadata.category,
                                localCategory = bookEntity.localCategory
                            )
                            db.bookDao().update(updatedEntity)
                        }

                        "epub" -> {
                            val epubBook = EpubParser.parse(context, fileUri)


                        }
                    }
                    loadBooks()
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Failed to resync book category", e)
                }
            }
        }
    }

    fun resyncBookInfo(book: Book) {
        _globalLoadingState.value = GlobalLoadingState(isLoading = true, message = "正在同步书籍信息...")
        viewModelScope.launch(Dispatchers.IO) {

            _editBookInfoState.value?.let { currentState ->
                _editBookInfoState.value = currentState.copy(isResyncing = true)
            }

            val bookEntity = db.bookDao().getBookByPath(book.path)
            if (bookEntity != null) {
                try {
                    val context = getApplication<Application>().applicationContext
                    val fileUri = Uri.fromFile(File(book.path))
                    var updatedEntity: com.bandbbs.ebook.database.BookEntity? = null

                    when (bookEntity.format) {
                        "nvb" -> {
                            val nvbBook = NvbParser.parse(context, fileUri)
                            updatedEntity = bookEntity.copy(
                                author = nvbBook.metadata.author,
                                summary = nvbBook.metadata.summary,
                                bookStatus = nvbBook.metadata.bookStatus,
                                category = nvbBook.metadata.category
                            )
                            db.bookDao().update(updatedEntity)
                        }

                        "epub" -> {
                            val epubBook = EpubParser.parse(context, fileUri)
                            updatedEntity = bookEntity.copy(
                                author = epubBook.author
                            )
                            db.bookDao().update(updatedEntity)
                        }

                        "txt" -> {

                            val chapters = db.chapterDao().getChapterInfoForBook(bookEntity.id)
                            if (chapters.isNotEmpty() &&
                                (chapters[0].name == "简介" || chapters[0].name == "介绍")
                            ) {
                                val chapter = db.chapterDao().getChapterById(chapters[0].id)
                                if (chapter != null) {
                                    val content =
                                        ChapterContentManager.readChapterContent(chapter.contentFilePath)
                                    val parsedInfo =
                                        BookInfoParser.parseIntroductionContent(content)
                                    if (parsedInfo != null) {
                                        updatedEntity = bookEntity.copy(
                                            author = parsedInfo.author ?: bookEntity.author,
                                            summary = parsedInfo.summary ?: bookEntity.summary,
                                            bookStatus = parsedInfo.status ?: bookEntity.bookStatus,
                                            category = parsedInfo.tags ?: bookEntity.category
                                        )
                                        db.bookDao().update(updatedEntity)
                                    }
                                }
                            }
                        }
                    }


                    if (updatedEntity != null) {
                        if (connectionHandler.isConnected()) {
                            try {
                                val fileConn = connectionHandler.getFileConnection()
                                fileConn.updateBookInfo(
                                    bookName = updatedEntity.name,
                                    author = updatedEntity.author,
                                    summary = updatedEntity.summary,
                                    bookStatus = updatedEntity.bookStatus,
                                    category = updatedEntity.category,
                                    localCategory = updatedEntity.localCategory
                                )
                                Log.d(
                                    "MainViewModel",
                                    "Book info updated on watch: ${updatedEntity.name}"
                                )
                            } catch (e: Exception) {
                                Log.e("MainViewModel", "Failed to update book info on watch", e)
                            }
                        }


                        withContext(Dispatchers.Main) {
                            _editBookInfoState.value =
                                EditBookInfoState(updatedEntity, isResyncing = false)
                        }
                    } else {

                        withContext(Dispatchers.Main) {
                            _editBookInfoState.value?.let { currentState ->
                                _editBookInfoState.value = currentState.copy(isResyncing = false)
                            }
                        }
                    }

                    loadBooks()
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Failed to resync book info", e)

                    withContext(Dispatchers.Main) {
                        _editBookInfoState.value?.let { currentState ->
                            _editBookInfoState.value = currentState.copy(isResyncing = false)
                        }
                    }
                }
            } else {

                withContext(Dispatchers.Main) {
                    _editBookInfoState.value?.let { currentState ->
                        _editBookInfoState.value = currentState.copy(isResyncing = false)
                    }
                }
            }
            withContext(Dispatchers.Main) {
                _globalLoadingState.value = GlobalLoadingState(isLoading = false)
            }
        }
    }

    fun syncAllReadingData() {
        Log.d("MainViewModel", "syncAllReadingData() called")
        if (!connectionHandler.isConnected()) {
            Log.w("MainViewModel", "Cannot sync: band not connected")
            _syncReadingDataState.value = SyncReadingDataState(
                isSyncing = false,
                statusText = "手环未连接",
                progress = 0f
            )
            return
        }

        val hasConfirmedFirstSync = prefs.getBoolean(FIRST_SYNC_READING_DATA_CONFIRMED_KEY, false)
        if (!hasConfirmedFirstSync) {
            Log.d("MainViewModel", "Showing confirm dialog for first time")
            _syncReadingDataState.value = _syncReadingDataState.value.copy(showConfirmDialog = true)
        } else {
            Log.d("MainViewModel", "First sync already confirmed, showing mode dialog directly")
            _syncReadingDataState.value = _syncReadingDataState.value.copy(showModeDialog = true)
        }
    }

    fun confirmSyncReadingData() {
        Log.d("MainViewModel", "confirmSyncReadingData() called")
        prefs.edit().putBoolean(FIRST_SYNC_READING_DATA_CONFIRMED_KEY, true).apply()
        _syncReadingDataState.value = _syncReadingDataState.value.copy(
            showConfirmDialog = false,
            showModeDialog = true
        )
    }

    fun cancelSyncReadingDataConfirm() {
        _syncReadingDataState.value = _syncReadingDataState.value.copy(showConfirmDialog = false)
    }

    fun setSyncModeAndStart(mode: SyncMode) {
        setSyncModesAndStart(mode, mode)
    }

    fun setSyncModesAndStart(progressMode: SyncMode, readingTimeMode: SyncMode, bookmarkMode: SyncMode = SyncMode.AUTO) {
        Log.d(
            "MainViewModel",
            "setSyncModesAndStart() called with progressMode: $progressMode, readingTimeMode: $readingTimeMode, bookmarkMode: $bookmarkMode"
        )
        syncReadingDataJob?.cancel()

        syncReadingDataJob = viewModelScope.launch(Dispatchers.IO) {
            try {

                val currentProgressMode = progressMode
                val currentReadingTimeMode = readingTimeMode
                val currentBookmarkMode = bookmarkMode

                withContext(Dispatchers.Main) {
                    _syncReadingDataState.value = _syncReadingDataState.value.copy(
                        showModeDialog = false,
                        progressSyncMode = currentProgressMode,
                        readingTimeSyncMode = currentReadingTimeMode,
                        bookmarkSyncMode = currentBookmarkMode
                    )
                }

                val allBooks = _books.value
                Log.d("MainViewModel", "Starting sync for ${allBooks.size} books")
                if (allBooks.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        _syncReadingDataState.value = SyncReadingDataState(
                            isSyncing = false,
                            statusText = "没有书籍需要同步",
                            progress = 1f
                        )
                    }
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    _syncReadingDataState.value = _syncReadingDataState.value.copy(
                        isSyncing = true,
                        statusText = "开始同步阅读数据...",
                        progress = 0f,
                        totalBooks = allBooks.size,
                        syncedBooks = 0
                    )
                }

                val fileConn = connectionHandler.getFileConnection()
                var syncedCount = 0
                val changedBooks = mutableListOf<String>()
                val failedBooks = mutableMapOf<String, String>()

                for ((index, book) in allBooks.withIndex()) {

                    if (!coroutineContext.isActive) {
                        withContext(Dispatchers.Main) {
                            _syncReadingDataState.value = SyncReadingDataState(
                                isSyncing = false,
                                statusText = "同步已取消",
                                progress = 0f
                            )
                        }
                        return@launch
                    }

                    withContext(Dispatchers.Main) {
                        _syncReadingDataState.value = _syncReadingDataState.value.copy(
                            currentBook = book.name,
                            progress = index.toFloat() / allBooks.size
                        )
                    }

                    val bookStatus = try {
                        fileConn.getBookStatus(book.name)
                    } catch (e: Exception) {
                        Log.e("MainViewModel", "Failed to get book status for ${book.name}", e)
                        failedBooks[book.name] = "获取书籍状态失败: ${e.message ?: "未知错误"}"
                        withContext(Dispatchers.Main) {
                            val currentFailed = _syncReadingDataState.value.failedBooks.toMutableMap()
                            currentFailed[book.name] = "获取书籍状态失败: ${e.message ?: "未知错误"}"
                            _syncReadingDataState.value = _syncReadingDataState.value.copy(
                                failedBooks = currentFailed
                            )
                        }
                        continue
                    }

                    val bookExistsOnBand = bookStatus.syncedChapters.isNotEmpty() || bookStatus.hasCover
                    if (!bookExistsOnBand) {
                        Log.d("MainViewModel", "Book ${book.name} does not exist on band: syncedChapters=${bookStatus.syncedChapters.size}, hasCover=${bookStatus.hasCover}")
                        failedBooks[book.name] = "手环端不存在"
                        withContext(Dispatchers.Main) {
                            val currentFailed = _syncReadingDataState.value.failedBooks.toMutableMap()
                            currentFailed[book.name] = "手环端不存在"
                            _syncReadingDataState.value = _syncReadingDataState.value.copy(
                                failedBooks = currentFailed
                            )
                        }
                        continue
                    }

                    Log.d("MainViewModel", "Book ${book.name} exists on band: syncedChapters=${bookStatus.syncedChapters.size}, hasCover=${bookStatus.hasCover}")

                    try {
                        Log.d("MainViewModel", "Syncing reading data for book: ${book.name}")

                        val bandReadingData: com.bandbbs.ebook.logic.ReadingDataResult? = try {
                            val data = fileConn.getReadingData(book.name)
                            Log.d(
                                "MainViewModel",
                                "Got reading data from band for ${book.name}: progress=${data.progress != null}, readingTime=${data.readingTime != null}"
                            )
                            data
                        } catch (e: Exception) {
                            Log.e(
                                "MainViewModel",
                                "Failed to get reading data from band for ${book.name}",
                                e
                            )
                            null
                        }


                        val phoneProgress = getPhoneReadingProgress(book)
                        Log.d(
                            "MainViewModel",
                            "Phone progress for ${book.name}: ${phoneProgress != null}"
                        )
                        var bandProgress: Map<String, Any>? = null
                        if (bandReadingData != null) {
                            try {
                                if (bandReadingData.progress != null) {
                                    val progressMap = org.json.JSONObject(bandReadingData.progress)
                                    val tempMap = mutableMapOf<String, Any>()
                                    val keys = progressMap.keys()
                                    while (keys.hasNext()) {
                                        val key = keys.next()
                                        val value = progressMap.get(key)

                                        if (key == "chapterIndex") {
                                            when {
                                                value == org.json.JSONObject.NULL -> {
                                                    continue
                                                }

                                                value is Int -> tempMap[key] = value
                                                value is Long -> tempMap[key] = value.toInt()
                                                value is Double -> tempMap[key] = value.toInt()
                                                value is String -> {
                                                    try {
                                                        val intValue = value.toInt()
                                                        if (intValue >= 0) {
                                                            tempMap[key] = intValue
                                                        }
                                                    } catch (e: Exception) {
                                                    }
                                                }

                                                else -> {
                                                    try {
                                                        val intValue = (value as? Number)?.toInt()
                                                        if (intValue != null && intValue >= 0) {
                                                            tempMap[key] = intValue
                                                        }
                                                    } catch (e: Exception) {
                                                    }
                                                }
                                            }
                                        } else {
                                            tempMap[key] = when (value) {
                                                is org.json.JSONObject -> value.toString()
                                                is org.json.JSONArray -> value.toString()
                                                is Boolean -> value
                                                is Int -> value
                                                is Long -> value
                                                is Double -> value
                                                is String -> value
                                                else -> value.toString()
                                            }
                                        }
                                    }

                                    if (tempMap.containsKey("chapterIndex")) {
                                        bandProgress = tempMap
                                    } else {
                                        Log.d(
                                            "MainViewModel",
                                            "Band progress has no valid chapterIndex, ignoring"
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("MainViewModel", "Failed to parse band reading data", e)
                            }
                        }


                        val phoneReadingTime = getPhoneReadingTime(book.name)
                        Log.d(
                            "MainViewModel",
                            "Phone reading time for ${book.name}: ${if (phoneReadingTime != null) "exists (totalSeconds=${phoneReadingTime["totalSeconds"]})" else "null"}"
                        )

                        var bandReadingTime: Map<String, Any>? = null
                        if (bandReadingData != null && bandReadingData.readingTime != null) {
                            try {
                                Log.d("MainViewModel", "Parsing band reading time for ${book.name}")
                                val readingTimeMap =
                                    org.json.JSONObject(bandReadingData.readingTime)
                                val tempMap = mutableMapOf<String, Any>()
                                val keys = readingTimeMap.keys()
                                while (keys.hasNext()) {
                                    val key = keys.next()
                                    val value = readingTimeMap.get(key)
                                    tempMap[key] = when (value) {
                                        is org.json.JSONObject -> {
                                            val sessionMap = mutableMapOf<String, Any>()
                                            val sessionKeys = value.keys()
                                            while (sessionKeys.hasNext()) {
                                                val sessionKey = sessionKeys.next()
                                                sessionMap[sessionKey] = value.get(sessionKey)
                                            }
                                            sessionMap
                                        }

                                        is org.json.JSONArray -> {
                                            val sessionList = mutableListOf<Any>()
                                            for (i in 0 until value.length()) {
                                                val sessionObj = value.getJSONObject(i)
                                                val sessionMap = mutableMapOf<String, Any>()
                                                val sessionKeys = sessionObj.keys()
                                                while (sessionKeys.hasNext()) {
                                                    val sessionKey = sessionKeys.next()
                                                    val sessionValue = sessionObj.get(sessionKey)
                                                    sessionMap[sessionKey] = when (sessionValue) {
                                                        is org.json.JSONObject -> sessionValue.toString()
                                                        is org.json.JSONArray -> sessionValue.toString()
                                                        is Boolean -> sessionValue
                                                        is Int -> sessionValue
                                                        is Long -> sessionValue
                                                        is Double -> sessionValue
                                                        is String -> sessionValue
                                                        else -> sessionValue.toString()
                                                    }
                                                }
                                                sessionList.add(sessionMap)
                                            }
                                            sessionList
                                        }

                                        is Boolean -> value
                                        is Int -> value
                                        is Long -> value
                                        is Double -> value
                                        is String -> value
                                        else -> value.toString()
                                    }
                                }
                                bandReadingTime = tempMap
                                val sessionsSize =
                                    when (val sessions = bandReadingTime["sessions"]) {
                                        is List<*> -> sessions.size
                                        else -> 0
                                    }
                                Log.d(
                                    "MainViewModel",
                                    "Parsed band reading time for ${book.name}: totalSeconds=${bandReadingTime["totalSeconds"]}, sessions=$sessionsSize"
                                )
                            } catch (e: Exception) {
                                Log.e(
                                    "MainViewModel",
                                    "Failed to parse band reading time for ${book.name}",
                                    e
                                )
                            }
                        } else {
                            Log.d("MainViewModel", "No band reading time data for ${book.name}")
                        }


                        Log.d(
                            "MainViewModel",
                            "Merging data for ${book.name} with progressMode: $currentProgressMode, readingTimeMode: $currentReadingTimeMode"
                        )


                        val finalProgress = when (currentProgressMode) {
                            SyncMode.AUTO -> {
                                val merged = mergeProgress(phoneProgress, bandProgress)
                                Log.d(
                                    "MainViewModel",
                                    "Auto merged progress for ${book.name}: ${merged != null}"
                                )
                                merged
                            }

                            SyncMode.BAND_ONLY -> {
                                Log.d("MainViewModel", "Using band progress for ${book.name}")
                                bandProgress
                            }

                            SyncMode.PHONE_ONLY -> {
                                Log.d("MainViewModel", "Using phone progress for ${book.name}")
                                phoneProgress
                            }
                        }

                        val finalReadingTime = when (currentReadingTimeMode) {
                            SyncMode.AUTO -> {
                                val merged = mergeReadingTime(phoneReadingTime, bandReadingTime)
                                Log.d(
                                    "MainViewModel",
                                    "Auto merged reading time for ${book.name}: ${if (merged != null) "totalSeconds=${merged["totalSeconds"]}, sessions=${(merged["sessions"] as? List<*>)?.size ?: 0}" else "null"}"
                                )
                                merged
                            }

                            SyncMode.BAND_ONLY -> {
                                Log.d("MainViewModel", "Using band reading time for ${book.name}")
                                bandReadingTime
                            }

                            SyncMode.PHONE_ONLY -> {
                                Log.d("MainViewModel", "Using phone reading time for ${book.name}")
                                phoneReadingTime
                            }
                        }

                        val hasProgressChange = when (currentProgressMode) {
                            SyncMode.AUTO -> finalProgress != phoneProgress
                            SyncMode.BAND_ONLY -> bandProgress != null
                            SyncMode.PHONE_ONLY -> false
                        }

                        val hasReadingTimeChange = when (currentReadingTimeMode) {
                            SyncMode.AUTO -> {
                                val phoneTotal = (phoneReadingTime?.get("totalSeconds") as? Number)?.toLong() ?: 0L
                                val bandTotal = (bandReadingTime?.get("totalSeconds") as? Number)?.toLong() ?: 0L
                                val finalTotal = (finalReadingTime?.get("totalSeconds") as? Number)?.toLong() ?: 0L
                                finalTotal != phoneTotal || (bandTotal > 0 && finalTotal != bandTotal)
                            }
                            SyncMode.BAND_ONLY -> bandReadingTime != null
                            SyncMode.PHONE_ONLY -> false
                        }

                        if (hasProgressChange || hasReadingTimeChange) {
                            changedBooks.add(book.name)
                        }


                        when (currentProgressMode) {
                            SyncMode.AUTO, SyncMode.BAND_ONLY -> {
                                savePhoneReadingProgress(book, finalProgress)
                            }

                            SyncMode.PHONE_ONLY -> {

                                Log.d(
                                    "MainViewModel",
                                    "PHONE_ONLY mode: keeping phone progress unchanged for ${book.name}"
                                )
                            }
                        }

                        when (currentReadingTimeMode) {
                            SyncMode.AUTO, SyncMode.BAND_ONLY -> {
                                savePhoneReadingTime(book.name, finalReadingTime)
                                Log.d("MainViewModel", "Saved reading time for ${book.name}")
                            }

                            SyncMode.PHONE_ONLY -> {

                                Log.d(
                                    "MainViewModel",
                                    "PHONE_ONLY mode: keeping phone reading time unchanged for ${book.name}"
                                )
                            }
                        }


                        val progressJson = when (currentProgressMode) {
                            SyncMode.AUTO, SyncMode.PHONE_ONLY -> {
                                finalProgress?.let { fp ->
                                    try {
                                        val normalized = HashMap<String, Any?>()
                                        normalized.putAll(fp)
                                        val rawOffsetAny = fp["offsetInChapter"]
                                        val rawOffset = when (rawOffsetAny) {
                                            is Number -> rawOffsetAny.toInt()
                                            is String -> rawOffsetAny.toIntOrNull() ?: 0
                                            else -> 0
                                        }
                                        var normalizedOffset = if (rawOffset < 0) 0 else rawOffset
                                        if (normalizedOffset % 2 == 1) normalizedOffset = Math.max(0, normalizedOffset - 1)
                                        if (fp.containsKey("offsetInChapter")) {
                                            normalized["offsetInChapter"] = normalizedOffset
                                        }
                                        org.json.JSONObject(normalized).toString()
                                    } catch (e: Exception) {
                                        Log.e("MainViewModel", "Failed to serialize progress", e)
                                        null
                                    }
                                }
                            }

                            SyncMode.BAND_ONLY -> {
                                Log.d(
                                    "MainViewModel",
                                    "BAND_ONLY mode: keeping band progress unchanged for ${book.name}"
                                )
                                null
                            }
                        }

                        val readingTimeJson = when (currentReadingTimeMode) {
                            SyncMode.AUTO, SyncMode.PHONE_ONLY -> {
                                finalReadingTime?.let {
                                    try {
                                        val json = org.json.JSONObject(it).toString()
                                        Log.d(
                                            "MainViewModel",
                                            "Serialized reading time JSON for ${book.name}: ${json.length} chars"
                                        )
                                        json
                                    } catch (e: Exception) {
                                        Log.e(
                                            "MainViewModel",
                                            "Failed to serialize reading time for ${book.name}",
                                            e
                                        )
                                        null
                                    }
                                }
                            }

                            SyncMode.BAND_ONLY -> {

                                Log.d(
                                    "MainViewModel",
                                    "BAND_ONLY mode: keeping band reading time unchanged for ${book.name}"
                                )
                                null
                            }
                        }

                        if (progressJson != null || readingTimeJson != null) {
                            Log.d(
                                "MainViewModel",
                                "Sending reading data to band for ${book.name}: progress=${progressJson != null}, readingTime=${readingTimeJson != null}"
                            )
                            fileConn.setReadingData(book.name, progressJson, readingTimeJson)
                            Log.d(
                                "MainViewModel",
                                "Successfully sent reading data to band for ${book.name}"
                            )
                        } else {
                            Log.d(
                                "MainViewModel",
                                "No reading data to send to band for ${book.name}"
                            )
                        }

                        try {
                            val phoneBookmarks = BookmarkManager.getBookmarksForSync(getApplication(), book.id)
                            val bandBookmarks = try {
                                fileConn.getBookmarks(book.name)
                            } catch (e: Exception) {
                                Log.e("MainViewModel", "Failed to get bookmarks from band for ${book.name}", e)
                                emptyList()
                            }

                            val currentBookmarkMode = _syncReadingDataState.value.bookmarkSyncMode
                            when (currentBookmarkMode) {
                                SyncMode.PHONE_ONLY -> {
                                    if (phoneBookmarks.isNotEmpty()) {
                                        val bookmarkData = phoneBookmarks.map { bm ->
                                            com.bandbbs.ebook.logic.BookmarkData(
                                                name = bm.name,
                                                chapterIndex = bm.chapterIndex,
                                                chapterName = bm.chapterName,
                                                offsetInChapter = bm.offsetInChapter,
                                                scrollOffset = bm.scrollOffset,
                                                time = bm.time
                                            )
                                        }
                                        fileConn.setBookmarks(book.name, bookmarkData)
                                        Log.d("MainViewModel", "Synced ${bookmarkData.size} bookmarks from phone to band for ${book.name}")
                                    }
                                }
                                SyncMode.BAND_ONLY -> {
                                    if (bandBookmarks.isNotEmpty()) {
                                        val bookmarkEntities = bandBookmarks.map { bm ->
                                            BookmarkEntity(
                                                bookId = book.id,
                                                name = bm.name,
                                                chapterIndex = bm.chapterIndex,
                                                chapterName = bm.chapterName,
                                                offsetInChapter = bm.offsetInChapter,
                                                scrollOffset = bm.scrollOffset,
                                                time = bm.time
                                            )
                                        }
                                        BookmarkManager.syncBookmarksFromBand(getApplication(), book.id, bookmarkEntities)
                                        Log.d("MainViewModel", "Synced ${bookmarkEntities.size} bookmarks from band to phone for ${book.name}")
                                    }
                                }
                                SyncMode.AUTO -> {
                                    val mergedBookmarks = mutableListOf<BookmarkEntity>()
                                    val bandBookmarkMap = bandBookmarks.associateBy { "${it.chapterIndex}_${it.offsetInChapter}" }
                                    val phoneBookmarkMap = phoneBookmarks.associateBy { "${it.chapterIndex}_${it.offsetInChapter}" }

                                    bandBookmarks.forEach { bm ->
                                        mergedBookmarks.add(
                                            BookmarkEntity(
                                                bookId = book.id,
                                                name = bm.name,
                                                chapterIndex = bm.chapterIndex,
                                                chapterName = bm.chapterName,
                                                offsetInChapter = bm.offsetInChapter,
                                                scrollOffset = bm.scrollOffset,
                                                time = bm.time
                                            )
                                        )
                                    }

                                    phoneBookmarks.forEach { bm ->
                                        val key = "${bm.chapterIndex}_${bm.offsetInChapter}"
                                        if (!bandBookmarkMap.containsKey(key)) {
                                            mergedBookmarks.add(bm)
                                        }
                                    }

                                    if (mergedBookmarks.isNotEmpty()) {
                                        val bookmarkData = mergedBookmarks.map { bm ->
                                            com.bandbbs.ebook.logic.BookmarkData(
                                                name = bm.name,
                                                chapterIndex = bm.chapterIndex,
                                                chapterName = bm.chapterName,
                                                offsetInChapter = bm.offsetInChapter,
                                                scrollOffset = bm.scrollOffset,
                                                time = bm.time
                                            )
                                        }
                                        fileConn.setBookmarks(book.name, bookmarkData)
                                        BookmarkManager.syncBookmarksFromBand(getApplication(), book.id, mergedBookmarks)
                                        Log.d("MainViewModel", "Merged and synced ${mergedBookmarks.size} bookmarks for ${book.name}")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("MainViewModel", "Failed to sync bookmarks for ${book.name}", e)
                        }

                        syncedCount++
                        withContext(Dispatchers.Main) {
                            _syncReadingDataState.value = _syncReadingDataState.value.copy(
                                syncedBooks = syncedCount
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("MainViewModel", "Failed to sync reading data for ${book.name}", e)
                        failedBooks[book.name] = "同步失败: ${e.message ?: "未知错误"}"
                        withContext(Dispatchers.Main) {
                            val currentFailed = _syncReadingDataState.value.failedBooks.toMutableMap()
                            currentFailed[book.name] = "同步失败: ${e.message ?: "未知错误"}"
                            _syncReadingDataState.value = _syncReadingDataState.value.copy(
                                failedBooks = currentFailed
                            )
                        }
                    }
                }


                loadBooks()

                withContext(Dispatchers.Main) {
                    val statusText = if (failedBooks.isEmpty()) {
                        "同步完成，共同步 $syncedCount 本书"
                    } else {
                        val failedCount = failedBooks.size
                        val failedDetails = failedBooks.entries.joinToString("；") { (name, reason) ->
                            "$name: $reason"
                        }
                        "同步完成，成功 $syncedCount 本，失败 $failedCount 本。失败原因：$failedDetails"
                    }
                    _syncReadingDataState.value = SyncReadingDataState(
                        isSyncing = false,
                        statusText = statusText,
                        progress = 1f,
                        totalBooks = allBooks.size,
                        syncedBooks = syncedCount,
                        failedBooks = failedBooks
                    )
                    if (changedBooks.isNotEmpty()) {
                        _syncResultState.value = SyncResultState(
                            changedBooks = changedBooks,
                            syncedCount = syncedCount
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {

                    withContext(Dispatchers.Main) {
                        _syncReadingDataState.value = SyncReadingDataState(
                            isSyncing = false,
                            statusText = "同步已取消",
                            progress = 0f
                        )
                    }
                } else {
                    Log.e("MainViewModel", "Failed to sync reading data", e)
                    withContext(Dispatchers.Main) {
                        _syncReadingDataState.value = SyncReadingDataState(
                            isSyncing = false,
                            statusText = "同步失败: ${e.message}",
                            progress = 0f
                        )
                    }
                }
            } finally {
                syncReadingDataJob = null
            }
        }
    }

    fun cancelSyncReadingData() {
        syncReadingDataJob?.cancel()
        syncReadingDataJob = null
    }

    fun clearSyncReadingDataState() {
        _syncReadingDataState.value = SyncReadingDataState()
    }

    fun dismissSyncResult() {
        _syncResultState.value = null
    }

    fun dismissSyncModeDialog() {
        _syncReadingDataState.value = _syncReadingDataState.value.copy(showModeDialog = false)
    }

    fun dismissVersionIncompatible() {
        _versionIncompatibleState.value = null
    }

    fun clearAllReadingTimeData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val readingTimePrefs = getApplication<Application>().getSharedPreferences(
                    "reading_time_prefs",
                    Context.MODE_PRIVATE
                )
                val editor = readingTimePrefs.edit()
                editor.clear()
                editor.apply()
                Log.d("MainViewModel", "Cleared all reading time data")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to clear reading time data", e)
            }
        }
    }

    fun checkForUpdates(isAutoCheck: Boolean = false) {

        val ipCollectionAllowed = prefs.getBoolean(IP_COLLECTION_PERMISSION_KEY, false)
        val hasAskedBefore = prefs.getBoolean(IP_COLLECTION_PERMISSION_ASKED_KEY, false)

        if (!hasAskedBefore) {

            _ipCollectionPermissionState.value = IpCollectionPermissionState(
                showSheet = true,
                isFirstTime = true
            )
            return
        }

        if (!ipCollectionAllowed) {

            if (!isAutoCheck) {
                _ipCollectionPermissionState.value = IpCollectionPermissionState(
                    showSheet = true,
                    isFirstTime = false
                )
            }
            return
        }


        performUpdateCheck(isAutoCheck)
    }


    fun autoCheckUpdates() {
        val ipCollectionAllowed = prefs.getBoolean(IP_COLLECTION_PERMISSION_KEY, false)
        if (!ipCollectionAllowed) {
            return
        }

        checkForUpdates(isAutoCheck = true)
        FIRST_AUTO_CHECK = false
    }

    fun onIpCollectionPermissionResult(allowed: Boolean) {
        val editor = prefs.edit()
        editor.putBoolean(IP_COLLECTION_PERMISSION_KEY, allowed)
        editor.putBoolean(IP_COLLECTION_PERMISSION_ASKED_KEY, true)
        editor.apply()

        _ipCollectionAllowed.value = allowed

        _ipCollectionPermissionState.value = IpCollectionPermissionState(showSheet = false)

        if (allowed) {
            performUpdateCheck(isAutoCheck = false)
        } else {
            _updateCheckState.value = UpdateCheckState(
                isChecking = false,
                errorMessage = "版本更新检测功能已禁用",
                deviceName = connectionHandler.getDeviceName(),
                showSheet = true,
                isAutoCheck = false
            )
        }
    }

    fun dismissIpCollectionPermissionSheet() {
        _ipCollectionPermissionState.value = IpCollectionPermissionState(showSheet = false)
    }


    fun setShowRecentImport(show: Boolean) {
        prefs.edit().putBoolean(SHOW_RECENT_IMPORT_KEY, show).apply()
        _showRecentImport.value = show
    }

    fun setShowRecentUpdate(show: Boolean) {
        prefs.edit().putBoolean(SHOW_RECENT_UPDATE_KEY, show).apply()
        _showRecentUpdate.value = show
    }

    fun setAutoCheckUpdates(enabled: Boolean) {
        prefs.edit().putBoolean(AUTO_CHECK_UPDATES_KEY, enabled).apply()
        _autoCheckUpdates.value = enabled
    }

    fun setIpCollectionAllowed(allowed: Boolean) {
        prefs.edit().putBoolean(IP_COLLECTION_PERMISSION_KEY, allowed).apply()
        _ipCollectionAllowed.value = allowed
    }

    fun setShowConnectionError(show: Boolean) {
        prefs.edit().putBoolean(SHOW_CONNECTION_ERROR_KEY, show).apply()
        _showConnectionError.value = show
    }

    fun setShowSearchBar(show: Boolean) {
        prefs.edit().putBoolean(SHOW_SEARCH_BAR_KEY, show).apply()
        _showSearchBar.value = show
    }

    fun setQuickEditCategory(enabled: Boolean) {
        prefs.edit().putBoolean(QUICK_EDIT_CATEGORY_KEY, enabled).apply()
        _quickEditCategoryEnabled.value = enabled
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(THEME_MODE_KEY, mode.name).apply()
        _themeMode.value = mode
    }

    fun setAutoMinimizeOnTransfer(enabled: Boolean) {
        prefs.edit().putBoolean(AUTO_MINIMIZE_ON_TRANSFER_KEY, enabled).apply()
        _autoMinimizeOnTransfer.value = enabled
    }

    fun setAutoRetryOnTransferError(enabled: Boolean) {
        prefs.edit().putBoolean(AUTO_RETRY_ON_TRANSFER_ERROR_KEY, enabled).apply()
        _autoRetryOnTransferError.value = enabled
    }

    fun setQuickRenameCategory(enabled: Boolean) {
        prefs.edit().putBoolean(QUICK_RENAME_CATEGORY_KEY, enabled).apply()
        _quickRenameCategoryEnabled.value = enabled
    }

    fun renameCategory(oldName: String, newName: String) {
        categoryHandler.renameCategory(oldName, newName)
    }

    private fun performUpdateCheck(isAutoCheck: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val deviceName = connectionHandler.getDeviceName()

            if (!isAutoCheck) {
                _updateCheckState.value = UpdateCheckState(
                    isChecking = true,
                    deviceName = deviceName,
                    showSheet = true,
                    isAutoCheck = isAutoCheck
                )
            }

            try {
                val context = getApplication<Application>().applicationContext
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)

                @Suppress("DEPRECATION")
                val currentVersionCode = packageInfo.versionCode


                val androidResult = VersionChecker.checkUpdate(currentVersionCode)
                val updateInfoList = mutableListOf<VersionChecker.UpdateInfo>()
                var errorMsg: String? = null


                androidResult.fold(
                    onSuccess = { androidUpdateInfo ->
                        if (androidUpdateInfo.hasUpdate && androidUpdateInfo.deviceType == "android") {
                            updateInfoList.add(androidUpdateInfo)
                        }
                    },
                    onFailure = { error ->
                        errorMsg = "检查手机更新失败: ${error.message}"
                    }
                )

                withContext(Dispatchers.Main) {
                    val hasUpdates = updateInfoList.isNotEmpty()
                    _updateCheckState.value = UpdateCheckState(
                        isChecking = false,
                        updateInfo = updateInfoList.firstOrNull(),
                        updateInfoList = updateInfoList,
                        errorMessage = errorMsg,
                        deviceName = deviceName,
                        showSheet = !isAutoCheck || hasUpdates,
                        isAutoCheck = isAutoCheck
                    )
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "获取版本信息失败", e)
                withContext(Dispatchers.Main) {
                    _updateCheckState.value = UpdateCheckState(
                        isChecking = false,
                        errorMessage = "获取版本信息失败: ${e.message}",
                        deviceName = deviceName,
                        showSheet = !isAutoCheck,
                        isAutoCheck = isAutoCheck
                    )
                }
            }
        }
    }

    fun dismissUpdateCheck() {
        _updateCheckState.value = UpdateCheckState()
    }

    private fun checkBandUpdateOnly(bandVersion: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val deviceName = connectionHandler.getDeviceName()
            if (deviceName == null) {
                Log.w("MainViewModel", "设备名称为空，无法检查手环更新")
                return@launch
            }

            try {
                val bandResult = VersionChecker.checkBandUpdate(deviceName, bandVersion)

                bandResult.fold(
                    onSuccess = { bandUpdateInfo ->
                        if (bandUpdateInfo.hasUpdate && bandUpdateInfo.deviceType == "band") {
                            withContext(Dispatchers.Main) {
                                _updateCheckState.value = UpdateCheckState(
                                    isChecking = false,
                                    updateInfo = bandUpdateInfo,
                                    updateInfoList = listOf(bandUpdateInfo),
                                    deviceName = deviceName,
                                    showSheet = true,
                                    isAutoCheck = true
                                )
                            }
                        }
                    },
                    onFailure = { error ->
                        Log.e("MainViewModel", "检查手环更新失败", error)
                    }
                )
            } catch (e: Exception) {
                Log.e("MainViewModel", "检查手环更新异常", e)
            }
        }
    }

    private val json = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    private suspend fun getPhoneReadingProgress(book: Book): Map<String, Any>? {
        val lastReadChapterId = readerPrefs.getInt("last_read_chapter_${book.id}", -1)
        if (lastReadChapterId == -1) return null

        val chapter = db.chapterDao().getChapterById(lastReadChapterId) ?: return null
        val allChapters = db.chapterDao().getChapterInfoForBook(book.id)
        val chapterIndex = allChapters.indexOfFirst { it.id == lastReadChapterId }
        if (chapterIndex == -1) return null

        val pageIndex = readerPrefs.getInt("reading_position_index_$lastReadChapterId", 0)
        val offset = readerPrefs.getInt("reading_position_offset_$lastReadChapterId", 0)
        val lastReadTimestamp = readerPrefs.getLong("last_read_timestamp_${book.id}", 0L)

        return mapOf(
            "chapterIndex" to chapterIndex,
            "offsetInChapter" to offset,
            "scrollOffset" to 0,
            "lastReadTimestamp" to (if (lastReadTimestamp > 0L) lastReadTimestamp else System.currentTimeMillis())
        )
    }


    private fun mergeProgress(
        phoneProgress: Map<String, Any>?,
        bandProgress: Map<String, Any>?
    ): Map<String, Any>? {
        if (phoneProgress == null && bandProgress == null) return null
        if (phoneProgress == null) return bandProgress
        if (bandProgress == null) return phoneProgress

        val phoneTimestamp = (phoneProgress["lastReadTimestamp"] as? Number)?.toLong() ?: 0L
        val bandTimestamp = (bandProgress["lastReadTimestamp"] as? Number)?.toLong() ?: 0L

        return if (phoneTimestamp >= bandTimestamp) phoneProgress else bandProgress
    }


    private suspend fun savePhoneReadingProgress(book: Book, progress: Map<String, Any>?) {
        if (progress == null) return

        val chapterIndex = (progress["chapterIndex"] as? Number)?.toInt()
        if (chapterIndex != null && chapterIndex >= 0) {
            val allChapters = db.chapterDao().getChapterInfoForBook(book.id)
            if (chapterIndex < allChapters.size && allChapters.isNotEmpty()) {
                val chapterId = allChapters[chapterIndex].id
                val offset = (progress["offsetInChapter"] as? Number)?.toInt() ?: 0
                val timestamp = (progress["lastReadTimestamp"] as? Number)?.toLong() ?: 0L



                if (timestamp > 0L || chapterIndex > 0 || offset > 0) {
                    readerPrefs.edit()
                        .putInt("last_read_chapter_${book.id}", chapterId)
                        .putInt("reading_position_offset_$chapterId", offset)
                        .putLong(
                            "last_read_timestamp_${book.id}",
                            if (timestamp > 0L) timestamp else System.currentTimeMillis()
                        )
                        .apply()
                    Log.d(
                        "MainViewModel",
                        "Saved progress for ${book.name}: chapterIndex=$chapterIndex, chapterId=$chapterId, offset=$offset, timestamp=$timestamp"
                    )
                } else {
                    Log.w(
                        "MainViewModel",
                        "Skipping save progress for ${book.name}: invalid data (chapterIndex=$chapterIndex, offset=$offset, timestamp=$timestamp)"
                    )
                }
            } else {
                Log.w(
                    "MainViewModel",
                    "Invalid chapterIndex $chapterIndex for ${book.name} (total chapters: ${allChapters.size})"
                )
            }
        } else {
            Log.w("MainViewModel", "No valid chapterIndex in progress for ${book.name}")
        }
    }

    private fun getPhoneReadingTime(bookName: String): Map<String, Any>? {
        val readingTimePrefs = getApplication<Application>().getSharedPreferences(
            "reading_time_prefs",
            Context.MODE_PRIVATE
        )
        val totalSeconds = readingTimePrefs.getLong("${bookName}_total_seconds", 0L)
        Log.d("MainViewModel", "getPhoneReadingTime($bookName): totalSeconds=$totalSeconds")
        if (totalSeconds == 0L) {
            Log.d("MainViewModel", "No reading time data found for $bookName")
            return null
        }

        val sessionsJson = readingTimePrefs.getString("${bookName}_sessions", null)
        val sessions = if (sessionsJson != null) {
            try {
                org.json.JSONArray(sessionsJson)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to parse sessions JSON for $bookName", e)
                null
            }
        } else null

        val sessionList = sessions?.let {
            (0 until it.length()).map { i ->
                try {
                    it.getJSONObject(i)
                } catch (e: Exception) {
                    null
                }
            }.filterNotNull()
        } ?: emptyList<Any>()

        val lastReadDate = readingTimePrefs.getString("${bookName}_last_read_date", null) ?: ""
        val firstReadDate = readingTimePrefs.getString("${bookName}_first_read_date", null) ?: ""

        Log.d(
            "MainViewModel",
            "getPhoneReadingTime($bookName): sessions=${sessionList.size}, firstReadDate=$firstReadDate, lastReadDate=$lastReadDate"
        )

        return mapOf(
            "totalSeconds" to totalSeconds,
            "sessions" to sessionList,
            "lastReadDate" to lastReadDate,
            "firstReadDate" to firstReadDate
        )
    }

    private fun savePhoneReadingTime(bookName: String, readingTime: Map<String, Any>?) {
        if (readingTime == null) {
            Log.d("MainViewModel", "savePhoneReadingTime($bookName): skipping, readingTime is null")
            return
        }

        val readingTimePrefs = getApplication<Application>().getSharedPreferences(
            "reading_time_prefs",
            Context.MODE_PRIVATE
        )
        val totalSeconds = (readingTime["totalSeconds"] as? Number)?.toLong() ?: 0L
        val lastReadDate = readingTime["lastReadDate"] as? String ?: ""
        val firstReadDate = readingTime["firstReadDate"] as? String ?: ""

        Log.d(
            "MainViewModel",
            "savePhoneReadingTime($bookName): totalSeconds=$totalSeconds, firstReadDate=$firstReadDate, lastReadDate=$lastReadDate"
        )

        val editor = readingTimePrefs.edit()
        editor.putLong("${bookName}_total_seconds", totalSeconds)
        if (lastReadDate.isNotEmpty()) {
            editor.putString("${bookName}_last_read_date", lastReadDate)
        }
        if (firstReadDate.isNotEmpty()) {
            editor.putString("${bookName}_first_read_date", firstReadDate)
        }

        val sessions = readingTime["sessions"]
        if (sessions is List<*>) {
            try {
                val sessionsArray = org.json.JSONArray()
                sessions.forEach { session ->
                    if (session is Map<*, *>) {
                        val sessionObj = org.json.JSONObject()
                        session.forEach { (key, value) ->
                            when (value) {
                                is Number -> sessionObj.put(key.toString(), value)
                                is String -> sessionObj.put(key.toString(), value)
                                is Boolean -> sessionObj.put(key.toString(), value)
                                else -> sessionObj.put(key.toString(), value.toString())
                            }
                        }
                        sessionsArray.put(sessionObj)
                    } else if (session is org.json.JSONObject) {
                        sessionsArray.put(session)
                    }
                }
                editor.putString("${bookName}_sessions", sessionsArray.toString())
                Log.d("MainViewModel", "Saved ${sessionsArray.length()} sessions for $bookName")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to save sessions for $bookName", e)
            }
        } else {
            Log.d("MainViewModel", "No sessions to save for $bookName")
        }

        editor.apply()
        Log.d("MainViewModel", "Successfully saved reading time for $bookName")
    }

    private fun mergeReadingTime(
        phoneReadingTime: Map<String, Any>?,
        bandReadingTime: Map<String, Any>?
    ): Map<String, Any>? {
        if (phoneReadingTime == null && bandReadingTime == null) return null
        if (phoneReadingTime == null) return bandReadingTime
        if (bandReadingTime == null) return phoneReadingTime

        val phoneTotalSeconds = (phoneReadingTime["totalSeconds"] as? Number)?.toLong() ?: 0L
        val bandTotalSeconds = (bandReadingTime["totalSeconds"] as? Number)?.toLong() ?: 0L

        return if (phoneTotalSeconds >= bandTotalSeconds) phoneReadingTime else bandReadingTime
    }


    private fun loadBooks() {
        viewModelScope.launch(Dispatchers.IO) {
            val bookEntities = db.bookDao().getAllBooks()

            val bookUiModels = bookEntities.map { entity ->
                val chapterCount = db.chapterDao().getChapterCountForBook(entity.id)
                val wordCount = db.chapterDao().getTotalWordCountForBook(entity.id) ?: 0

                val lastReadChapterId = readerPrefs.getInt("last_read_chapter_${entity.id}", -1)
                var lastReadInfo: String? = null
                var chapterIndex: Int? = null
                var chapterProgressPercent: Float = 0f

                if (lastReadChapterId != -1) {
                    val chapter = db.chapterDao().getChapterById(lastReadChapterId)
                    if (chapter != null) {
                        val allChapters = db.chapterDao().getChapterInfoForBook(entity.id)
                        chapterIndex = allChapters.indexOfFirst { it.id == lastReadChapterId }
                        if (chapterIndex != -1 && allChapters.isNotEmpty()) {
                            chapterProgressPercent =
                                (chapterIndex + 1).toFloat() / allChapters.size * 100f
                        }
                        lastReadInfo = "读至：${chapter.name}"
                    }
                }
                if (lastReadInfo == null && chapterCount > 0) {
                    lastReadInfo = "未读"
                }

                val lastReadTimestamp = readerPrefs.getLong("last_read_timestamp_${entity.id}", 0L)

                Book(
                    id = entity.id,
                    name = entity.name,
                    path = entity.path,
                    size = entity.size,
                    chapterCount = chapterCount,
                    wordCount = wordCount,
                    syncedChapterCount = 0,
                    coverImagePath = entity.coverImagePath,
                    localCategory = entity.localCategory,
                    lastReadInfo = lastReadInfo,
                    lastReadTimestamp = lastReadTimestamp,
                    chapterIndex = chapterIndex,
                    chapterProgressPercent = chapterProgressPercent
                )
            }

            val recentUpdatedBook = bookUiModels.maxByOrNull { book ->
                try {
                    File(book.path).lastModified()
                } catch (e: Exception) {
                    0L
                }
            }

            withContext(Dispatchers.Main) {
                _books.value = bookUiModels.sortedByDescending { it.name }

                _recentBook.value = bookUiModels.maxByOrNull { it.id }
                _recentUpdatedBook.value = recentUpdatedBook
            }
        }
    }

    fun backupData(uri: Uri) {
        viewModelScope.launch {
            val result = DataBackupManager.backupData(getApplication(), uri, db)
            _backupRestoreState.value = if (result.isSuccess) {
                BackupRestoreResult(success = true, message = "备份成功")
            } else {
                BackupRestoreResult(success = false, message = "备份失败: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun restoreData(uri: Uri) {
        viewModelScope.launch {
            val result = DataBackupManager.restoreData(getApplication(), uri, db)
            if (result.isSuccess) {
                loadBooks()
                _backupRestoreState.value = BackupRestoreResult(success = true, message = "恢复成功")
            } else {
                _backupRestoreState.value = BackupRestoreResult(success = false, message = "恢复失败: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun clearBackupRestoreState() {
        _backupRestoreState.value = null
    }

    fun loadBandSettings() {
        _globalLoadingState.value = GlobalLoadingState(isLoading = true, message = "正在加载手环设置...")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val conn = connectionHandler.getFileConnection()
                val settings = conn.getSettings(listOf(
                    "EBOOK_FONT", "EBOOK_OPACITY", "EBOOK_BOLD_ENABLED",
                    "EBOOK_VERTICAL_MARGIN", "EBOOK_TIME_FORMAT", "EBOOK_READ_MODE",
                    "EBOOK_TXTSZPAGE", "EBOOK_SHOW_PROGRESS_BAR",
                    "EBOOK_SHOW_PROGRESS_BAR_PERCENT", "EBOOK_PROGRESS_BAR_OPACITY",
                    "EBOOK_PROGRESS_BAR_HEIGHT", "EBOOK_PRELOAD_CHAPTER",
                    "EBOOK_PREVENT_PARAGRAPH_SPLITTING", "EBOOK_BRIGHTNESS",
                    "EBOOK_BRIGHTNESS_FOLLOW_SYSTEM", "EBOOK_ALWAYS_SHOW_TIME",
                    "EBOOK_ALWAYS_SHOW_BATTERY", "EBOOK_ALWAYS_SHOW_TIME_SENSITIVITY",
                    "EBOOK_CHAPTER_START_EMPTY_LINES", "EBOOK_CHAPTER_START_NUMBER",
                    "EBOOK_CHAPTER_START_NAME", "EBOOK_CHAPTER_START_WORD_COUNT",
                    "EBOOK_CHAPTER_SWITCH_STYLE", "EBOOK_CHAPTER_SWITCH_HEIGHT",
                    "EBOOK_CHAPTER_SWITCH_SENSITIVITY", "EBOOK_CHAPTER_SWITCH_SHOW_INFO",
                    "EBOOK_SWIPE_SENSITIVITY", "EBOOK_SWIPE", "EBOOK_AUTO",
                    "EBOOK_AUTO_READ_DISTANCE", "EBOOK_GESTURE",
                    "EBOOK_PROGRESS_SAVE_MODE", "EBOOK_PROGRESS_SAVE_INTERVAL",
                    "EBOOK_SHELF_MARQUEE_ENABLED", "EBOOK_BOOKMARK_MARQUEE_ENABLED",
                    "EBOOK_BOOKINFO_MARQUEE_ENABLED", "EBOOK_CHAPTER_LIST_MARQUEE_ENABLED",
                    "EBOOK_TEXT_READER_MARQUEE_ENABLED", "EBOOK_DETAIL_MARQUEE_ENABLED",
                    "EBOOK_DETAIL_PROGRESS_MARQUEE_ENABLED", "EBOOK_NOSTALGIC_PAGE_TURN_MODE",
                    "EBOOK_TEACHER_SCREEN_ENABLED"
                ))

                val autoReadData = try {
                    settings["EBOOK_AUTO"]?.let { json ->
                        val jsonObj = JSONObject(json)
                        Pair(jsonObj.optBoolean("enable", false), jsonObj.optInt("speed", 10))
                    } ?: Pair(false, 10)
                } catch (e: Exception) {
                    Pair(false, 10)
                }

                withContext(Dispatchers.Main) {
                    _bandSettingsState.value = BandSettingsState(
                        fontSize = settings["EBOOK_FONT"]?.toIntOrNull() ?: 30,
                        opacity = settings["EBOOK_OPACITY"]?.toIntOrNull() ?: 100,
                        boldEnabled = settings["EBOOK_BOLD_ENABLED"]?.let { it != "false" } ?: true,
                        verticalMargin = settings["EBOOK_VERTICAL_MARGIN"]?.toIntOrNull() ?: 10,
                        timeFormat = settings["EBOOK_TIME_FORMAT"] ?: "24h",
                        readMode = settings["EBOOK_READ_MODE"] ?: "scroll",
                        txtSizePage = settings["EBOOK_TXTSZPAGE"]?.toIntOrNull() ?: 400,
                        showProgressBar = settings["EBOOK_SHOW_PROGRESS_BAR"]?.let { it == "true" } ?: true,
                        showProgressBarPercent = settings["EBOOK_SHOW_PROGRESS_BAR_PERCENT"] == "true",
                        progressBarOpacity = settings["EBOOK_PROGRESS_BAR_OPACITY"]?.toIntOrNull() ?: 100,
                        progressBarHeight = settings["EBOOK_PROGRESS_BAR_HEIGHT"]?.toIntOrNull() ?: 8,
                        preloadChapter = settings["EBOOK_PRELOAD_CHAPTER"] == "true",
                        preventParagraphSplitting = settings["EBOOK_PREVENT_PARAGRAPH_SPLITTING"] == "true",
                        brightness = settings["EBOOK_BRIGHTNESS"]?.toIntOrNull() ?: 128,
                        brightnessFollowSystem = settings["EBOOK_BRIGHTNESS_FOLLOW_SYSTEM"]?.let { it != "false" } ?: true,
                        alwaysShowTime = settings["EBOOK_ALWAYS_SHOW_TIME"] == "true",
                        alwaysShowBattery = settings["EBOOK_ALWAYS_SHOW_BATTERY"]?.let { it != "false" } ?: true,
                        alwaysShowTimeSensitivity = settings["EBOOK_ALWAYS_SHOW_TIME_SENSITIVITY"]?.toIntOrNull() ?: 200,
                        chapterStartEmptyLines = settings["EBOOK_CHAPTER_START_EMPTY_LINES"] == "true",
                        chapterStartNumber = settings["EBOOK_CHAPTER_START_NUMBER"] == "true",
                        chapterStartName = settings["EBOOK_CHAPTER_START_NAME"] == "true",
                        chapterStartWordCount = settings["EBOOK_CHAPTER_START_WORD_COUNT"] == "true",
                        chapterSwitchStyle = settings["EBOOK_CHAPTER_SWITCH_STYLE"] ?: "button",
                        chapterSwitchHeight = settings["EBOOK_CHAPTER_SWITCH_HEIGHT"]?.toIntOrNull() ?: 80,
                        chapterSwitchSensitivity = settings["EBOOK_CHAPTER_SWITCH_SENSITIVITY"]?.toIntOrNull() ?: 50,
                        chapterSwitchShowInfo = settings["EBOOK_CHAPTER_SWITCH_SHOW_INFO"] == "true",
                        swipeSensitivity = settings["EBOOK_SWIPE_SENSITIVITY"]?.toIntOrNull() ?: 80,
                        swipe = settings["EBOOK_SWIPE"] ?: "column",
                        autoReadEnabled = autoReadData.first,
                        autoReadSpeed = autoReadData.second,
                        autoReadDistance = settings["EBOOK_AUTO_READ_DISTANCE"]?.toIntOrNull() ?: 100,
                        gesture = settings["EBOOK_GESTURE"] ?: "single",
                        progressSaveMode = settings["EBOOK_PROGRESS_SAVE_MODE"] ?: "exit",
                        progressSaveInterval = settings["EBOOK_PROGRESS_SAVE_INTERVAL"]?.toIntOrNull() ?: 10,
                        shelfMarqueeEnabled = settings["EBOOK_SHELF_MARQUEE_ENABLED"] == "true",
                        bookmarkMarqueeEnabled = settings["EBOOK_BOOKMARK_MARQUEE_ENABLED"] == "true",
                        bookinfoMarqueeEnabled = settings["EBOOK_BOOKINFO_MARQUEE_ENABLED"]?.let { it == "true" } ?: true,
                        chapterListMarqueeEnabled = settings["EBOOK_CHAPTER_LIST_MARQUEE_ENABLED"] == "true",
                        textReaderMarqueeEnabled = settings["EBOOK_TEXT_READER_MARQUEE_ENABLED"] == "true",
                        detailMarqueeEnabled = settings["EBOOK_DETAIL_MARQUEE_ENABLED"] == "true",
                        detailProgressMarqueeEnabled = settings["EBOOK_DETAIL_PROGRESS_MARQUEE_ENABLED"] == "true",
                        nostalgicPageTurnMode = settings["EBOOK_NOSTALGIC_PAGE_TURN_MODE"] ?: "topBottomClick",
                        teacherScreenEnabled = settings["EBOOK_TEACHER_SCREEN_ENABLED"] == "true"
                    )
                    _globalLoadingState.value = GlobalLoadingState(isLoading = false)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading band settings", e)
                withContext(Dispatchers.Main) {
                    _globalLoadingState.value = GlobalLoadingState(isLoading = false)
                }
            }
        }
    }

    fun updateBandSetting(key: String, value: String) {
        _bandSettingsState.value?.let { current ->
            _bandSettingsState.value = when (key) {
                "EBOOK_FONT" -> current.copy(fontSize = value.toInt())
                "EBOOK_OPACITY" -> current.copy(opacity = value.toInt())
                "EBOOK_BOLD_ENABLED" -> current.copy(boldEnabled = value.toBoolean())
                "EBOOK_VERTICAL_MARGIN" -> current.copy(verticalMargin = value.toInt())
                "EBOOK_TIME_FORMAT" -> current.copy(timeFormat = value)
                "EBOOK_READ_MODE" -> current.copy(readMode = value)
                "EBOOK_TXTSZPAGE" -> current.copy(txtSizePage = value.toInt())
                "EBOOK_SHOW_PROGRESS_BAR" -> current.copy(showProgressBar = value.toBoolean())
                "EBOOK_SHOW_PROGRESS_BAR_PERCENT" -> current.copy(showProgressBarPercent = value.toBoolean())
                "EBOOK_PROGRESS_BAR_OPACITY" -> current.copy(progressBarOpacity = value.toInt())
                "EBOOK_PRELOAD_CHAPTER" -> current.copy(preloadChapter = value.toBoolean())
                "EBOOK_PREVENT_PARAGRAPH_SPLITTING" -> current.copy(preventParagraphSplitting = value.toBoolean())
                "EBOOK_BRIGHTNESS" -> current.copy(brightness = value.toInt())
                "EBOOK_BRIGHTNESS_FOLLOW_SYSTEM" -> current.copy(brightnessFollowSystem = value.toBoolean())
                "EBOOK_ALWAYS_SHOW_TIME" -> current.copy(alwaysShowTime = value.toBoolean())
                "EBOOK_ALWAYS_SHOW_BATTERY" -> current.copy(alwaysShowBattery = value.toBoolean())
                "EBOOK_ALWAYS_SHOW_TIME_SENSITIVITY" -> current.copy(alwaysShowTimeSensitivity = value.toInt())
                "EBOOK_CHAPTER_START_EMPTY_LINES" -> current.copy(chapterStartEmptyLines = value.toBoolean())
                "EBOOK_CHAPTER_START_NUMBER" -> current.copy(chapterStartNumber = value.toBoolean())
                "EBOOK_CHAPTER_START_NAME" -> current.copy(chapterStartName = value.toBoolean())
                "EBOOK_CHAPTER_START_WORD_COUNT" -> current.copy(chapterStartWordCount = value.toBoolean())
                "EBOOK_CHAPTER_SWITCH_STYLE" -> current.copy(chapterSwitchStyle = value)
                "EBOOK_CHAPTER_SWITCH_HEIGHT" -> current.copy(chapterSwitchHeight = value.toInt())
                "EBOOK_CHAPTER_SWITCH_SENSITIVITY" -> current.copy(chapterSwitchSensitivity = value.toInt())
                "EBOOK_CHAPTER_SWITCH_SHOW_INFO" -> current.copy(chapterSwitchShowInfo = value.toBoolean())
                "EBOOK_SWIPE_SENSITIVITY" -> current.copy(swipeSensitivity = value.toInt())
                "EBOOK_SWIPE" -> current.copy(swipe = value)
                "EBOOK_AUTO_READ_DISTANCE" -> current.copy(autoReadDistance = value.toInt())
                "EBOOK_PROGRESS_BAR_HEIGHT" -> current.copy(progressBarHeight = value.toInt())
                "EBOOK_GESTURE" -> current.copy(gesture = value)
                "EBOOK_PROGRESS_SAVE_MODE" -> current.copy(progressSaveMode = value)
                "EBOOK_PROGRESS_SAVE_INTERVAL" -> current.copy(progressSaveInterval = value.toInt())
                "EBOOK_SHELF_MARQUEE_ENABLED" -> current.copy(shelfMarqueeEnabled = value.toBoolean())
                "EBOOK_BOOKMARK_MARQUEE_ENABLED" -> current.copy(bookmarkMarqueeEnabled = value.toBoolean())
                "EBOOK_BOOKINFO_MARQUEE_ENABLED" -> current.copy(bookinfoMarqueeEnabled = value.toBoolean())
                "EBOOK_CHAPTER_LIST_MARQUEE_ENABLED" -> current.copy(chapterListMarqueeEnabled = value.toBoolean())
                "EBOOK_TEXT_READER_MARQUEE_ENABLED" -> current.copy(textReaderMarqueeEnabled = value.toBoolean())
                "EBOOK_DETAIL_MARQUEE_ENABLED" -> current.copy(detailMarqueeEnabled = value.toBoolean())
                "EBOOK_DETAIL_PROGRESS_MARQUEE_ENABLED" -> current.copy(detailProgressMarqueeEnabled = value.toBoolean())
                "EBOOK_NOSTALGIC_PAGE_TURN_MODE" -> current.copy(nostalgicPageTurnMode = value)
                "EBOOK_TEACHER_SCREEN_ENABLED" -> current.copy(teacherScreenEnabled = value.toBoolean())
                else -> current
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _globalLoadingState.value = GlobalLoadingState(isLoading = true, message = "正在保存设置...")
            }
            try {
                withTimeout(5000L) {
                    val conn = connectionHandler.getFileConnection()
                    val success = conn.setSettings(mapOf(key to value))
                    if (!success) {
                        throw Exception("设备返回失败")
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error updating band setting", e)
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(getApplication(), "保存超时或失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
                loadBandSettings()
            } finally {
                withContext(Dispatchers.Main) {
                    _globalLoadingState.value = GlobalLoadingState(isLoading = false)
                }
            }
        }
    }

    fun updateAutoReadSetting(enabled: Boolean, speed: Int) {
        _bandSettingsState.value?.let { current ->
            _bandSettingsState.value = current.copy(autoReadEnabled = enabled, autoReadSpeed = speed)
        }

        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _globalLoadingState.value = GlobalLoadingState(isLoading = true, message = "正在保存设置...")
            }
            try {
                withTimeout(5000L) {
                    val conn = connectionHandler.getFileConnection()
                    val autoValue = JSONObject().apply {
                        put("enable", enabled)
                        put("speed", speed)
                    }.toString()
                    val success = conn.setSettings(mapOf("EBOOK_AUTO" to autoValue))
                    if (!success) {
                        throw Exception("设备返回失败")
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error updating auto read setting", e)
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(getApplication(), "保存超时或失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
                loadBandSettings()
            } finally {
                withContext(Dispatchers.Main) {
                    _globalLoadingState.value = GlobalLoadingState(isLoading = false)
                }
            }
        }
    }

    fun clearBandSettings() {
        _bandSettingsState.value = null
    }
}
