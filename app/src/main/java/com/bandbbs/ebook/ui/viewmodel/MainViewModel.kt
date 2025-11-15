package com.bandbbs.ebook.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bandbbs.ebook.database.AppDatabase
import com.bandbbs.ebook.database.BookEntity
import com.bandbbs.ebook.database.Chapter
import com.bandbbs.ebook.database.ChapterInfo
import com.bandbbs.ebook.logic.InterHandshake
import com.bandbbs.ebook.logic.InterconnetFile
import com.bandbbs.ebook.ui.model.Book
import com.bandbbs.ebook.utils.ChapterSplitter
import com.bandbbs.ebook.utils.EpubParser
import com.bandbbs.ebook.utils.NvbParser
import com.bandbbs.ebook.utils.UritoFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import java.io.File
import kotlin.math.min

data class ConnectionState(
    val statusText: String = "手环连接中",
    val descriptionText: String = "请确保小米运动健康后台运行",
    val isConnected: Boolean = false
)

data class ConnectionErrorState(
    val deviceName: String? = null,
    val isUnsupportedDevice: Boolean = false
)

data class PushState(
    val book: Book? = null,
    val progress: Double = 0.0,
    val preview: String = "...",
    val transferLog: List<String> = emptyList(),
    val speed: String = "0 B/s",
    val statusText: String = "等待中...",
    val isFinished: Boolean = false,
    val isSuccess: Boolean = false,
    val isSendingCover: Boolean = false,
    val coverProgress: String = "",
    val isTransferring: Boolean = false
)

data class ImportState(
    val uri: Uri,
    val bookName: String,
    val fileSize: Long,
    val splitMethod: String = ChapterSplitter.METHOD_DEFAULT,
    val noSplit: Boolean = false,
    val fileFormat: String = "txt",
    val wordsPerChapter: Int = 5000,
    val selectedCategory: String? = null,
    val enableChapterMerge: Boolean = false,
    val mergeMinWords: Int = 500,
    val enableChapterRename: Boolean = false,
    val renamePattern: String = ""
)

data class ImportingState(
    val bookName: String,
    val statusText: String = "正在准备",
    val progress: Float = 0f
)

data class ImportReportState(
    val bookName: String,
    val mergedChaptersInfo: String
)

data class SyncOptionsState(
    val book: Book,
    val totalChapters: Int,
    val syncedChapters: Int,
    val chapters: List<ChapterInfo> = emptyList(),
    val hasCover: Boolean = false,
    val isCoverSynced: Boolean = false
)

