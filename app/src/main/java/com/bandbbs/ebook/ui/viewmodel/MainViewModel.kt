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

    private val connectionHandler = ConnectionHandler(
        scope = viewModelScope,
        connectionState = _connectionState,
        connectionErrorState = _connectionErrorState,
        versionIncompatibleState = _versionIncompatibleState
    )

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

    fun importCoverForBook(uri: android.net.Uri) = libraryHandler.importCoverForBook(uri)

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
        if (!connectionHandler.isConnected()) {
            _syncReadingDataState.value = SyncReadingDataState(
                isSyncing = false,
                statusText = "手环未连接",
                progress = 0f
            )
            return
        }

        
        syncReadingDataJob?.cancel()
        
        syncReadingDataJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val allBooks = _books.value
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
                        
                        val bandReadingData: com.bandbbs.ebook.logic.ReadingDataResult? = try {
                            fileConn.getReadingData(book.name)
                        } catch (e: Exception) {
                            Log.e("MainViewModel", "Failed to get reading data from band for ${book.name}", e)
                            null
                        }

                        
                        val phoneProgress = getPhoneReadingProgress(book)
                        val phoneReadingTime = getPhoneReadingTime(book.name)

                        
                        var bandProgress: Map<String, Any>? = null
                        var bandReadingTime: Map<String, Any>? = null
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
                                if (bandReadingData.readingTime != null) {
                                    val readingTimeMap = org.json.JSONObject(bandReadingData.readingTime)
                                    val tempMap = mutableMapOf<String, Any>()
                                    val keys = readingTimeMap.keys()
                                    while (keys.hasNext()) {
                                        val key = keys.next()
                                        val value = readingTimeMap.get(key)
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
                                    bandReadingTime = tempMap
                                }
                            } catch (e: Exception) {
                                Log.e("MainViewModel", "Failed to parse band reading data", e)
                            }
                        }

                        
                        val mergedProgress = mergeProgress(phoneProgress, bandProgress)
                        val mergedReadingTime = mergeReadingTime(phoneReadingTime, bandReadingTime)

                        
                        
                        savePhoneReadingProgress(book, mergedProgress)
                        savePhoneReadingTime(book.name, mergedReadingTime)
                        
                        
                        if (mergedReadingTime != null) {
                            val totalSeconds = (mergedReadingTime["totalSeconds"] as? Number)?.toLong() ?: 0L
                            if (totalSeconds > 0L) {
                                Log.d("MainViewModel", "Saved reading time for ${book.name}: $totalSeconds seconds")
                            }
                        }

                        
                        if (mergedProgress != null || mergedReadingTime != null) {
                            val progressJson = mergedProgress?.let { 
                                try {
                                    org.json.JSONObject(it).toString()
                                } catch (e: Exception) {
                                    Log.e("MainViewModel", "Failed to serialize progress", e)
                                    null
                                }
                            }
                            val readingTimeJson = mergedReadingTime?.let {
                                try {
                                    val jsonObj = org.json.JSONObject()
                                    it.forEach { (key, value) ->
                                        when (value) {
                                            is org.json.JSONArray -> jsonObj.put(key, value)
                                            is org.json.JSONObject -> jsonObj.put(key, value)
                                            is Number -> jsonObj.put(key, value)
                                            is Boolean -> jsonObj.put(key, value)
                                            is String -> jsonObj.put(key, value)
                                            else -> jsonObj.put(key, value.toString())
                                        }
                                    }
                                    jsonObj.toString()
                                } catch (e: Exception) {
                                    Log.e("MainViewModel", "Failed to serialize reading time", e)
                                    null
                                }
                            }
                            fileConn.setReadingData(book.name, progressJson, readingTimeJson)
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

    fun dismissVersionIncompatible() {
        _versionIncompatibleState.value = null
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

    private fun getPhoneReadingTime(bookName: String): Map<String, Any>? {
        val readingTimePrefs = getApplication<Application>().getSharedPreferences("reading_time_prefs", Context.MODE_PRIVATE)
        val totalSeconds = readingTimePrefs.getLong("${bookName}_total_seconds", 0L)
        if (totalSeconds == 0L) return null

        val sessionsJson = readingTimePrefs.getString("${bookName}_sessions", null)
        val sessions = if (sessionsJson != null) {
            try {
                org.json.JSONArray(sessionsJson)
            } catch (e: Exception) {
                org.json.JSONArray()
            }
        } else {
            org.json.JSONArray()
        }

        val result = mutableMapOf<String, Any>(
            "totalSeconds" to totalSeconds,
            "lastReadDate" to (readingTimePrefs.getString("${bookName}_last_read_date", null) ?: ""),
            "firstReadDate" to (readingTimePrefs.getString("${bookName}_first_read_date", null) ?: "")
        )
        
        if (sessions.length() > 0) {
            result["sessions"] = sessions
        }
        
        return result
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

    private fun mergeReadingTime(
        phoneReadingTime: Map<String, Any>?,
        bandReadingTime: Map<String, Any>?
    ): Map<String, Any>? {
        if (phoneReadingTime == null && bandReadingTime == null) return null
        if (phoneReadingTime == null) return bandReadingTime
        if (bandReadingTime == null) return phoneReadingTime

        val phoneTotal = (phoneReadingTime["totalSeconds"] as? Number)?.toLong() ?: 0L
        val bandTotal = (bandReadingTime["totalSeconds"] as? Number)?.toLong() ?: 0L

        
        val mergedTotal = maxOf(phoneTotal, bandTotal)

        val phoneLastDate = phoneReadingTime["lastReadDate"] as? String ?: ""
        val bandLastDate = bandReadingTime["lastReadDate"] as? String ?: ""

        
        val phoneSessions = try {
            when (val sessions = phoneReadingTime["sessions"]) {
                is org.json.JSONArray -> sessions
                is String -> org.json.JSONArray(sessions)
                else -> org.json.JSONArray()
            }
        } catch (e: Exception) {
            org.json.JSONArray()
        }
        
        val bandSessions = try {
            when (val sessions = bandReadingTime["sessions"]) {
                is org.json.JSONArray -> sessions
                is String -> org.json.JSONArray(sessions)
                else -> org.json.JSONArray()
            }
        } catch (e: Exception) {
            org.json.JSONArray()
        }

        
        val mergedSessionsMap = mutableMapOf<Long, org.json.JSONObject>()
        
        
        for (i in 0 until phoneSessions.length()) {
            try {
                val session = phoneSessions.getJSONObject(i)
                val startTime = session.getLong("startTime")
                mergedSessionsMap[startTime] = session
            } catch (e: Exception) {
                
            }
        }
        
        
        for (i in 0 until bandSessions.length()) {
            try {
                val session = bandSessions.getJSONObject(i)
                val startTime = session.getLong("startTime")
                val sessionDuration = (session.optLong("duration", 0L))
                val existingSession = mergedSessionsMap[startTime]
                val existingDuration = existingSession?.optLong("duration", 0L) ?: 0L
                
                
                if (!mergedSessionsMap.containsKey(startTime) || sessionDuration > existingDuration) {
                    mergedSessionsMap[startTime] = session
                }
            } catch (e: Exception) {
                
            }
        }
        
        
        val mergedSessions = org.json.JSONArray()
        mergedSessionsMap.values.sortedBy { it.getLong("startTime") }.forEach { session ->
            mergedSessions.put(session)
        }
        
        
        val finalSessions = org.json.JSONArray()
        val startIndex = maxOf(0, mergedSessions.length() - 100)
        for (i in startIndex until mergedSessions.length()) {
            finalSessions.put(mergedSessions.get(i))
        }

        val result = mutableMapOf<String, Any>(
            "totalSeconds" to mergedTotal,
            "lastReadDate" to if (phoneLastDate > bandLastDate) phoneLastDate else bandLastDate,
            "firstReadDate" to (phoneReadingTime["firstReadDate"] as? String ?: bandReadingTime["firstReadDate"] as? String ?: "")
        )
        
        if (finalSessions.length() > 0) {
            result["sessions"] = finalSessions
        }
        
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

    private fun savePhoneReadingTime(bookName: String, readingTime: Map<String, Any>?) {
        if (readingTime == null) return

        val readingTimePrefs = getApplication<Application>().getSharedPreferences("reading_time_prefs", Context.MODE_PRIVATE)
        val totalSeconds = (readingTime["totalSeconds"] as? Number)?.toLong() ?: 0L
        val lastReadDate = readingTime["lastReadDate"] as? String ?: ""
        val firstReadDate = readingTime["firstReadDate"] as? String ?: ""

        val editor = readingTimePrefs.edit()
            .putLong("${bookName}_total_seconds", totalSeconds)
            .putString("${bookName}_last_read_date", lastReadDate)
            .putString("${bookName}_first_read_date", firstReadDate)
        
        
        val sessions = readingTime["sessions"]
        if (sessions != null) {
            try {
                val sessionsJson = when (sessions) {
                    is org.json.JSONArray -> sessions.toString()
                    is String -> sessions
                    else -> null
                }
                if (sessionsJson != null) {
                    editor.putString("${bookName}_sessions", sessionsJson)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to save sessions for $bookName", e)
            }
        }
        
        
        editor.commit()
    }

    private fun loadBooks() {
        viewModelScope.launch(Dispatchers.IO) {
            val bookEntities = db.bookDao().getAllBooks()
            val readingTimePrefs = getApplication<Application>().getSharedPreferences("reading_time_prefs", Context.MODE_PRIVATE)
            
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

                val readingTimeSeconds = readingTimePrefs.getLong("${entity.name}_total_seconds", 0L)
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
                    readingTimeSeconds = readingTimeSeconds,
                    lastReadTimestamp = lastReadTimestamp,
                    chapterIndex = chapterIndex,
                    chapterProgressPercent = chapterProgressPercent
                )
            }
            withContext(Dispatchers.Main) {
                _books.value = bookUiModels.sortedByDescending { it.name }

                _recentBook.value = bookUiModels.maxByOrNull { it.id }
            }
        }
    }
}
