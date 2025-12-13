package com.bandbbs.ebook.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import android.util.Log
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
import com.bandbbs.ebook.utils.EpubParser
import com.bandbbs.ebook.utils.NvbParser
import com.bandbbs.ebook.utils.VersionChecker
import android.net.Uri
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val booksDir = File(application.filesDir, "books").apply { mkdirs() }
    private val db = AppDatabase.getDatabase(application)
    private val prefs: SharedPreferences =
        application.getSharedPreferences("ebook_prefs", Context.MODE_PRIVATE)
    private val readerPrefs: SharedPreferences =
        application.getSharedPreferences("chapter_reader_prefs", Context.MODE_PRIVATE)
    private val FIRST_SYNC_CONFIRMED_KEY = "first_sync_confirmed"

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

    private var syncReadingDataJob: Job? = null

    private val _versionIncompatibleState = MutableStateFlow<VersionIncompatibleState?>(null)
    val versionIncompatibleState = _versionIncompatibleState.asStateFlow()

    private val _updateCheckState = MutableStateFlow(UpdateCheckState())
    val updateCheckState = _updateCheckState.asStateFlow()

    private val _ipCollectionPermissionState = MutableStateFlow(IpCollectionPermissionState())
    val ipCollectionPermissionState = _ipCollectionPermissionState.asStateFlow()

    private val IP_COLLECTION_PERMISSION_KEY = "ip_collection_permission"
    private val IP_COLLECTION_PERMISSION_ASKED_KEY = "ip_collection_permission_asked"
    private val SHOW_RECENT_IMPORT_KEY = "show_recent_import"
    private val SHOW_RECENT_UPDATE_KEY = "show_recent_update"
    private val AUTO_CHECK_UPDATES_KEY = "auto_check_updates"
    private val SHOW_CONNECTION_ERROR_KEY = "show_connection_error"
    private val SHOW_SEARCH_BAR_KEY = "show_search_bar"

    private var FIRST_AUTO_CHECK = true

    
    private val _showRecentImport = MutableStateFlow(prefs.getBoolean(SHOW_RECENT_IMPORT_KEY, true))
    val showRecentImport = _showRecentImport.asStateFlow()

    private val _showRecentUpdate = MutableStateFlow(prefs.getBoolean(SHOW_RECENT_UPDATE_KEY, true))
    val showRecentUpdate = _showRecentUpdate.asStateFlow()

    private val _autoCheckUpdates = MutableStateFlow(prefs.getBoolean(AUTO_CHECK_UPDATES_KEY, true))
    val autoCheckUpdates = _autoCheckUpdates.asStateFlow()

    private val _ipCollectionAllowed = MutableStateFlow(prefs.getBoolean(IP_COLLECTION_PERMISSION_KEY, false))
    val ipCollectionAllowed = _ipCollectionAllowed.asStateFlow()

    private val _showConnectionError = MutableStateFlow(prefs.getBoolean(SHOW_CONNECTION_ERROR_KEY, true))
    val showConnectionError = _showConnectionError.asStateFlow()

    private val _showSearchBar = MutableStateFlow(prefs.getBoolean(SHOW_SEARCH_BAR_KEY, true))
    val showSearchBar = _showSearchBar.asStateFlow()

    private val connectionHandler = ConnectionHandler(
        scope = viewModelScope,
        connectionState = _connectionState,
        connectionErrorState = _connectionErrorState,
        showConnectionError = _showConnectionError,
        versionIncompatibleState = _versionIncompatibleState
    ).apply {
        onBandConnected = { deviceName ->
            if (FIRST_AUTO_CHECK) autoCheckUpdates()
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
        firstSyncConfirmedKey = FIRST_SYNC_CONFIRMED_KEY
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
        val ipCollectionAllowed = prefs.getBoolean(IP_COLLECTION_PERMISSION_KEY, false)
        val hasAskedBefore = prefs.getBoolean(IP_COLLECTION_PERMISSION_ASKED_KEY, false)

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

    fun setConnection(connection: InterHandshake) = connectionHandler.setConnection(connection)

    fun reconnect() = connectionHandler.reconnect()

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

    fun startPush(book: Book) = pushHandler.startPush(book)

    fun syncCoverOnly(book: Book) = pushHandler.syncCoverOnly(book)

    fun confirmPush(book: Book, selectedChapterIndices: Set<Int>, syncCover: Boolean = false) =
        pushHandler.confirmPush(book, selectedChapterIndices, syncCover)

    fun confirmFirstSync() = pushHandler.confirmFirstSync()

    fun cancelFirstSyncConfirm() = pushHandler.cancelFirstSyncConfirm()

    fun cancelPush() = pushHandler.cancelPush()

    fun resetPushState() = pushHandler.resetPushState()

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
                                (chapters[0].name == "简介" || chapters[0].name == "介绍")) {
                                val chapter = db.chapterDao().getChapterById(chapters[0].id)
                                if (chapter != null) {
                                    val content = ChapterContentManager.readChapterContent(chapter.contentFilePath)
                                    val parsedInfo = BookInfoParser.parseIntroductionContent(content)
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
                                Log.d("MainViewModel", "Book info updated on watch: ${updatedEntity.name}")
                            } catch (e: Exception) {
                                Log.e("MainViewModel", "Failed to update book info on watch", e)
                            }
                        }


                        withContext(Dispatchers.Main) {
                            _editBookInfoState.value = EditBookInfoState(updatedEntity, isResyncing = false)
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

        
        Log.d("MainViewModel", "Showing sync mode selection dialog")
        _syncReadingDataState.value = _syncReadingDataState.value.copy(showModeDialog = true)
    }

    fun setSyncModeAndStart(mode: SyncMode) {
        setSyncModesAndStart(mode, mode)
    }

    fun setSyncModesAndStart(progressMode: SyncMode, readingTimeMode: SyncMode) {
        Log.d("MainViewModel", "setSyncModesAndStart() called with progressMode: $progressMode, readingTimeMode: $readingTimeMode")
        syncReadingDataJob?.cancel()

        syncReadingDataJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                
                withContext(Dispatchers.Main) {
                    _syncReadingDataState.value = _syncReadingDataState.value.copy(
                        showModeDialog = false,
                        progressSyncMode = progressMode,
                        readingTimeSyncMode = readingTimeMode
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
                    _syncReadingDataState.value = SyncReadingDataState(
                        isSyncing = true,
                        statusText = "开始同步阅读数据...",
                        progress = 0f,
                        totalBooks = allBooks.size,
                        syncedBooks = 0
                    )
                }

                val fileConn = connectionHandler.getFileConnection()
                var syncedCount = 0

                for ((index, book) in allBooks.withIndex()) {

                    if (!isActive) {
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

                    try {
                        Log.d("MainViewModel", "Syncing reading data for book: ${book.name}")

                        val bandReadingData: com.bandbbs.ebook.logic.ReadingDataResult? = try {
                            val data = fileConn.getReadingData(book.name)
                            Log.d("MainViewModel", "Got reading data from band for ${book.name}: progress=${data.progress != null}, readingTime=${data.readingTime != null}")
                            data
                        } catch (e: Exception) {
                            Log.e("MainViewModel", "Failed to get reading data from band for ${book.name}", e)
                            null
                        }

                        
                        val phoneProgress = getPhoneReadingProgress(book)
                        Log.d("MainViewModel", "Phone progress for ${book.name}: ${phoneProgress != null}")
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
                                        Log.d("MainViewModel", "Band progress has no valid chapterIndex, ignoring")
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("MainViewModel", "Failed to parse band reading data", e)
                            }
                        }

                        
                        val phoneReadingTime = getPhoneReadingTime(book.name)
                        Log.d("MainViewModel", "Phone reading time for ${book.name}: ${if (phoneReadingTime != null) "exists (totalSeconds=${phoneReadingTime["totalSeconds"]})" else "null"}")
                        
                        var bandReadingTime: Map<String, Any>? = null
                        if (bandReadingData != null && bandReadingData.readingTime != null) {
                            try {
                                Log.d("MainViewModel", "Parsing band reading time for ${book.name}")
                                val readingTimeMap = org.json.JSONObject(bandReadingData.readingTime)
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
                                val sessionsSize = when (val sessions = bandReadingTime["sessions"]) {
                                    is List<*> -> sessions.size
                                    else -> 0
                                }
                                Log.d("MainViewModel", "Parsed band reading time for ${book.name}: totalSeconds=${bandReadingTime["totalSeconds"]}, sessions=$sessionsSize")
                            } catch (e: Exception) {
                                Log.e("MainViewModel", "Failed to parse band reading time for ${book.name}", e)
                            }
                        } else {
                            Log.d("MainViewModel", "No band reading time data for ${book.name}")
                        }

                        
                        val progressMode = _syncReadingDataState.value.progressSyncMode
                        val readingTimeMode = _syncReadingDataState.value.readingTimeSyncMode
                        Log.d("MainViewModel", "Merging data for ${book.name} with progressMode: $progressMode, readingTimeMode: $readingTimeMode")
                        
                        val finalProgress = when (progressMode) {
                            SyncMode.AUTO -> {
                                val merged = mergeProgress(phoneProgress, bandProgress)
                                Log.d("MainViewModel", "Auto merged progress for ${book.name}: ${merged != null}")
                                merged
                            }
                            SyncMode.BAND_ONLY -> {
                                Log.d("MainViewModel", "Using band progress for ${book.name}")
                                bandProgress ?: phoneProgress
                            }
                            SyncMode.PHONE_ONLY -> {
                                Log.d("MainViewModel", "Using phone progress for ${book.name}")
                                phoneProgress ?: bandProgress
                            }
                        }
                        savePhoneReadingProgress(book, finalProgress)

                        val finalReadingTime = when (readingTimeMode) {
                            SyncMode.AUTO -> {
                                val merged = mergeReadingTime(phoneReadingTime, bandReadingTime)
                                Log.d("MainViewModel", "Auto merged reading time for ${book.name}: ${if (merged != null) "totalSeconds=${merged["totalSeconds"]}, sessions=${(merged["sessions"] as? List<*>)?.size ?: 0}" else "null"}")
                                merged
                            }
                            SyncMode.BAND_ONLY -> {
                                Log.d("MainViewModel", "Using band reading time for ${book.name}")
                                bandReadingTime ?: phoneReadingTime
                            }
                            SyncMode.PHONE_ONLY -> {
                                Log.d("MainViewModel", "Using phone reading time for ${book.name}")
                                phoneReadingTime ?: bandReadingTime
                            }
                        }
                        savePhoneReadingTime(book.name, finalReadingTime)
                        Log.d("MainViewModel", "Saved reading time for ${book.name}")

                        
                        val progressJson = finalProgress?.let {
                            try {
                                org.json.JSONObject(it).toString()
                            } catch (e: Exception) {
                                Log.e("MainViewModel", "Failed to serialize progress", e)
                                null
                            }
                        }

                        val readingTimeJson = finalReadingTime?.let {
                            try {
                                val json = org.json.JSONObject(it).toString()
                                Log.d("MainViewModel", "Serialized reading time JSON for ${book.name}: ${json.length} chars")
                                json
                            } catch (e: Exception) {
                                Log.e("MainViewModel", "Failed to serialize reading time for ${book.name}", e)
                                null
                            }
                        }

                        if (progressJson != null || readingTimeJson != null) {
                            Log.d("MainViewModel", "Sending reading data to band for ${book.name}: progress=${progressJson != null}, readingTime=${readingTimeJson != null}")
                            fileConn.setReadingData(book.name, progressJson, readingTimeJson)
                            Log.d("MainViewModel", "Successfully sent reading data to band for ${book.name}")
                        } else {
                            Log.d("MainViewModel", "No reading data to send to band for ${book.name}")
                        }

                        syncedCount++
                        withContext(Dispatchers.Main) {
                            _syncReadingDataState.value = _syncReadingDataState.value.copy(
                                syncedBooks = syncedCount
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("MainViewModel", "Failed to sync reading data for ${book.name}", e)
                    }
                }


                loadBooks()

                withContext(Dispatchers.Main) {
                    _syncReadingDataState.value = SyncReadingDataState(
                        isSyncing = false,
                        statusText = "同步完成，共同步 $syncedCount 本书",
                        progress = 1f,
                        totalBooks = allBooks.size,
                        syncedBooks = syncedCount
                    )
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

    fun dismissSyncModeDialog() {
        _syncReadingDataState.value = _syncReadingDataState.value.copy(showModeDialog = false)
    }

    fun dismissVersionIncompatible() {
        _versionIncompatibleState.value = null
    }

    fun clearAllReadingTimeData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val readingTimePrefs = getApplication<Application>().getSharedPreferences("reading_time_prefs", Context.MODE_PRIVATE)
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


        val phoneChapterIndex = (phoneProgress["chapterIndex"] as? Number)?.toInt()
        val bandChapterIndex = (bandProgress["chapterIndex"] as? Number)?.toInt()


        if (phoneChapterIndex == null || phoneChapterIndex < 0) {
            if (bandChapterIndex != null && bandChapterIndex >= 0) {
                Log.d("MainViewModel", "Using band progress: phone chapterIndex invalid")
                return bandProgress
            }

            Log.d("MainViewModel", "Both progress invalid, keeping phone progress")
            return phoneProgress
        }
        if (bandChapterIndex == null || bandChapterIndex < 0) {
            Log.d("MainViewModel", "Using phone progress: band chapterIndex invalid")
            return phoneProgress
        }

        val phoneTimestamp = (phoneProgress["lastReadTimestamp"] as? Number)?.toLong() ?: 0L
        val bandTimestamp = (bandProgress["lastReadTimestamp"] as? Number)?.toLong() ?: 0L


        if (bandChapterIndex < phoneChapterIndex && phoneChapterIndex > 0) {
            val timeDiff = Math.abs(phoneTimestamp - bandTimestamp)

            if (timeDiff < 3600000L && bandChapterIndex == 0) {
                Log.d("MainViewModel", "Band progress seems reset (chapterIndex=0), using phone progress (chapterIndex=$phoneChapterIndex)")
                return phoneProgress
            }
        }


        if (phoneTimestamp == 0L && bandTimestamp == 0L) {
            val result = if (phoneChapterIndex >= bandChapterIndex) phoneProgress else bandProgress
            Log.d("MainViewModel", "Both timestamps 0, using progress with larger chapterIndex: ${if (phoneChapterIndex >= bandChapterIndex) phoneChapterIndex else bandChapterIndex}")
            return result
        }


        if (phoneTimestamp == 0L) {
            Log.d("MainViewModel", "Phone timestamp 0, using band progress (chapterIndex=$bandChapterIndex, timestamp=$bandTimestamp)")
            return bandProgress
        }
        if (bandTimestamp == 0L) {
            Log.d("MainViewModel", "Band timestamp 0, using phone progress (chapterIndex=$phoneChapterIndex, timestamp=$phoneTimestamp)")
            return phoneProgress
        }


        val result = if (phoneTimestamp >= bandTimestamp) phoneProgress else bandProgress
        val resultChapterIndex = if (phoneTimestamp >= bandTimestamp) phoneChapterIndex else bandChapterIndex
        Log.d("MainViewModel", "Both have timestamps, using newer: chapterIndex=$resultChapterIndex, phoneTs=$phoneTimestamp, bandTs=$bandTimestamp")
        return result
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
                        .putLong("last_read_timestamp_${book.id}", if (timestamp > 0L) timestamp else System.currentTimeMillis())
                        .apply()
                    Log.d("MainViewModel", "Saved progress for ${book.name}: chapterIndex=$chapterIndex, chapterId=$chapterId, offset=$offset, timestamp=$timestamp")
                } else {
                    Log.w("MainViewModel", "Skipping save progress for ${book.name}: invalid data (chapterIndex=$chapterIndex, offset=$offset, timestamp=$timestamp)")
                }
            } else {
                Log.w("MainViewModel", "Invalid chapterIndex $chapterIndex for ${book.name} (total chapters: ${allChapters.size})")
            }
        } else {
            Log.w("MainViewModel", "No valid chapterIndex in progress for ${book.name}")
        }
    }

    private fun getPhoneReadingTime(bookName: String): Map<String, Any>? {
        val readingTimePrefs = getApplication<Application>().getSharedPreferences("reading_time_prefs", Context.MODE_PRIVATE)
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
        
        Log.d("MainViewModel", "getPhoneReadingTime($bookName): sessions=${sessionList.size}, firstReadDate=$firstReadDate, lastReadDate=$lastReadDate")

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

        val readingTimePrefs = getApplication<Application>().getSharedPreferences("reading_time_prefs", Context.MODE_PRIVATE)
        val totalSeconds = (readingTime["totalSeconds"] as? Number)?.toLong() ?: 0L
        val lastReadDate = readingTime["lastReadDate"] as? String ?: ""
        val firstReadDate = readingTime["firstReadDate"] as? String ?: ""
        
        Log.d("MainViewModel", "savePhoneReadingTime($bookName): totalSeconds=$totalSeconds, firstReadDate=$firstReadDate, lastReadDate=$lastReadDate")

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
        Log.d("MainViewModel", "mergeReadingTime(): phone=${phoneReadingTime != null}, band=${bandReadingTime != null}")
        
        if (phoneReadingTime == null && bandReadingTime == null) {
            Log.d("MainViewModel", "mergeReadingTime(): both null, returning null")
            return null
        }
        if (phoneReadingTime == null) {
            Log.d("MainViewModel", "mergeReadingTime(): phone null, using band")
            return bandReadingTime
        }
        if (bandReadingTime == null) {
            Log.d("MainViewModel", "mergeReadingTime(): band null, using phone")
            return phoneReadingTime
        }

        val phoneTotalSeconds = (phoneReadingTime["totalSeconds"] as? Number)?.toLong() ?: 0L
        val bandTotalSeconds = (bandReadingTime["totalSeconds"] as? Number)?.toLong() ?: 0L

        Log.d("MainViewModel", "mergeReadingTime(): phoneTotalSeconds=$phoneTotalSeconds, bandTotalSeconds=$bandTotalSeconds")
        
        
        val usePhone = phoneTotalSeconds >= bandTotalSeconds
        val selectedReadingTime = if (usePhone) phoneReadingTime else bandReadingTime
        
        val selectedTotalSeconds = if (usePhone) phoneTotalSeconds else bandTotalSeconds
        val selectedSessions = if (usePhone) {
            (phoneReadingTime["sessions"] as? List<Any>) ?: emptyList<Any>()
        } else {
            (bandReadingTime["sessions"] as? List<Any>) ?: emptyList<Any>()
        }
        
        Log.d("MainViewModel", "mergeReadingTime(): using ${if (usePhone) "phone" else "band"} reading time (totalSeconds=$selectedTotalSeconds, sessions=${selectedSessions.size})")

        
        val finalSessions = if (selectedSessions.size > 100) {
            Log.d("MainViewModel", "mergeReadingTime(): limiting sessions from ${selectedSessions.size} to 100")
            selectedSessions.takeLast(100)
        } else {
            selectedSessions
        }
        
        Log.d("MainViewModel", "mergeReadingTime(): finalSessions=${finalSessions.size}")

        
        val phoneFirstDate = phoneReadingTime["firstReadDate"] as? String ?: ""
        val phoneLastDate = phoneReadingTime["lastReadDate"] as? String ?: ""
        val bandFirstDate = bandReadingTime["firstReadDate"] as? String ?: ""
        val bandLastDate = bandReadingTime["lastReadDate"] as? String ?: ""

        val firstReadDate = if (usePhone) {
            if (phoneFirstDate.isEmpty()) bandFirstDate else phoneFirstDate
        } else {
            if (bandFirstDate.isEmpty()) phoneFirstDate else bandFirstDate
        }

        val lastReadDate = if (usePhone) {
            if (phoneLastDate.isEmpty()) bandLastDate else phoneLastDate
        } else {
            if (bandLastDate.isEmpty()) phoneLastDate else bandLastDate
        }

        val result = mapOf(
            "totalSeconds" to selectedTotalSeconds,
            "sessions" to finalSessions,
            "firstReadDate" to firstReadDate,
            "lastReadDate" to lastReadDate
        )
        
        Log.d("MainViewModel", "mergeReadingTime(): result - totalSeconds=$selectedTotalSeconds, sessions=${finalSessions.size}, firstReadDate=$firstReadDate, lastReadDate=$lastReadDate")
        
        return result
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
                            chapterProgressPercent = (chapterIndex + 1).toFloat() / allChapters.size * 100f
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
}
