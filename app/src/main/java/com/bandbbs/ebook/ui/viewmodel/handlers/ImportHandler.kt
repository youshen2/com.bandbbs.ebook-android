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
import com.bandbbs.ebook.utils.ChapterContentManager
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
import java.util.zip.ZipInputStream

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

    private val prefs = application.getSharedPreferences("ebook_prefs", Context.MODE_PRIVATE)
    private val LAST_SPLIT_METHOD_KEY = "last_split_method"
    private val LAST_CUSTOM_REGEX_KEY = "last_custom_regex"

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
                    if (isWordFile(context, uri)) {
                        withContext(Dispatchers.Main) {
                            importingState.value = ImportingState(
                                bookName = sourceFile.nameWithoutExtension,
                                statusText = "${sourceFile.name} 不支持的文件格式\n禁止导入 WORD 格式（DOC、DOCX 等）",
                                progress = 0f
                            )
                        }
                        delay(2000)
                        return@let
                    }

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

            val defaultSplitMethod =
                prefs.getString(LAST_SPLIT_METHOD_KEY, ChapterSplitter.METHOD_DEFAULT)
                    ?: ChapterSplitter.METHOD_DEFAULT
            val savedCustomRegex = prefs.getString(LAST_CUSTOM_REGEX_KEY, "") ?: ""

            withContext(Dispatchers.Main) {
                importState.value = com.bandbbs.ebook.ui.viewmodel.ImportState(
                    uris = validFiles.map { it.uri },
                    files = validFiles,
                    splitMethod = defaultSplitMethod,
                    customRegex = savedCustomRegex
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

        if (!noSplit && splitMethod != ChapterSplitter.METHOD_DEFAULT) {
            prefs.edit().putString(LAST_SPLIT_METHOD_KEY, splitMethod).apply()
        }
        if (!noSplit && splitMethod == ChapterSplitter.METHOD_CUSTOM && customRegex.isNotBlank()) {
            prefs.edit().putString(LAST_CUSTOM_REGEX_KEY, customRegex).apply()
        }

        scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                importState.value = null
            }
            val finalCategory = selectedCategory ?: state.selectedCategory

            if (state.isMultipleFiles) {

                state.files.forEach { fileInfo ->
                    val finalBookName = fileInfo.bookName.trim()
                    if (finalBookName.isNotEmpty()) {
                        val existingBook = booksState.value.find { it.name == finalBookName }
                        val context = application.applicationContext
                        val fileFormat = detectFileFormat(context, fileInfo.uri)

                        if (existingBook != null && (fileFormat == "epub" || fileFormat == "nvb")) {

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

                    }
                }
            } else {

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
            ChapterContentManager.deleteBookChapters(context, bookEntity.id)
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

    /**
     * 清理并合并空章节 (0字或空内容)
     * 这是强制执行的逻辑，不管设置如何
     */
    private suspend fun cleanAndMergeChapters(
        context: Context,
        bookId: Int,
        chapters: List<Chapter>
    ): Pair<List<Chapter>, List<String>> {
        if (chapters.isEmpty()) return Pair(emptyList(), emptyList())

        val cleanedChapters = mutableListOf<Chapter>()
        val mergedTitles = mutableListOf<String>()

        for (chapter in chapters) {
            val content = ChapterContentManager.readChapterContent(chapter.contentFilePath)

            if (chapter.wordCount == 0 && content.isBlank()) {
                if (cleanedChapters.isNotEmpty()) {
                    val lastChapter = cleanedChapters.last()
                    val lastContent = ChapterContentManager.readChapterContent(lastChapter.contentFilePath)

                    val mergedContent = lastContent.trimEnd() + "\n\n" + chapter.name.trim()

                    ChapterContentManager.saveChapterContent(
                        context, bookId, lastChapter.index, mergedContent
                    )

                    cleanedChapters[cleanedChapters.size - 1] = lastChapter.copy(
                        wordCount = mergedContent.length
                    )

                    ChapterContentManager.deleteChapterContent(chapter.contentFilePath)
                    mergedTitles.add(chapter.name)
                } else {
                    ChapterContentManager.deleteChapterContent(chapter.contentFilePath)
                    mergedTitles.add("${chapter.name} (首章为空，已跳过)")
                }
            } else {
                cleanedChapters.add(chapter)
            }
        }

        val reIndexedChapters = cleanedChapters.mapIndexed { index, chapter ->
            chapter.copy(index = index)
        }

        return Pair(reIndexedChapters, mergedTitles)
    }

    private fun isWordFile(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val header = ByteArray(8)
                val read = inputStream.read(header)
                if (read < 8) return false

                val docMagic = byteArrayOf(
                    0xD0.toByte(),
                    0xCF.toByte(),
                    0x11.toByte(),
                    0xE0.toByte(),
                    0xA1.toByte(),
                    0xB1.toByte(),
                    0x1A.toByte(),
                    0xE1.toByte()
                )
                if (header.contentEquals(docMagic)) {
                    return true
                }

                val zipMagic =
                    byteArrayOf(0x50.toByte(), 0x4B.toByte(), 0x03.toByte(), 0x04.toByte())
                if (header.take(4).toByteArray().contentEquals(zipMagic)) {
                    context.contentResolver.openInputStream(uri)?.use { zipStream ->
                        ZipInputStream(zipStream).use { zip ->
                            var entry = zip.nextEntry
                            var checkedCount = 0
                            while (entry != null && checkedCount < 20) {
                                if (entry.name == "word/document.xml" || entry.name.startsWith("word/")) {
                                    return true
                                }
                                zip.closeEntry()
                                entry = zip.nextEntry
                                checkedCount++
                            }
                        }
                    }
                }
                false
            } ?: false
        } catch (e: Exception) {
            false
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
                    ChapterContentManager.readChapterContent(lastChapter.contentFilePath)
                val currentContent =
                    ChapterContentManager.readChapterContent(currentChapter.contentFilePath)
                val mergedContent =
                    lastContent.trimEnd() + "\n\n" + currentChapter.name + "\n\n" + currentContent.trimStart()

                ChapterContentManager.saveChapterContent(
                    context, bookId, lastChapter.index, mergedContent
                )
                ChapterContentManager.deleteChapterContent(currentChapter.contentFilePath)

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
                (processedChapters[0].title == "简介" || processedChapters[0].title == "介绍")
            ) {
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
                    ChapterContentManager.saveChapterContent(
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
                            ChapterContentManager.saveChapterContent(
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

                var processedList: List<Chapter> = newChapters
                if (enableChapterMerge && processedList.isNotEmpty()) {
                    importingState.update {
                        it?.copy(
                            statusText = "正在合并短章节...",
                            progress = 0.85f
                        )
                    }
                    processedList = mergeShortChapters(
                        context,
                        bookId.toInt(),
                        processedList,
                        mergeMinWords
                    )
                }

                processedList
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

            importingState.update { it?.copy(statusText = "正在清理空章节...", progress = 0.88f) }
            val (cleanedChapters, mergedTitles) = cleanAndMergeChapters(context, bookId.toInt(), finalChapters)

            importingState.update {
                it?.copy(
                    statusText = if (existingBook != null) "正在保存新章节 (${cleanedChapters.size} 章)..." else "正在保存章节...",
                    progress = 0.9f
                )
            }
            if (cleanedChapters.isNotEmpty()) {
                db.chapterDao().insertAll(cleanedChapters)
            }

            if (mergedTitles.isNotEmpty()) {
                val reportMessage =
                    "有 ${mergedTitles.size} 个章节因内容为空，已自动合并到上一章:\n\n" +
                            mergedTitles.joinToString("\n") { "- $it" }
                withContext(Dispatchers.Main) {
                    importReportState.value = ImportReportState(
                        bookName = finalBookName,
                        mergedChaptersInfo = reportMessage
                    )
                }
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
                (processedEpubChapters[0].title == "简介" || processedEpubChapters[0].title == "介绍")
            ) {
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
                    ChapterContentManager.saveChapterContent(
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
                            ChapterContentManager.saveChapterContent(
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

                var processedList: List<Chapter> = newChapters
                if (enableChapterMerge && processedList.isNotEmpty()) {
                    importingState.update {
                        it?.copy(
                            statusText = "正在合并短章节...",
                            progress = 0.85f
                        )
                    }
                    processedList = mergeShortChapters(
                        context,
                        bookId.toInt(),
                        processedList,
                        mergeMinWords
                    )
                }

                processedList
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

            importingState.update { it?.copy(statusText = "正在清理空章节...", progress = 0.88f) }
            val (cleanedChapters, mergedTitles) = cleanAndMergeChapters(context, bookId.toInt(), finalChapters)

            importingState.update {
                it?.copy(
                    statusText = if (existingBook != null) "正在保存新章节 (${cleanedChapters.size} 章)..." else "正在保存章节...",
                    progress = 0.9f
                )
            }
            if (cleanedChapters.isNotEmpty()) {
                db.chapterDao().insertAll(cleanedChapters)
            }

            if (mergedTitles.isNotEmpty()) {
                val reportMessage =
                    "有 ${mergedTitles.size} 个章节因内容为空，已自动合并到上一章:\n\n" +
                            mergedTitles.joinToString("\n") { "- $it" }
                withContext(Dispatchers.Main) {
                    importReportState.value = ImportReportState(
                        bookName = finalBookName,
                        mergedChaptersInfo = reportMessage
                    )
                }
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
                    ChapterContentManager.saveChapterContent(
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

            val (cleanedChapters, mergedTitles) = cleanAndMergeChapters(context, bookId.toInt(), initialChapters)

            importingState.update { it?.copy(statusText = "正在保存章节...", progress = 1.0f) }
            db.chapterDao().insertAll(cleanedChapters)

            sourceFile.delete()

            if (mergedTitles.isNotEmpty()) {
                val reportMessage =
                    "有 ${mergedTitles.size} 个章节因内容为空，其标题已被合并到上一章节末尾或被跳过:\n\n" +
                            mergedTitles.joinToString("\n") { "- $it" }
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