data class OverwriteConfirmState(
    val existingBook: Book,
    val uri: Uri,
    val newBookName: String,
    val splitMethod: String,
    val noSplit: Boolean,
    val wordsPerChapter: Int
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private lateinit var conn: InterHandshake
    private lateinit var fileConn: InterconnetFile

    private val booksDir = File(application.filesDir, "books").apply { mkdirs() }
    private val db = AppDatabase.getDatabase(application)

    private val _connectionState = MutableStateFlow(ConnectionState())
    val connectionState = _connectionState.asStateFlow()

    private val _books = MutableStateFlow<List<Book>>(emptyList())
    val books = _books.asStateFlow()

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

    private val _chapterToPreview = MutableStateFlow<com.bandbbs.ebook.ui.model.ChapterWithContent?>(null)
    val chapterToPreview = _chapterToPreview.asStateFlow()

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

    private val prefs: SharedPreferences = application.getSharedPreferences("ebook_prefs", Context.MODE_PRIVATE)
    private val CATEGORIES_KEY = "book_categories"
    private val FIRST_SYNC_CONFIRMED_KEY = "first_sync_confirmed"

    data class CategoryState(
        val categories: List<String>,
        val selectedCategory: String?,
        val book: Book?
    )

    init {
        loadBooks()
    }

    fun getCategories(): List<String> {
        return try {
            val set = prefs.getStringSet(CATEGORIES_KEY, null)
            if (set != null) {
                
                set.map { it.toString() }.sorted()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            
            try {
                prefs.edit().remove(CATEGORIES_KEY).apply()
            } catch (e2: Exception) {
                
            }
            emptyList()
        }
    }

    fun showCategorySelector(book: Book? = null) {
        val categories = getCategories()
        val selectedCategory = book?.localCategory ?: _importState.value?.selectedCategory
        _categoryState.value = CategoryState(
            categories = categories,
            selectedCategory = selectedCategory,
            book = book
        )
    }

    fun createCategory(categoryName: String) {
        try {
            val currentSet = prefs.getStringSet(CATEGORIES_KEY, null)?.toMutableSet() ?: mutableSetOf()
            currentSet.add(categoryName)
            prefs.edit().putStringSet(CATEGORIES_KEY, HashSet(currentSet)).apply()
            _categoryState.value?.let { state ->
                _categoryState.value = state.copy(categories = currentSet.toList().sorted())
            }
        } catch (e: Exception) {
            
            try {
                prefs.edit().remove(CATEGORIES_KEY).apply()
                val newSet = hashSetOf(categoryName)
                prefs.edit().putStringSet(CATEGORIES_KEY, newSet).apply()
                _categoryState.value?.let { state ->
                    _categoryState.value = state.copy(categories = listOf(categoryName))
                }
            } catch (e2: Exception) {
                
            }
        }
    }

    fun deleteCategory(categoryName: String) {
        try {
            val currentSet = prefs.getStringSet(CATEGORIES_KEY, null)?.toMutableSet() ?: mutableSetOf()
            currentSet.remove(categoryName)
            prefs.edit().putStringSet(CATEGORIES_KEY, HashSet(currentSet)).apply()
            
            
            viewModelScope.launch(Dispatchers.IO) {
                val books = db.bookDao().getAllBooks()
                books.forEach { bookEntity ->
                    if (bookEntity.localCategory == categoryName) {
                        db.bookDao().update(bookEntity.copy(localCategory = null))
                    }
                }
                withContext(Dispatchers.Main) {
                    loadBooks()
                }
            }
            
            _categoryState.value?.let { state ->
                _categoryState.value = state.copy(categories = currentSet.toList().sorted())
            }
        } catch (e: Exception) {
            
            try {
                prefs.edit().remove(CATEGORIES_KEY).apply()
                _categoryState.value?.let { state ->
                    _categoryState.value = state.copy(categories = emptyList())
                }
            } catch (e2: Exception) {
                
            }
        }
    }

    fun selectCategory(category: String?) {
        _categoryState.value?.let { state ->
            val book = state.book
            if (book != null) {
                
                viewModelScope.launch(Dispatchers.IO) {
                    val bookEntity = db.bookDao().getBookByPath(book.path)
                    if (bookEntity != null) {
                        db.bookDao().update(bookEntity.copy(localCategory = category))
                        withContext(Dispatchers.Main) {
                            loadBooks()
                        }
                    }
                }
            } else {
                
                _importState.value?.let { importState ->
                    _importState.value = importState.copy(selectedCategory = category)
                }
            }
            _categoryState.value = null
        }
    }

    fun dismissCategorySelector() {
        _categoryState.value = null
    }

    fun setConnection(connection: InterHandshake) {
        this.conn = connection
        this.fileConn = InterconnetFile(conn)
        reconnect()
    }

    fun reconnect() {
        viewModelScope.launch {
            _connectionState.update {
                it.copy(
                    statusText = "手环连接中",
                    descriptionText = "请确保小米运动健康后台运行",
                    isConnected = false
                )
            }
            try {
                withTimeout(3000L) {
                    conn.destroy().await()
                    val deviceName = conn.connect().await().replace(" ", "")

                    val unsupportedDevices = listOf("小米手环8", "小米手环9")
                    val isUnsupported = unsupportedDevices.any { deviceName.equals(it) }
                    
                    if (isUnsupported) {
                        _connectionState.update {
                            it.copy(
                                statusText = "设备不受支持",
                                descriptionText = "$deviceName 不受支持",
                                isConnected = false
                            )
                        }
                        // 延迟设置错误状态，确保之前的sheet已经完全隐藏
                        delay(300)
                        _connectionErrorState.value = ConnectionErrorState(
                            deviceName = deviceName,
                            isUnsupportedDevice = true
                        )
                        return@withTimeout
                    }
                    
                    conn.auth().await()
                    try {
                        if (!conn.getAppState().await()) {
                            _connectionState.update {
                                it.copy(
                                    statusText = "弦电子书未安装",
                                    descriptionText = "请在手环上安装小程序",
                                    isConnected = false
                                )
                            }
                            // 延迟设置错误状态，确保之前的sheet已经完全隐藏
                            delay(300)
                            _connectionErrorState.value = ConnectionErrorState(
                                deviceName = deviceName,
                                isUnsupportedDevice = false
                            )
                            return@withTimeout
                        }
                    } catch (e: Exception) {
                        _connectionState.update {
                            it.copy(
                                statusText = "弦电子书未安装",
                                descriptionText = "请在手环上安装小程序",
                                isConnected = false
                            )
                        }
                        // 延迟设置错误状态，确保之前的sheet已经完全隐藏
                        delay(300)
                        _connectionErrorState.value = ConnectionErrorState(
                            deviceName = deviceName,
                            isUnsupportedDevice = false
                        )
                        return@withTimeout
                    }
                    conn.openApp().await()
                    conn.registerListener().await()
                    _connectionState.update {
                        it.copy(
                            statusText = "设备连接成功",
                            descriptionText = "$deviceName 已连接",
                            isConnected = true
                        )
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e("MainViewModel", "connect timeout")
                _connectionState.update {
                    it.copy(
                        statusText = "手环连接失败",
                        descriptionText = "连接超时",
                        isConnected = false
                    )
                }
                // 延迟设置错误状态，确保之前的sheet已经完全隐藏
                delay(300)
                _connectionErrorState.value = ConnectionErrorState(
                    deviceName = null,
                    isUnsupportedDevice = false
                )
            } catch (e: Exception) {
                Log.e("MainViewModel", "connect fail ${e.message}")
                _connectionState.update {
                    it.copy(
                        statusText = "手环连接失败",
                        descriptionText = e.message ?: "未知错误",
                        isConnected = false
                    )
                }
                // 延迟设置错误状态，确保之前的sheet已经完全隐藏
                delay(300)
                _connectionErrorState.value = ConnectionErrorState(
                    deviceName = null,
                    isUnsupportedDevice = false
                )
            }
        }
    }
    
    fun dismissConnectionError() {
        _connectionErrorState.value = null
    }

    private fun loadBooks() {
        viewModelScope.launch(Dispatchers.IO) {
            val bookEntities = db.bookDao().getAllBooks()
            val bookUiModels = bookEntities.map { entity ->
                val chapterCount = db.chapterDao().getChapterCountForBook(entity.id)
                val wordCount = db.chapterDao().getTotalWordCountForBook(entity.id) ?: 0
                Book(
                    name = entity.name,
                    path = entity.path,
                    size = entity.size,
                    chapterCount = chapterCount,
                    wordCount = wordCount,
                    syncedChapterCount = 0,
                    coverImagePath = entity.coverImagePath,
                    localCategory = entity.localCategory
                )
            }
            withContext(Dispatchers.Main) {
                _books.value = bookUiModels.sortedByDescending { it.name }
            }
        }
    }

    fun startImport(uri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            UritoFile(uri, context)?.let { sourceFile ->
                
                val fileName = sourceFile.name.lowercase()
                val allowedExtensions = listOf(".txt", ".epub", ".nvb")
                val hasValidExtension = allowedExtensions.any { fileName.endsWith(it) }
                
                if (!hasValidExtension) {
                    
                    withContext(Dispatchers.Main) {
                        _importState.value = null
                        _importingState.value = ImportingState(
                            bookName = sourceFile.nameWithoutExtension,
                            statusText = "不支持的文件格式\n仅支持 TXT、EPUB、NVB 格式",
                            progress = 0f
                        )
                    }
                    delay(3000)
                    withContext(Dispatchers.Main) {
                        _importingState.value = null
                    }
                    return@launch
                }
                
                val fileFormat = detectFileFormat(context, uri)
                _importState.value =
                    ImportState(
                        uri = uri, 
                        bookName = sourceFile.nameWithoutExtension,
                        fileSize = sourceFile.length(),
                        fileFormat = fileFormat
                    )
            }
        }
    }

    fun cancelImport() {
        _importState.value = null
    }

    fun confirmImport(bookName: String, splitMethod: String, noSplit: Boolean, wordsPerChapter: Int, selectedCategory: String? = null, enableChapterMerge: Boolean = false, mergeMinWords: Int = 500, enableChapterRename: Boolean = false, renamePattern: String = "") {
        val state = _importState.value ?: return

        val finalBookName = bookName.trim()
        if (finalBookName.isEmpty()) {
            return
        }

        
        val finalCategory = selectedCategory ?: state.selectedCategory

        viewModelScope.launch(Dispatchers.IO) {
            val existingBook = _books.value.find { it.name == finalBookName }
            
            
            val context = getApplication<Application>().applicationContext
            val fileFormat = detectFileFormat(context, state.uri)
            
            
            if (existingBook != null && (fileFormat == "epub" || fileFormat == "nvb")) {
                withContext(Dispatchers.Main) {
                    _importState.value = null
                }
                
                performImport(state.uri, finalBookName, splitMethod, noSplit, false, wordsPerChapter, finalCategory, enableChapterMerge, mergeMinWords, enableChapterRename, renamePattern)
                return@launch
            }
            
            
            if (existingBook != null) {
                withContext(Dispatchers.Main) {
                    _importState.value = null
                    _overwriteConfirmState.value = OverwriteConfirmState(
                        existingBook = existingBook,
                        uri = state.uri,
                        newBookName = finalBookName,
                        splitMethod = splitMethod,
                        noSplit = noSplit,
                        wordsPerChapter = wordsPerChapter
                    )
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                _importState.value = null
            }
            performImport(state.uri, finalBookName, splitMethod, noSplit, false, wordsPerChapter, finalCategory, enableChapterMerge, mergeMinWords, enableChapterRename, renamePattern)
        }
    }

    fun cancelOverwriteConfirm() {
        _overwriteConfirmState.value = null
    }

    fun confirmOverwrite() {
        val overwriteState = _overwriteConfirmState.value ?: return
        _overwriteConfirmState.value = null

        viewModelScope.launch(Dispatchers.IO) {
            deleteBookInternal(overwriteState.existingBook)
            performImport(
                overwriteState.uri,
                overwriteState.newBookName,
                overwriteState.splitMethod,
                overwriteState.noSplit,
                true,
                overwriteState.wordsPerChapter,
                null
            )
        }
    }

    private suspend fun deleteBookInternal(book: Book) {
        File(book.path).delete()
        val bookEntity = db.bookDao().getBookByPath(book.path)
        if (bookEntity != null) {
            val context = getApplication<Application>().applicationContext
            com.bandbbs.ebook.utils.ChapterContentManager.deleteBookChapters(context, bookEntity.id)
            db.chapterDao().deleteChaptersByBookId(bookEntity.id)
            db.bookDao().delete(bookEntity)
        }
    }

    private suspend fun performImport(
        uri: Uri,
        finalBookName: String,
        splitMethod: String,
        noSplit: Boolean,
        isOverwrite: Boolean,
        wordsPerChapter: Int,
        selectedCategory: String? = null,
        enableChapterMerge: Boolean = false,
        mergeMinWords: Int = 500,
        enableChapterRename: Boolean = false,
        renamePattern: String = ""
    ) {
        _importingState.value = ImportingState(bookName = finalBookName)
        val context = getApplication<Application>().applicationContext
        
        try {
            
            _importingState.update { it?.copy(statusText = "正在识别文件格式...") }
            val fileFormat = detectFileFormat(context, uri)
            
            when (fileFormat) {
                "nvb" -> importNvbFile(context, uri, finalBookName, noSplit, selectedCategory, enableChapterMerge, mergeMinWords, enableChapterRename, renamePattern)
                "epub" -> importEpubFile(context, uri, finalBookName, noSplit, selectedCategory, enableChapterMerge, mergeMinWords, enableChapterRename, renamePattern)
                else -> importTxtFile(context, uri, finalBookName, splitMethod, noSplit, wordsPerChapter, selectedCategory)
            }
            
            withContext(Dispatchers.Main) {
                _importingState.value = null
                loadBooks()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                _importingState.update { 
                    it?.copy(statusText = "导入失败: ${e.message}", progress = 0f) 
                }
            }
            Log.e("MainViewModel", "Import failed", e)
        }
    }

    private fun detectFileFormat(context: Context, uri: Uri): String {
        return when {
            NvbParser.isNvbFile(context, uri) -> "nvb"
            EpubParser.isEpubFile(context, uri) -> "epub"
            else -> "txt"
        }
    }

    /**
     * 应用章节重命名规则
     * 格式: "查找模式 -> 替换文本"
     * 支持正则表达式，替换文本中可以使用 $1, $2 等引用捕获组
     */
    private fun applyRenamePattern(chapterName: String, pattern: String): String {
        if (pattern.isBlank()) return chapterName
        
        try {
            val parts = pattern.split(" -> ", limit = 2)
            if (parts.size != 2) return chapterName
            
            val findPattern = parts[0].trim()
            val replaceText = parts[1].trim()
            
            val regex = Regex(findPattern)
            return regex.replace(chapterName) { matchResult ->
                var result = replaceText
                matchResult.groupValues.forEachIndexed { index, group ->
                    if (index > 0) {
                        result = result.replace("\$$index", group)
                    }
                }
                result
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to apply rename pattern: ${e.message}")
            return chapterName
        }
    }

    /**
     * 合并短章节
     */
    private suspend fun mergeShortChapters(
        context: Context,
        bookId: Int,
        chapters: List<Chapter>,
        minWords: Int
    ): List<Chapter> {
        if (chapters.isEmpty() || minWords <= 0) return chapters
        
        val mergedChapters = mutableListOf<Chapter>()
        var i = 0
        
        while (i < chapters.size) {
            val currentChapter = chapters[i]
            
            if (currentChapter.wordCount < minWords && mergedChapters.isNotEmpty()) {
                val lastChapter = mergedChapters.last()
                val lastContent = com.bandbbs.ebook.utils.ChapterContentManager.readChapterContent(lastChapter.contentFilePath)
                val currentContent = com.bandbbs.ebook.utils.ChapterContentManager.readChapterContent(currentChapter.contentFilePath)
                val mergedContent = lastContent.trimEnd() + "\n\n" + currentChapter.name + "\n\n" + currentContent.trimStart()
                
                com.bandbbs.ebook.utils.ChapterContentManager.saveChapterContent(
                    context, bookId, lastChapter.index, mergedContent
                )
                com.bandbbs.ebook.utils.ChapterContentManager.deleteChapterContent(currentChapter.contentFilePath)
                
                mergedChapters[mergedChapters.size - 1] = lastChapter.copy(
                    wordCount = mergedContent.length
                )
            } else {
                mergedChapters.add(currentChapter)
            }
            
            i++
        }
        
        return mergedChapters.mapIndexed { index, chapter ->
            chapter.copy(index = index)
        }
    }

    private suspend fun importNvbFile(context: Context, uri: Uri, finalBookName: String, noSplit: Boolean, selectedCategory: String? = null, enableChapterMerge: Boolean = false, mergeMinWords: Int = 500, enableChapterRename: Boolean = false, renamePattern: String = "") {
        _importingState.update { it?.copy(statusText = "正在解析 NVB 文件...", progress = 0.1f) }
        val nvbBook = NvbParser.parse(context, uri)
        
        
        val existingBook = db.bookDao().getBookByName(finalBookName)
        
        _importingState.update { it?.copy(statusText = "正在复制文件...", progress = 0.3f) }
        UritoFile(uri, context)?.let { sourceFile ->
            val destFile = File(booksDir, sourceFile.name)
            sourceFile.copyTo(destFile, overwrite = true)
            
            
            var coverImagePath: String? = null
            nvbBook.coverImage?.let { coverBytes ->
                val coverFile = File(booksDir, "${finalBookName}_cover.jpg")
                coverFile.writeBytes(coverBytes)
                coverImagePath = coverFile.absolutePath
            }
            
            val bookId = if (existingBook != null) {
                
                _importingState.update { it?.copy(statusText = "检测到已存在的书籍，准备更新...", progress = 0.5f) }
                
                if (coverImagePath != null) {
                    db.bookDao().update(existingBook.copy(
                        size = destFile.length(),
                        coverImagePath = coverImagePath
                    ))
                } else {
                    db.bookDao().update(existingBook.copy(size = destFile.length()))
                }
                existingBook.id.toLong()
            } else {
                
                _importingState.update { it?.copy(statusText = "正在写入数据库...", progress = 0.5f) }
                db.bookDao().insert(
                    BookEntity(
                        name = finalBookName,
                        path = destFile.absolutePath,
                        size = destFile.length(),
                        format = "nvb",
                        coverImagePath = coverImagePath,
                        author = nvbBook.metadata.author,
                        summary = nvbBook.metadata.summary,
                        bookStatus = nvbBook.metadata.bookStatus,
                        category = nvbBook.metadata.category,
                        localCategory = selectedCategory
                    )
                )
            }
            
            _importingState.update { it?.copy(statusText = "正在导入章节...", progress = 0.7f) }
            
            
            
            val existingChapters = db.chapterDao().getChapterInfoForBook(bookId.toInt())
            val existingChapterNames = existingChapters.map { it.name }.toSet()
            
            val chapters = if (noSplit) {
                
                val allContent = nvbBook.chapters.joinToString("\n\n") { chapter ->
                    "${chapter.title}\n\n${chapter.content}"
                }
                val totalWordCount = nvbBook.chapters.sumOf { it.wordCount }
                val contentFilePath = com.bandbbs.ebook.utils.ChapterContentManager.saveChapterContent(
                    context, bookId.toInt(), 0, allContent
                )
                listOf(
                    Chapter(
                        bookId = bookId.toInt(),
                        index = 0,
                        name = "全文",
                        contentFilePath = contentFilePath,
                        wordCount = totalWordCount
                    )
                )
            } else {
                
                val startIndex = if (existingBook != null) existingChapters.size else 0
                val newChapters = mutableListOf<Chapter>()
                var currentIndex = startIndex
                
                nvbBook.chapters.forEach { nvbChapter ->
                    if (existingBook == null || nvbChapter.title !in existingChapterNames) {
                        var chapterName = nvbChapter.title
                        if (enableChapterRename) {
                            chapterName = applyRenamePattern(chapterName, renamePattern)
                        }
                        
                        val contentFilePath = com.bandbbs.ebook.utils.ChapterContentManager.saveChapterContent(
                            context, bookId.toInt(), currentIndex, nvbChapter.content
                        )
                        newChapters.add(
                            Chapter(
                                bookId = bookId.toInt(),
                                index = currentIndex,
                                name = chapterName,
                                contentFilePath = contentFilePath,
                                wordCount = nvbChapter.wordCount
                            )
                        )
                        currentIndex++
                    }
                }
                
                var processedChapters: List<Chapter> = newChapters
                if (enableChapterMerge && processedChapters.isNotEmpty()) {
                    _importingState.update { it?.copy(statusText = "正在合并短章节...", progress = 0.85f) }
                    processedChapters = mergeShortChapters(context, bookId.toInt(), processedChapters, mergeMinWords)
                }
                
                processedChapters
            }
            
            var finalChapters: List<Chapter> = chapters
            if (enableChapterMerge && !noSplit && finalChapters.isNotEmpty()) {
                _importingState.update { it?.copy(statusText = "正在合并短章节...", progress = 0.85f) }
                finalChapters = mergeShortChapters(context, bookId.toInt(), finalChapters, mergeMinWords)
            }
            
            _importingState.update { 
                it?.copy(
                    statusText = if (existingBook != null) "正在保存新章节 (${finalChapters.size} 章)..." else "正在保存章节...", 
                    progress = 0.9f
                ) 
            }
            if (finalChapters.isNotEmpty()) {
                db.chapterDao().insertAll(finalChapters)
            }
            
            sourceFile.delete()
        }
    }

    private suspend fun importEpubFile(context: Context, uri: Uri, finalBookName: String, noSplit: Boolean, selectedCategory: String? = null, enableChapterMerge: Boolean = false, mergeMinWords: Int = 500, enableChapterRename: Boolean = false, renamePattern: String = "") {
        _importingState.update { it?.copy(statusText = "正在解析 EPUB 文件...", progress = 0.1f) }
        val epubBook = EpubParser.parse(context, uri)
        
        
        val existingBook = db.bookDao().getBookByName(finalBookName)
        
        _importingState.update { it?.copy(statusText = "正在复制文件...", progress = 0.3f) }
        UritoFile(uri, context)?.let { sourceFile ->
            val destFile = File(booksDir, sourceFile.name)
            sourceFile.copyTo(destFile, overwrite = true)
            
            
            var coverImagePath: String? = null
            epubBook.coverImage?.let { coverBytes ->
                val coverFile = File(booksDir, "${finalBookName}_cover.jpg")
                coverFile.writeBytes(coverBytes)
                coverImagePath = coverFile.absolutePath
            }
            
            val bookId = if (existingBook != null) {
                
                _importingState.update { it?.copy(statusText = "检测到已存在的书籍，准备更新...", progress = 0.5f) }
                
                if (coverImagePath != null) {
                    db.bookDao().update(existingBook.copy(
                        size = destFile.length(),
                        coverImagePath = coverImagePath
                    ))
                } else {
                    db.bookDao().update(existingBook.copy(size = destFile.length()))
                }
                existingBook.id.toLong()
            } else {
                
                _importingState.update { it?.copy(statusText = "正在写入数据库...", progress = 0.5f) }
                db.bookDao().insert(
                    BookEntity(
                        name = finalBookName,
                        path = destFile.absolutePath,
                        size = destFile.length(),
                        format = "epub",
                        coverImagePath = coverImagePath,
                        author = epubBook.author,
                        localCategory = selectedCategory
                    )
                )
            }
            
            _importingState.update { it?.copy(statusText = "正在导入章节...", progress = 0.7f) }
            
            
            
            val existingChapters = db.chapterDao().getChapterInfoForBook(bookId.toInt())
            val existingChapterNames = existingChapters.map { it.name }.toSet()
            
            val chapters = if (noSplit) {
                
                val allContent = epubBook.chapters.joinToString("\n\n") { chapter ->
                    "${chapter.title}\n\n${chapter.content}"
                }
                val totalWordCount = epubBook.chapters.sumOf { it.wordCount }
                val contentFilePath = com.bandbbs.ebook.utils.ChapterContentManager.saveChapterContent(
                    context, bookId.toInt(), 0, allContent
                )
                listOf(
                    Chapter(
                        bookId = bookId.toInt(),
                        index = 0,
                        name = "全文",
                        contentFilePath = contentFilePath,
                        wordCount = totalWordCount
                    )
                )
            } else {
                
                val startIndex = if (existingBook != null) existingChapters.size else 0
                val newChapters = mutableListOf<Chapter>()
                var currentIndex = startIndex
                
                epubBook.chapters.forEach { epubChapter ->
                    if (existingBook == null || epubChapter.title !in existingChapterNames) {
                        var chapterName = epubChapter.title
                        if (enableChapterRename) {
                            chapterName = applyRenamePattern(chapterName, renamePattern)
                        }
                        
                        val contentFilePath = com.bandbbs.ebook.utils.ChapterContentManager.saveChapterContent(
                            context, bookId.toInt(), currentIndex, epubChapter.content
                        )
                        newChapters.add(
                            Chapter(
                                bookId = bookId.toInt(),
                                index = currentIndex,
                                name = chapterName,
                                contentFilePath = contentFilePath,
                                wordCount = epubChapter.wordCount
                            )
                        )
                        currentIndex++
                    }
                }
                
                var processedChapters: List<Chapter> = newChapters
                if (enableChapterMerge && processedChapters.isNotEmpty()) {
                    _importingState.update { it?.copy(statusText = "正在合并短章节...", progress = 0.85f) }
                    processedChapters = mergeShortChapters(context, bookId.toInt(), processedChapters, mergeMinWords)
                }
                
                processedChapters
            }
            
            var finalChapters: List<Chapter> = chapters
            if (enableChapterMerge && !noSplit && finalChapters.isNotEmpty()) {
                _importingState.update { it?.copy(statusText = "正在合并短章节...", progress = 0.85f) }
                finalChapters = mergeShortChapters(context, bookId.toInt(), finalChapters, mergeMinWords)
            }
            
            _importingState.update { 
                it?.copy(
                    statusText = if (existingBook != null) "正在保存新章节 (${finalChapters.size} 章)..." else "正在保存章节...", 
                    progress = 0.9f
                ) 
            }
            if (finalChapters.isNotEmpty()) {
                db.chapterDao().insertAll(finalChapters)
            }
            
            sourceFile.delete()
        }
    }

    private suspend fun importTxtFile(
        context: Context,
        uri: Uri,
        finalBookName: String,
        splitMethod: String,
        noSplit: Boolean,
        wordsPerChapter: Int,
        selectedCategory: String? = null
    ) {
        UritoFile(uri, context)?.let { sourceFile ->
            _importingState.update { it?.copy(statusText = "正在复制文件...") }
            val destFile = File(booksDir, sourceFile.name)
            sourceFile.copyTo(destFile, overwrite = true)

            _importingState.update { it?.copy(statusText = "正在写入数据库...") }
            val bookId = db.bookDao().insert(
                BookEntity(
                    name = finalBookName,
                    path = destFile.absolutePath,
                    size = destFile.length(),
                    format = "txt",
                    localCategory = selectedCategory
                )
            )

            val initialChapters = if (noSplit) {
                _importingState.update { it?.copy(statusText = "正在读取全文...", progress = 0.5f) }
                val content = ChapterSplitter.readTextFromUri(context, uri)
                val contentFilePath = com.bandbbs.ebook.utils.ChapterContentManager.saveChapterContent(
                    context, bookId.toInt(), 0, content.trim()
                )
                listOf(
                    Chapter(
                        bookId = bookId.toInt(),
                        index = 0,
                        name = "全文",
                        contentFilePath = contentFilePath,
                        wordCount = content.trim().length
                    )
                )
            } else {
                ChapterSplitter.split(context, uri, bookId.toInt(), splitMethod, { progress, status ->
                    _importingState.update {
                        it?.copy(
                            statusText = status,
                            progress = progress
                        )
                    }
                }, wordsPerChapter)
            }

            _importingState.update { it?.copy(statusText = "正在后处理章节...", progress = 0.9f) }
            val finalChapters = mutableListOf<Chapter>()
            val mergedChapterTitles = mutableListOf<String>()

            for (chapter in initialChapters) {
                val chapterContent = com.bandbbs.ebook.utils.ChapterContentManager.readChapterContent(chapter.contentFilePath)
                if (chapter.wordCount == 0 && chapterContent.isBlank()) {
                    if (finalChapters.isNotEmpty()) {
                        val lastChapter = finalChapters.last()
                        val lastContent = com.bandbbs.ebook.utils.ChapterContentManager.readChapterContent(lastChapter.contentFilePath)
                        val updatedContent = lastContent.trimEnd() + "\n\n" + chapter.name.trim()
                        com.bandbbs.ebook.utils.ChapterContentManager.saveChapterContent(
                            context, bookId.toInt(), lastChapter.index, updatedContent
                        )
                        finalChapters[finalChapters.size - 1] = lastChapter.copy(
                            wordCount = updatedContent.length
                        )
                        com.bandbbs.ebook.utils.ChapterContentManager.deleteChapterContent(chapter.contentFilePath)
                        mergedChapterTitles.add(chapter.name)
                    } else {
                        com.bandbbs.ebook.utils.ChapterContentManager.deleteChapterContent(chapter.contentFilePath)
                        mergedChapterTitles.add("${chapter.name} (因内容为空已被跳过)")
                    }
                } else {
                    finalChapters.add(chapter)
                }
            }

            val reIndexedChapters = finalChapters.mapIndexed { index, chapter ->
                chapter.copy(index = index)
            }

            _importingState.update { it?.copy(statusText = "正在保存章节...", progress = 1.0f) }
            db.chapterDao().insertAll(reIndexedChapters)

            sourceFile.delete()
            
            if (mergedChapterTitles.isNotEmpty()) {
                val reportMessage = "有 ${mergedChapterTitles.size} 个章节因内容为空，其标题已被合并到上一章节末尾或被跳过:\n\n" +
                        mergedChapterTitles.joinToString("\n") { "- $it" }
                withContext(Dispatchers.Main) {
                    _importReportState.value = ImportReportState(
                        bookName = finalBookName,
                        mergedChaptersInfo = reportMessage
                    )
                }
            }
        } ?: run {
            throw IllegalArgumentException("无法读取文件")
        }
    }

    fun dismissImportReport() {
        _importReportState.value = null
    }

    fun requestDeleteBook(book: Book) {
        _bookToDelete.value = book
    }

    fun confirmDeleteBook() {
        _bookToDelete.value?.let { book ->
            viewModelScope.launch(Dispatchers.IO) {
                File(book.path).delete()
                val bookEntity = db.bookDao().getBookByPath(book.path)
                if (bookEntity != null) {
                    val context = getApplication<Application>().applicationContext
                    com.bandbbs.ebook.utils.ChapterContentManager.deleteBookChapters(context, bookEntity.id)
                    db.chapterDao().deleteChaptersByBookId(bookEntity.id)
                    db.bookDao().delete(bookEntity)
                }
                withContext(Dispatchers.Main) {
                    loadBooks()
                }
            }
        }
        _bookToDelete.value = null
    }

    fun cancelDeleteBook() {
        _bookToDelete.value = null
    }

    fun startPush(book: Book) {
        if (fileConn.busy || _syncOptionsState.value != null) return

        _syncOptionsState.value = SyncOptionsState(book, 0, 0, emptyList(), false)

        viewModelScope.launch {
            try {
                conn.init()
                delay(500L)
                val bookStatus = withContext(Dispatchers.IO) {
                    fileConn.getBookStatus(book.name)
                }
                val (totalChapters, chapters, hasCover) = withContext(Dispatchers.IO) {
                    val bookEntity = db.bookDao().getBookByPath(book.path)
                    if (bookEntity != null) {
                        val count = db.chapterDao().getChapterCountForBook(bookEntity.id)
                        
                        val chapterList = db.chapterDao().getChapterInfoForBook(bookEntity.id)
                        val hasCoverImage = bookEntity.coverImagePath != null
                        Triple(count, chapterList, hasCoverImage)
                    } else {
                        Triple(0, emptyList(), false)
                    }
                }
                _syncOptionsState.value = SyncOptionsState(
                    book = book, 
                    totalChapters = totalChapters, 
                    syncedChapters = bookStatus.syncedChapters.size,
                    chapters = chapters, 
                    hasCover = hasCover,
                    isCoverSynced = bookStatus.hasCover
                )
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to get book status", e)
                _pushState.update { it.copy(statusText = "获取手环状态失败: ${e.message}", isFinished = true, isSuccess = false, book = book) }
                _syncOptionsState.value = null
            }
        }
    }

    private fun addTransferLog(message: String) {
        _pushState.update { state ->
            val newLog = (state.transferLog + message).takeLast(100)
            state.copy(transferLog = newLog)
        }
    }

    fun syncCoverOnly(book: Book) {
        if (fileConn.busy || book.coverImagePath == null) return

        val initialLog = listOf("准备传输封面...")
        _pushState.value = PushState(book = book, preview = "准备传输封面...", transferLog = initialLog)

        viewModelScope.launch(Dispatchers.Main) {
            fileConn.sendCoverOnly(
                book = book,
                coverImagePath = book.coverImagePath,
                onError = { error, _ ->
                    addTransferLog("[错误] 封面同步失败: $error")
                    _pushState.update {
                        it.copy(
                            statusText = "封面同步失败: $error",
                            isFinished = true,
                            isSuccess = false
                        )
                    }
                },
                onSuccess = { _, _ ->
                    addTransferLog("[成功] 封面同步完成")
                    _pushState.update {
                        it.copy(
                            statusText = "封面同步成功",
                            progress = 1.0,
                            isFinished = true,
                            isSuccess = true
                        )
                    }
                },
                onCoverProgress = { current, total ->
                    if (total > 0) {
                        val logMessage = "传输封面分块: $current/$total"
                        addTransferLog(logMessage)
                        _pushState.update {
                            it.copy(
                                isSendingCover = true,
                                coverProgress = "封面: $current/$total",
                                statusText = "正在同步封面..."
                            )
                        }
                    } else {
                        _pushState.update {
                            it.copy(isSendingCover = false, coverProgress = "")
                        }
                    }
                }
            )
        }
    }

    private var pendingPushBook: Book? = null
    private var pendingPushChapters: Set<Int>? = null
    private var pendingSyncCover: Boolean = false

    fun confirmPush(book: Book, selectedChapterIndices: Set<Int>, syncCover: Boolean = false) {
        if (selectedChapterIndices.isEmpty()) {
            return
        }

        val hasConfirmedFirstSync = prefs.getBoolean(FIRST_SYNC_CONFIRMED_KEY, false)
        if (!hasConfirmedFirstSync) {
            pendingPushBook = book
            pendingPushChapters = selectedChapterIndices
            pendingSyncCover = syncCover
            _firstSyncConfirmState.value = book
            return
        }

        val isCoverAlreadySynced = _syncOptionsState.value?.isCoverSynced ?: false
        _syncOptionsState.value = null
        performPush(book, selectedChapterIndices, syncCover, isCoverAlreadySynced)
    }

    fun confirmFirstSync() {
        prefs.edit().putBoolean(FIRST_SYNC_CONFIRMED_KEY, true).apply()
        _firstSyncConfirmState.value = null
        
        val book = pendingPushBook
        val chapters = pendingPushChapters
        val syncCover = pendingSyncCover
        
        pendingPushBook = null
        pendingPushChapters = null
        pendingSyncCover = false
        
        if (book != null && chapters != null && chapters.isNotEmpty()) {
            val isCoverAlreadySynced = _syncOptionsState.value?.isCoverSynced ?: false
            _syncOptionsState.value = null
            performPush(book, chapters, syncCover, isCoverAlreadySynced)
        }
    }

    fun cancelFirstSyncConfirm() {
        _firstSyncConfirmState.value = null
        pendingPushBook = null
        pendingPushChapters = null
        pendingSyncCover = false
    }

    private fun performPush(book: Book, selectedChapterIndices: Set<Int>, syncCover: Boolean, isCoverAlreadySynced: Boolean) {

        val initialMessage = if (syncCover && !isCoverAlreadySynced) "准备传输封面..." else "准备开始传输..."
        val initialLog = listOf(initialMessage)
        _pushState.value = PushState(book = book, preview = initialMessage, transferLog = initialLog, isTransferring = true)

        viewModelScope.launch(Dispatchers.IO) {
            val bookEntity = db.bookDao().getBookByPath(book.path) ?: return@launch
            
            val sortedIndices = selectedChapterIndices.sorted()

            if (sortedIndices.isEmpty()) {
                withContext(Dispatchers.Main) {
                    addTransferLog("[完成] 没有需要同步的章节")
                    _pushState.update { it.copy(statusText = "没有需要同步的章节", isFinished = true, isSuccess = true, isTransferring = false) }
                }
                return@launch
            }
            
            val firstChapterName = if (sortedIndices.isNotEmpty()) {
                db.chapterDao().getChapterInfoForBook(bookEntity.id)
                    .find { it.index == sortedIndices.first() }?.name ?: ""
            } else {
                ""
            }

            val startFromIndex = sortedIndices.first()
            val totalChaptersInBook = db.chapterDao().getChapterCountForBook(bookEntity.id)
            
            val coverImagePath = if (syncCover && !isCoverAlreadySynced) bookEntity.coverImagePath else null

            withContext(Dispatchers.Main) {
                addTransferLog("开始传输，共 ${sortedIndices.size} 章")
                if (coverImagePath != null) {
                    addTransferLog("包含封面图片")
                }
                
                fileConn.sentChapters(
                    book = book,
                    bookId = bookEntity.id,
                    chaptersIndicesToSend = sortedIndices,
                    chapterDao = db.chapterDao(),
                    totalChaptersInBook = totalChaptersInBook,
                    startFromIndex = startFromIndex,
                    firstChapterName = firstChapterName,
                    coverImagePath = coverImagePath,
                    bookEntity = bookEntity,
                    onError = { error, count ->
                        addTransferLog("[错误] 传输失败: $error (章节索引: $count)")
                        _pushState.update {
                            it.copy(
                                statusText = "传输失败: $error",
                                isFinished = true,
                                isSuccess = false,
                                isTransferring = false
                            )
                        }
                    },
                    onSuccess = { message, count ->
                        addTransferLog("[成功] $message，共传输 $count 章")
                        _pushState.update {
                            it.copy(
                                statusText = "传输成功",
                                progress = 1.0,
                                isFinished = true,
                                isSuccess = true,
                                isTransferring = false
                            )
                        }
                    },
                    onProgress = { p, preview, speed ->
                        val progressPercent = (p * 100).toInt()
                        val logMessage = if (preview.isNotEmpty()) {
                            "[$progressPercent%] $preview"
                        } else {
                            "[$progressPercent%] 传输中"
                        }
                        addTransferLog(logMessage)
                        _pushState.update {
                            it.copy(
                                progress = p,
                                preview = preview,
                                speed = speed,
                                statusText = "正在推送 $progressPercent%",
                                isTransferring = true
                            )
                        }
                    },
                    onCoverProgress = { current, total ->
                        if (total > 0) {
                            val logMessage = "传输封面分块: $current/$total"
                            addTransferLog(logMessage)
                            _pushState.update {
                                it.copy(
                                    isSendingCover = true,
                                    coverProgress = "封面: $current/$total",
                                    isTransferring = true
                                )
                            }
                        } else {
                            _pushState.update {
                                it.copy(isSendingCover = false, coverProgress = "")
                            }
                        }
                    }
                )
            }
        }
    }

    fun cancelPush() {
        if (fileConn.busy) {
            fileConn.cancel()
        }
        conn.setOnDisconnected { }
        _syncOptionsState.value = null
        resetPushState()
    }

    fun resetPushState() {
        _pushState.value = PushState()
    }

    fun showChapterList(book: Book) {
        viewModelScope.launch(Dispatchers.IO) {
            val bookEntity = db.bookDao().getBookByPath(book.path)
            if (bookEntity != null) {
                
                val chapters = db.chapterDao().getChapterInfoForBook(bookEntity.id)
                withContext(Dispatchers.Main) {
                    _chaptersForSelectedBook.value = chapters
                    _selectedBookForChapters.value = book
                }
            }
        }
    }

    fun closeChapterList() {
        _selectedBookForChapters.value = null
        _chaptersForSelectedBook.value = emptyList()
    }

    fun showChapterPreview(chapterId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val chapter = db.chapterDao().getChapterById(chapterId)
            if (chapter != null) {
                val content = com.bandbbs.ebook.utils.ChapterContentManager.readChapterContent(chapter.contentFilePath)
                val chapterWithContent = com.bandbbs.ebook.ui.model.ChapterWithContent(
                    id = chapter.id,
                    name = chapter.name,
                    content = content,
                    wordCount = chapter.wordCount
                )
                withContext(Dispatchers.Main) {
                    _chapterToPreview.value = chapterWithContent
                }
            } else {
                withContext(Dispatchers.Main) {
                    _chapterToPreview.value = null
                }
            }
        }
    }

    fun closeChapterPreview() {
        _chapterToPreview.value = null
    }
    
    fun requestImportCover(book: Book) {
        _bookForCoverImport.value = book
    }
    
    fun cancelImportCover() {
        _bookForCoverImport.value = null
    }
    
    fun importCoverForBook(uri: Uri) {
        val book = _bookForCoverImport.value ?: return
        _bookForCoverImport.value = null
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>().applicationContext
                val bookEntity = db.bookDao().getBookByPath(book.path) ?: return@launch
                
                
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                val imageBytes = inputStream.readBytes()
                inputStream.close()
                
                
                val coverFile = File(booksDir, "${bookEntity.name}_cover.jpg")
                coverFile.writeBytes(imageBytes)
                
                
                db.bookDao().update(bookEntity.copy(coverImagePath = coverFile.absolutePath))
                
                
                withContext(Dispatchers.Main) {
                    loadBooks()
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to import cover", e)
            }
        }
    }
}
