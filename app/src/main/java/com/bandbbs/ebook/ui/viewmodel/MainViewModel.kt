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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    private val connectionHandler = ConnectionHandler(
        scope = viewModelScope,
        connectionState = _connectionState,
        connectionErrorState = _connectionErrorState
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

    private fun loadBooks() {
        viewModelScope.launch(Dispatchers.IO) {
            val bookEntities = db.bookDao().getAllBooks()
            val bookUiModels = bookEntities.map { entity ->
                val chapterCount = db.chapterDao().getChapterCountForBook(entity.id)
                val wordCount = db.chapterDao().getTotalWordCountForBook(entity.id) ?: 0


                val lastReadChapterId = readerPrefs.getInt("last_read_chapter_${entity.id}", -1)
                var lastReadInfo: String? = null
                if (lastReadChapterId != -1) {
                    val chapter = db.chapterDao().getChapterById(lastReadChapterId)
                    if (chapter != null) {



                        lastReadInfo = "读至：${chapter.name}"
                    }
                }
                if (lastReadInfo == null && chapterCount > 0) {
                    lastReadInfo = "未读"
                }

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
                    lastReadInfo = lastReadInfo
                )
            }
            withContext(Dispatchers.Main) {
                _books.value = bookUiModels.sortedByDescending { it.name }

                _recentBook.value = bookUiModels.maxByOrNull { it.id }
            }
        }
    }
}
