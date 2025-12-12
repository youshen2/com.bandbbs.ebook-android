package com.bandbbs.ebook.ui.viewmodel.handlers

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import com.bandbbs.ebook.database.AppDatabase
import com.bandbbs.ebook.database.BookEntity
import com.bandbbs.ebook.database.Chapter
import com.bandbbs.ebook.ui.model.Book
import com.bandbbs.ebook.ui.viewmodel.ImportReportState
import com.bandbbs.ebook.ui.viewmodel.ImportState
import com.bandbbs.ebook.ui.viewmodel.ImportingState
import com.bandbbs.ebook.ui.viewmodel.OverwriteConfirmState
import com.bandbbs.ebook.utils.BookInfoParser
import com.bandbbs.ebook.utils.ChapterSplitter
import com.bandbbs.ebook.utils.EpubParser
import com.bandbbs.ebook.utils.NvbParser
import com.bandbbs.ebook.utils.UritoFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ImportHandler(
    private val application: Application,
    private val db: AppDatabase,
    private val booksDir: File,
    private val scope: CoroutineScope,
    private val booksState: MutableStateFlow<List<Book>>,
    private val importState: MutableStateFlow<ImportState?>,
    private val importingState: MutableStateFlow<ImportingState?>,
    private val importReportState: MutableStateFlow<ImportReportState?>,
    private val overwriteConfirmState: MutableStateFlow<OverwriteConfirmState?>,
    private val onBooksChanged: () -> Unit,
) {

    fun startImport(uri: Uri) {
        startImportBatch(listOf(uri))
    }

    fun startImportBatch(uris: List<Uri>) {
        scope.launch {
            val context = application.applicationContext
            val validFiles = mutableListOf<com.bandbbs.ebook.ui.viewmodel.ImportFileInfo>()
            val allowedExtensions = listOf(".txt", ".epub", ".nvb")
            
            uris.forEach { uri ->
                UritoFile(uri, context)?.let { sourceFile ->
                    val fileName = sourceFile.name.lowercase()
                    val hasValidExtension = allowedExtensions.any { fileName.endsWith(it) }

                    if (hasValidExtension) {
                        val fileFormat = detectFileFormat(context, uri)
                        validFiles.add(
                            com.bandbbs.ebook.ui.viewmodel.ImportFileInfo(
                                uri = uri,
                                bookName = sourceFile.nameWithoutExtension,
                                fileSize = sourceFile.length(),
                                fileFormat = fileFormat
                            )
                        )
                    } else {
                        // 对于无效文件，显示错误但不阻止其他文件导入
                        withContext(Dispatchers.Main) {
                            importingState.value = ImportingState(
                                bookName = sourceFile.nameWithoutExtension,
                                statusText = "${sourceFile.name} 不支持的文件格式\n仅支持 TXT、EPUB、NVB 格式",
                                progress = 0f
                            )
                        }
                        delay(2000)
                    }
                }
            }

            if (validFiles.isEmpty()) {
                withContext(Dispatchers.Main) {
                    importState.value = null
                    importingState.value = null
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                importState.value = com.bandbbs.ebook.ui.viewmodel.ImportState(
                    uris = validFiles.map { it.uri },
                    files = validFiles
                )
            }
        }
    }

    fun cancelImport() {
        importState.value = null
    }

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
    ) {
        val state = importState.value ?: return

        scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                importState.value = null
            }
            val finalCategory = selectedCategory ?: state.selectedCategory

            if (state.isMultipleFiles) {
                // 批量导入：使用文件名作为书名，统一应用设置
                state.files.forEach { fileInfo ->
                    val finalBookName = fileInfo.bookName.trim()
                    if (finalBookName.isNotEmpty()) {
                        val existingBook = booksState.value.find { it.name == finalBookName }
                        val context = application.applicationContext
                        val fileFormat = detectFileFormat(context, fileInfo.uri)

                        if (existingBook != null && (fileFormat == "epub" || fileFormat == "nvb")) {
                            // EPUB/NVB 格式直接更新
                            performImport(
                                fileInfo.uri,
                                finalBookName,
                                splitMethod,
                                noSplit,
                                false,
                                wordsPerChapter,
                                finalCategory,
                                enableChapterMerge,
                                mergeMinWords,
                                enableChapterRename,
                                renamePattern,
                                customRegex
                            )
                        } else if (existingBook == null) {
                            // 新书直接导入
                            performImport(
                                fileInfo.uri,
                                finalBookName,
                                splitMethod,
                                noSplit,
                                false,
                                wordsPerChapter,
                                finalCategory,
                                enableChapterMerge,
                                mergeMinWords,
                                enableChapterRename,
                                renamePattern,
                                customRegex
                            )
                        }
                        // 如果存在同名书籍且不是 EPUB/NVB，跳过（避免覆盖确认弹窗打断批量导入）
                    }
                }
            } else {
                // 单文件导入：保持原有逻辑
                val finalBookName = bookName.trim()
                if (finalBookName.isEmpty()) {
                    return@launch
                }

                val existingBook = booksState.value.find { it.name == finalBookName }
                val context = application.applicationContext
                val fileFormat = detectFileFormat(context, state.uri)

                if (existingBook != null && (fileFormat == "epub" || fileFormat == "nvb")) {
                    performImport(
                        state.uri,
                        finalBookName,
                        splitMethod,
                        noSplit,
                        false,
                        wordsPerChapter,
                        finalCategory,
                        enableChapterMerge,
                        mergeMinWords,
                        enableChapterRename,
                        renamePattern,
                        customRegex
                    )
                    return@launch
                }

                if (existingBook != null) {
                    withContext(Dispatchers.Main) {
                        overwriteConfirmState.value = OverwriteConfirmState(
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

                performImport(
                    state.uri,
                    finalBookName,
                    splitMethod,
                    noSplit,
                    false,
                    wordsPerChapter,
                    finalCategory,
                    enableChapterMerge,
                    mergeMinWords,
                    enableChapterRename,
                    renamePattern,
                    customRegex
                )
            }
        }
    }

    fun cancelOverwriteConfirm() {
        overwriteConfirmState.value = null
    }

    fun confirmOverwrite() {
        val overwriteState = overwriteConfirmState.value ?: return
        overwriteConfirmState.value = null

        scope.launch(Dispatchers.IO) {
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
            val context = application.applicationContext
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
        renamePattern: String = "",
        customRegex: String = ""
    ) {
        importingState.value = ImportingState(bookName = finalBookName)
        val context = application.applicationContext

        try {
            importingState.update { it?.copy(statusText = "正在识别文件格式...") }
            val fileFormat = detectFileFormat(context, uri)

            when (fileFormat) {
                "nvb" -> importNvbFile(
                    context,
                    uri,
                    finalBookName,
                    noSplit,
                    selectedCategory,
                    enableChapterMerge,
                    mergeMinWords,
                    enableChapterRename,
                    renamePattern
                )

                "epub" -> importEpubFile(
                    context,
                    uri,
                    finalBookName,
                    noSplit,
                    selectedCategory,
                    enableChapterMerge,
                    mergeMinWords,
                    enableChapterRename,
                    renamePattern
                )

                else -> importTxtFile(
                    context,
                    uri,
                    finalBookName,
                    splitMethod,
                    noSplit,
                    wordsPerChapter,
                    selectedCategory,
                    customRegex
                )
            }

            withContext(Dispatchers.Main) {
                importingState.value = null
                onBooksChanged()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                importingState.update {
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

    private fun applyRenamePattern(chapterName: String, pattern: String): String {
        if (pattern.isBlank()) return chapterName

        return try {
            val parts = pattern.split(" -> ", limit = 2)
            if (parts.size != 2) return chapterName

            val findPattern = parts[0].trim()
            val replaceText = parts[1].trim()

            val regex = Regex(findPattern)
            regex.replace(chapterName) { matchResult ->
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
            chapterName
        }
    }

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
                val lastContent =
                    com.bandbbs.ebook.utils.ChapterContentManager.readChapterContent(lastChapter.contentFilePath)
                val currentContent =
                    com.bandbbs.ebook.utils.ChapterContentManager.readChapterContent(currentChapter.contentFilePath)
                val mergedContent =
                    lastContent.trimEnd() + "\n\n" + currentChapter.name + "\n\n" + currentContent.trimStart()

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

    private suspend fun importNvbFile(
        context: Context,
        uri: Uri,
        finalBookName: String,
        noSplit: Boolean,
        selectedCategory: String? = null,
        enableChapterMerge: Boolean = false,
        mergeMinWords: Int = 500,
        enableChapterRename: Boolean = false,
        renamePattern: String = ""
    ) {
        importingState.update { it?.copy(statusText = "正在解析 NVB 文件...", progress = 0.1f) }
        val nvbBook = NvbParser.parse(context, uri)

        val existingBook = db.bookDao().getBookByName(finalBookName)

        importingState.update { it?.copy(statusText = "正在复制文件...", progress = 0.3f) }
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
                importingState.update {
                    it?.copy(
                        statusText = "检测到已存在的书籍，准备更新...",
                        progress = 0.5f
                    )
                }

                if (coverImagePath != null) {
                    db.bookDao().update(
                        existingBook.copy(
                            size = destFile.length(),
                            coverImagePath = coverImagePath
                        )
                    )
                } else {
                    db.bookDao().update(existingBook.copy(size = destFile.length()))
                }
                existingBook.id.toLong()
            } else {
                importingState.update {
                    it?.copy(
                        statusText = "正在写入数据库...",
                        progress = 0.5f
                    )
                }
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

            importingState.update { it?.copy(statusText = "正在导入章节...", progress = 0.7f) }

            val existingChapters = db.chapterDao().getChapterInfoForBook(bookId.toInt())
            val existingChapterNames = existingChapters.map { it.name }.toSet()

            
            var processedChapters = nvbBook.chapters
            var parsedBookInfo: BookInfoParser.ParsedBookInfo? = null
            
            if (!noSplit && processedChapters.isNotEmpty() && 
                (processedChapters[0].title == "简介" || processedChapters[0].title == "介绍")) {
                importingState.update { 
                    it?.copy(statusText = "正在解析书籍信息...", progress = 0.65f) 
                }
                
                val introContent = processedChapters[0].content
                parsedBookInfo = BookInfoParser.parseIntroductionContent(introContent)
                
                if (parsedBookInfo != null) {
                    
                    val bookEntity = db.bookDao().getBookByPath(destFile.absolutePath)
                    if (bookEntity != null) {
                        val updatedEntity = bookEntity.copy(
                            author = parsedBookInfo.author ?: bookEntity.author,
                            summary = parsedBookInfo.summary ?: bookEntity.summary,
                            bookStatus = parsedBookInfo.status ?: bookEntity.bookStatus,
                            category = parsedBookInfo.tags ?: bookEntity.category
                        )
                        db.bookDao().update(updatedEntity)
                    }
                    
                    
                    processedChapters = processedChapters.drop(1)
                }
            }

            val chapters = if (noSplit) {
                val allContent = processedChapters.joinToString("\n\n") { chapter ->
                    "${chapter.title}\n\n${chapter.content}"
                }
                val totalWordCount = processedChapters.sumOf { it.wordCount }
                val contentFilePath =
                    com.bandbbs.ebook.utils.ChapterContentManager.saveChapterContent(
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

                processedChapters.forEach { nvbChapter ->
                    if (existingBook == null || nvbChapter.title !in existingChapterNames) {
                        var chapterName = nvbChapter.title
                        if (enableChapterRename) {
                            chapterName = applyRenamePattern(chapterName, renamePattern)
                        }

                        val contentFilePath =
                            com.bandbbs.ebook.utils.ChapterContentManager.saveChapterContent(
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
                    importingState.update {
                        it?.copy(
                            statusText = "正在合并短章节...",
                            progress = 0.85f
                        )
                    }
                    processedChapters = mergeShortChapters(
                        context,
                        bookId.toInt(),
                        processedChapters,
                        mergeMinWords
                    )
                }

                processedChapters
            }

            var finalChapters: List<Chapter> = chapters
            if (enableChapterMerge && !noSplit && finalChapters.isNotEmpty()) {
                importingState.update {
                    it?.copy(
                        statusText = "正在合并短章节...",
                        progress = 0.85f
                    )
                }
                finalChapters =
                    mergeShortChapters(context, bookId.toInt(), finalChapters, mergeMinWords)
            }

            importingState.update {
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

    private suspend fun importEpubFile(
        context: Context,
        uri: Uri,
        finalBookName: String,
        noSplit: Boolean,
        selectedCategory: String? = null,
        enableChapterMerge: Boolean = false,
        mergeMinWords: Int = 500,
        enableChapterRename: Boolean = false,
        renamePattern: String = ""
    ) {
        importingState.update { it?.copy(statusText = "正在解析 EPUB 文件...", progress = 0.1f) }
        val epubBook = EpubParser.parse(context, uri)

        val existingBook = db.bookDao().getBookByName(finalBookName)

        importingState.update { it?.copy(statusText = "正在复制文件...", progress = 0.3f) }
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
                importingState.update {
                    it?.copy(
                        statusText = "检测到已存在的书籍，准备更新...",
                        progress = 0.5f
                    )
                }

                if (coverImagePath != null) {
                    db.bookDao().update(
                        existingBook.copy(
                            size = destFile.length(),
                            coverImagePath = coverImagePath
                        )
                    )
                } else {
                    db.bookDao().update(existingBook.copy(size = destFile.length()))
                }
                existingBook.id.toLong()
            } else {
                importingState.update {
                    it?.copy(
                        statusText = "正在写入数据库...",
                        progress = 0.5f
                    )
                }
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

            importingState.update { it?.copy(statusText = "正在导入章节...", progress = 0.7f) }

            val existingChapters = db.chapterDao().getChapterInfoForBook(bookId.toInt())
            val existingChapterNames = existingChapters.map { it.name }.toSet()

            
            var processedEpubChapters = epubBook.chapters
            var parsedBookInfo: BookInfoParser.ParsedBookInfo? = null
            
            if (!noSplit && processedEpubChapters.isNotEmpty() && 
                (processedEpubChapters[0].title == "简介" || processedEpubChapters[0].title == "介绍")) {
                importingState.update { 
                    it?.copy(statusText = "正在解析书籍信息...", progress = 0.65f) 
                }
                
                val introContent = processedEpubChapters[0].content
                parsedBookInfo = BookInfoParser.parseIntroductionContent(introContent)
                
                if (parsedBookInfo != null) {
                    
                    val bookEntity = db.bookDao().getBookByPath(destFile.absolutePath)
                    if (bookEntity != null) {
                        val updatedEntity = bookEntity.copy(
                            author = parsedBookInfo.author ?: bookEntity.author,
                            summary = parsedBookInfo.summary ?: bookEntity.summary,
                            bookStatus = parsedBookInfo.status ?: bookEntity.bookStatus,
                            category = parsedBookInfo.tags ?: bookEntity.category
                        )
                        db.bookDao().update(updatedEntity)
                    }
                    
                    
                    processedEpubChapters = processedEpubChapters.drop(1)
                }
            }

            val chapters = if (noSplit) {
                val allContent = processedEpubChapters.joinToString("\n\n") { chapter ->
                    "${chapter.title}\n\n${chapter.content}"
                }
                val totalWordCount = processedEpubChapters.sumOf { it.wordCount }
                val contentFilePath =
                    com.bandbbs.ebook.utils.ChapterContentManager.saveChapterContent(
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

                processedEpubChapters.forEach { epubChapter ->
                    if (existingBook == null || epubChapter.title !in existingChapterNames) {
                        var chapterName = epubChapter.title
                        if (enableChapterRename) {
                            chapterName = applyRenamePattern(chapterName, renamePattern)
                        }

                        val contentFilePath =
                            com.bandbbs.ebook.utils.ChapterContentManager.saveChapterContent(
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
                    importingState.update {
                        it?.copy(
                            statusText = "正在合并短章节...",
                            progress = 0.85f
                        )
                    }
                    processedChapters = mergeShortChapters(
                        context,
                        bookId.toInt(),
                        processedChapters,
                        mergeMinWords
                    )
                }

                processedChapters
            }

            var finalChapters: List<Chapter> = chapters
            if (enableChapterMerge && !noSplit && finalChapters.isNotEmpty()) {
                importingState.update {
                    it?.copy(
                        statusText = "正在合并短章节...",
                        progress = 0.85f
                    )
                }
                finalChapters =
                    mergeShortChapters(context, bookId.toInt(), finalChapters, mergeMinWords)
            }

            importingState.update {
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
        selectedCategory: String? = null,
        customRegex: String = ""
    ) {
        UritoFile(uri, context)?.let { sourceFile ->
            importingState.update { it?.copy(statusText = "正在复制文件...") }
            val destFile = File(booksDir, sourceFile.name)
            sourceFile.copyTo(destFile, overwrite = true)

            importingState.update { it?.copy(statusText = "正在写入数据库...") }
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
                importingState.update { it?.copy(statusText = "正在读取全文...", progress = 0.5f) }
                val content = ChapterSplitter.readTextFromUri(context, uri)
                val contentFilePath =
                    com.bandbbs.ebook.utils.ChapterContentManager.saveChapterContent(
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
                ChapterSplitter.split(
                    context,
                    uri,
                    bookId.toInt(),
                    splitMethod,
                    { progress, status ->
                        importingState.update {
                            it?.copy(
                                statusText = status,
                                progress = progress
                            )
                        }
                    },
                    wordsPerChapter,
                    if (splitMethod == ChapterSplitter.METHOD_CUSTOM) customRegex else null
                )
            }

            importingState.update { it?.copy(statusText = "正在后处理章节...", progress = 0.9f) }
            val finalChapters = mutableListOf<Chapter>()
            val mergedChapterTitles = mutableListOf<String>()

            for (chapter in initialChapters) {
                val chapterContent =
                    com.bandbbs.ebook.utils.ChapterContentManager.readChapterContent(chapter.contentFilePath)
                if (chapter.wordCount == 0 && chapterContent.isBlank()) {
                    if (finalChapters.isNotEmpty()) {
                        val lastChapter = finalChapters.last()
                        val lastContent =
                            com.bandbbs.ebook.utils.ChapterContentManager.readChapterContent(
                                lastChapter.contentFilePath
                            )
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

            importingState.update { it?.copy(statusText = "正在保存章节...", progress = 1.0f) }
            db.chapterDao().insertAll(reIndexedChapters)

            sourceFile.delete()

            if (mergedChapterTitles.isNotEmpty()) {
                val reportMessage =
                    "有 ${mergedChapterTitles.size} 个章节因内容为空，其标题已被合并到上一章节末尾或被跳过:\n\n" +
                            mergedChapterTitles.joinToString("\n") { "- $it" }
                withContext(Dispatchers.Main) {
                    importReportState.value = ImportReportState(
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
        importReportState.value = null
    }
}

