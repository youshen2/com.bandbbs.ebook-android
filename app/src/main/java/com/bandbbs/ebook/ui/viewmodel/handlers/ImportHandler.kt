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
import com.bandbbs.ebook.ui.viewmodel.LargeChapterWarningState
import com.bandbbs.ebook.ui.viewmodel.OverwriteConfirmState
import com.bandbbs.ebook.ui.viewmodel.RegexTemplate
import com.bandbbs.ebook.utils.BookInfoParser
import com.bandbbs.ebook.utils.ChapterContentManager
import com.bandbbs.ebook.utils.ChapterSplitter
import com.bandbbs.ebook.utils.DocxParser
import com.bandbbs.ebook.utils.EpubParser
import com.bandbbs.ebook.utils.MobiParser
import com.bandbbs.ebook.utils.NvbParser
import com.bandbbs.ebook.utils.PdfParser
import com.bandbbs.ebook.utils.ReadingTimeStorage
import com.bandbbs.ebook.utils.UritoFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
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
    private val largeChapterWarningState: MutableStateFlow<LargeChapterWarningState?>,
    private val onBooksChanged: () -> Unit,
) {

    private val prefs = application.getSharedPreferences("ebook_prefs", Context.MODE_PRIVATE)
    private val LAST_SPLIT_METHOD_KEY = "last_split_method"
    private val LAST_CUSTOM_REGEX_KEY = "last_custom_regex"
    private val CUSTOM_REGEX_TEMPLATES_KEY = "custom_regex_templates"
    private val LAST_CUSTOM_REGEX_TEMPLATE_NAME_KEY = "last_custom_regex_template_name"

    fun startImport(uri: Uri) {
        startImportBatch(listOf(uri))
    }

    fun startImportBatch(uris: List<Uri>) {
        scope.launch {
            val context = application.applicationContext
            val validFiles = mutableListOf<com.bandbbs.ebook.ui.viewmodel.ImportFileInfo>()
            val allowedExtensions = listOf(".txt", ".epub", ".nvb", ".docx", ".pdf", ".mobi")

            uris.forEach { uri ->
                UritoFile(uri, context)?.let { sourceFile ->
                    if (isDocFile(context, uri)) {
                        withContext(Dispatchers.Main) {
                            importingState.value = ImportingState(
                                bookName = sourceFile.nameWithoutExtension,
                                statusText = "${sourceFile.name} 不支持的文件格式\n禁止导入 DOC 格式",
                                progress = 0f
                            )
                        }
                        delay(2000)
                        return@let
                    }

                    val fileName = sourceFile.name.lowercase()
                    val hasValidExtension = allowedExtensions.any { fileName.endsWith(it) }
                    val fileFormat = detectFileFormat(context, uri)
                    val isRecognizedFormat =
                        fileFormat == "epub" || fileFormat == "nvb" || fileFormat == "docx" || fileFormat == "pdf" || fileFormat == "mobi"

                    if (hasValidExtension || isRecognizedFormat) {
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
                                statusText = "${sourceFile.name} 不支持的文件格式\n仅支持 TXT、EPUB、NVB、DOCX、PDF、MOBI 格式",
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
            val templates = loadCustomRegexTemplates()
            val lastTemplateName = prefs.getString(LAST_CUSTOM_REGEX_TEMPLATE_NAME_KEY, null)
            val selectedTemplateName =
                lastTemplateName?.takeIf { name -> templates.any { it.name == name } }

            withContext(Dispatchers.Main) {
                importState.value = ImportState(
                    uris = validFiles.map { it.uri },
                    files = validFiles,
                    splitMethod = defaultSplitMethod,
                    customRegex = savedCustomRegex,
                    customRegexTemplates = templates,
                    selectedCustomRegexTemplateName = selectedTemplateName
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
        customRegex: String = "",
        selectedCustomRegexTemplateName: String? = null,
        customRegexTemplates: List<RegexTemplate> = emptyList()
    ) {
        val state = importState.value ?: return

        if (!noSplit && splitMethod != ChapterSplitter.METHOD_DEFAULT) {
            prefs.edit().putString(LAST_SPLIT_METHOD_KEY, splitMethod).apply()
        }
        if (!noSplit && splitMethod == ChapterSplitter.METHOD_CUSTOM && customRegex.isNotBlank()) {
            prefs.edit().putString(LAST_CUSTOM_REGEX_KEY, customRegex).apply()
        }
        saveCustomRegexTemplates(customRegexTemplates)
        prefs.edit().putString(LAST_CUSTOM_REGEX_TEMPLATE_NAME_KEY, selectedCustomRegexTemplateName).apply()

        scope.launch(Dispatchers.IO) {
            val finalCategory = selectedCategory ?: state.selectedCategory

            val oversizedChapterMessage = findOversizedChapterWarningMessage(
                state = state,
                splitMethod = splitMethod,
                noSplit = noSplit,
                wordsPerChapter = wordsPerChapter,
                customRegex = customRegex
            )
            if (oversizedChapterMessage != null) {
                withContext(Dispatchers.Main) {
                    largeChapterWarningState.value = LargeChapterWarningState(
                        message = oversizedChapterMessage
                    )
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                importState.value = null
            }

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
            val readerPrefs =
                application.getSharedPreferences("chapter_reader_prefs", Context.MODE_PRIVATE)
            readerPrefs.edit().remove("last_read_chapter_${bookEntity.id}").apply()
            ReadingTimeStorage.clearReadingTime(context, bookEntity.name)
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

                "docx" -> importDocxFile(
                    context,
                    uri,
                    finalBookName,
                    splitMethod,
                    noSplit,
                    wordsPerChapter,
                    selectedCategory,
                    customRegex
                )

                "pdf" -> importPdfFile(
                    context,
                    uri,
                    finalBookName,
                    splitMethod,
                    noSplit,
                    wordsPerChapter,
                    selectedCategory,
                    customRegex
                )

                "mobi" -> importMobiFile(
                    context,
                    uri,
                    finalBookName,
                    splitMethod,
                    noSplit,
                    wordsPerChapter,
                    selectedCategory,
                    customRegex
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
                    val lastContent =
                        ChapterContentManager.readChapterContent(lastChapter.contentFilePath)

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

    private fun isDocFile(context: Context, uri: Uri): Boolean {
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
                header.contentEquals(docMagic)
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    private fun detectFileFormat(context: Context, uri: Uri): String {
        return when {
            NvbParser.isNvbFile(context, uri) -> "nvb"
            EpubParser.isEpubFile(context, uri) -> "epub"
            DocxParser.isDocxFile(context, uri) -> "docx"
            PdfParser.isPdfFile(context, uri) -> "pdf"
            MobiParser.isMobiFile(context, uri) -> "mobi"
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
            val (cleanedChapters, mergedTitles) = cleanAndMergeChapters(
                context,
                bookId.toInt(),
                finalChapters
            )

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
            val (cleanedChapters, mergedTitles) = cleanAndMergeChapters(
                context,
                bookId.toInt(),
                finalChapters
            )

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

            val (cleanedChapters, mergedTitles) = cleanAndMergeChapters(
                context,
                bookId.toInt(),
                initialChapters
            )

            importingState.update { it?.copy(statusText = "正在保存章节...", progress = 1.0f) }
            db.chapterDao().insertAll(cleanedChapters)

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

    private suspend fun importDocxFile(
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
            importingState.update {
                it?.copy(
                    statusText = "正在解析 DOCX 文件...",
                    progress = 0.1f
                )
            }
            val content = DocxParser.extractPlainText(context, uri)

            importingState.update { it?.copy(statusText = "正在复制文件...", progress = 0.3f) }
            val destFile = File(booksDir, sourceFile.name)
            sourceFile.copyTo(destFile, overwrite = true)

            importingState.update { it?.copy(statusText = "正在写入数据库...", progress = 0.5f) }
            val bookId = db.bookDao().insert(
                BookEntity(
                    name = finalBookName,
                    path = destFile.absolutePath,
                    size = destFile.length(),
                    format = "docx",
                    localCategory = selectedCategory
                )
            )

            val initialChapters = if (noSplit) {
                importingState.update { it?.copy(statusText = "正在读取全文...", progress = 0.7f) }
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
                ChapterSplitter.splitFromText(
                    context = context,
                    content = content,
                    bookId = bookId.toInt(),
                    method = splitMethod,
                    onProgress = { progress, status ->
                        importingState.update {
                            it?.copy(
                                statusText = status,
                                progress = progress
                            )
                        }
                    },
                    wordsPerChapter = wordsPerChapter,
                    customRegex = if (splitMethod == ChapterSplitter.METHOD_CUSTOM) customRegex else null
                )
            }

            importingState.update { it?.copy(statusText = "正在后处理章节...", progress = 0.9f) }

            val (cleanedChapters, mergedTitles) = cleanAndMergeChapters(
                context,
                bookId.toInt(),
                initialChapters
            )

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

    private suspend fun importMobiFile(
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
            importingState.update {
                it?.copy(
                    statusText = "正在解析 MOBI 文件...",
                    progress = 0.1f
                )
            }

            val chaptersFromMobi = if (!noSplit && splitMethod == ChapterSplitter.METHOD_DEFAULT) {
                MobiParser.extractChapters(context, uri) { p, status ->
                    importingState.update {
                        it?.copy(
                            statusText = status,
                            progress = 0.1f + 0.25f * p.coerceIn(0f, 1f)
                        )
                    }
                }
            } else {
                emptyList()
            }

            val content = if (chaptersFromMobi.isNotEmpty()) {
                chaptersFromMobi.joinToString("\n\n") { it.title + "\n\n" + it.content }
            } else {
                MobiParser.extractPlainText(context, uri) { p, status ->
                    importingState.update {
                        it?.copy(
                            statusText = status,
                            progress = 0.1f + 0.25f * p.coerceIn(0f, 1f)
                        )
                    }
                }
            }

            importingState.update { it?.copy(statusText = "正在复制文件...", progress = 0.3f) }
            val destFile = File(booksDir, sourceFile.name)
            sourceFile.copyTo(destFile, overwrite = true)

            importingState.update { it?.copy(statusText = "正在写入数据库...", progress = 0.5f) }
            val bookId = db.bookDao().insert(
                BookEntity(
                    name = finalBookName,
                    path = destFile.absolutePath,
                    size = destFile.length(),
                    format = "mobi",
                    localCategory = selectedCategory
                )
            )

            val initialChapters = if (noSplit) {
                importingState.update { it?.copy(statusText = "正在读取全文...", progress = 0.7f) }
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
            } else if (chaptersFromMobi.isNotEmpty()) {
                importingState.update {
                    it?.copy(
                        statusText = "正在保存章节 (0/${chaptersFromMobi.size})...",
                        progress = 0.7f
                    )
                }

                val chapters = chaptersFromMobi.mapIndexed { index, ch ->
                    if (index == 0 || index % 10 == 0 || index == chaptersFromMobi.lastIndex) {
                        val p = 0.7f + 0.2f * (index.toFloat() / chaptersFromMobi.size.toFloat())
                        importingState.update {
                            it?.copy(
                                statusText = "正在保存章节 (${index + 1}/${chaptersFromMobi.size})...",
                                progress = p
                            )
                        }
                    }
                    val contentFilePath = ChapterContentManager.saveChapterContent(
                        context,
                        bookId.toInt(),
                        index,
                        ch.content.trim()
                    )
                    Chapter(
                        bookId = bookId.toInt(),
                        index = index,
                        name = ch.title.trim().ifBlank { "第${index + 1}章" },
                        contentFilePath = contentFilePath,
                        wordCount = ch.wordCount
                    )
                }
                chapters
            } else {
                ChapterSplitter.splitFromText(
                    context = context,
                    content = content,
                    bookId = bookId.toInt(),
                    method = splitMethod,
                    onProgress = { progress, status ->
                        importingState.update {
                            it?.copy(
                                statusText = status,
                                progress = progress
                            )
                        }
                    },
                    wordsPerChapter = wordsPerChapter,
                    customRegex = if (splitMethod == ChapterSplitter.METHOD_CUSTOM) customRegex else null
                )
            }

            importingState.update { it?.copy(statusText = "正在后处理章节...", progress = 0.9f) }

            val (cleanedChapters, mergedTitles) = cleanAndMergeChapters(
                context,
                bookId.toInt(),
                initialChapters
            )

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

    private suspend fun importPdfFile(
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
            importingState.update { it?.copy(statusText = "正在复制文件...", progress = 0.3f) }
            val destFile = File(booksDir, sourceFile.name)
            sourceFile.copyTo(destFile, overwrite = true)

            importingState.update { it?.copy(statusText = "正在写入数据库...", progress = 0.5f) }
            val bookId = db.bookDao().insert(
                BookEntity(
                    name = finalBookName,
                    path = destFile.absolutePath,
                    size = destFile.length(),
                    format = "pdf",
                    localCategory = selectedCategory
                )
            )

            importingState.update { it?.copy(statusText = "正在解析页数...", progress = 0.7f) }
            val pageCount = PdfParser.getPageCount(destFile).coerceAtLeast(1)

            importingState.update { it?.copy(statusText = "正在生成页目录...", progress = 0.85f) }
            val chaptersDir = ChapterContentManager.getChaptersDir(context)
            val bookDir = File(chaptersDir, "book_${bookId.toInt()}").apply { mkdirs() }
            val chapters = (0 until pageCount).map { pageIndex ->
                val placeholderPath = File(bookDir, "pdf_page_${pageIndex}.bin").absolutePath
                Chapter(
                    bookId = bookId.toInt(),
                    index = pageIndex,
                    name = "第${pageIndex + 1}页",
                    contentFilePath = placeholderPath,
                    wordCount = 0
                )
            }

            importingState.update { it?.copy(statusText = "正在保存目录...", progress = 1.0f) }
            db.chapterDao().insertAll(chapters)

            sourceFile.delete()
        } ?: run {
            throw IllegalArgumentException("无法读取文件")
        }
    }

    private fun loadCustomRegexTemplates(): List<RegexTemplate> {
        val savedTemplates = runCatching {
            val raw = prefs.getString(CUSTOM_REGEX_TEMPLATES_KEY, null) ?: return@runCatching emptyList()
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val name = obj.optString("name").trim()
                    val regex = obj.optString("regex")
                    if (name.isNotBlank() && regex.isNotBlank()) {
                        add(RegexTemplate(name = name, regex = regex))
                    }
                }
            }
        }.getOrElse {
            emptyList()
        }

        val defaultTemplates = defaultRegexTemplates()

        return (defaultTemplates + savedTemplates)
            .groupBy { it.name }
            .mapNotNull { (_, items) -> items.lastOrNull() }
    }

    private fun saveCustomRegexTemplates(templates: List<RegexTemplate>) {
        val defaultNames = defaultRegexTemplates().map { it.name }.toSet()
        val uniqueTemplates = templates
            .mapNotNull {
                val name = it.name.trim()
                val regex = it.regex
                if (name.isBlank() || regex.isBlank()) null else RegexTemplate(name, regex)
            }
            .distinctBy { it.name }
            .filterNot { it.name in defaultNames }

        val array = JSONArray()
        uniqueTemplates.forEach { template ->
            array.put(
                JSONObject().apply {
                    put("name", template.name)
                    put("regex", template.regex)
                }
            )
        }
        prefs.edit().putString(CUSTOM_REGEX_TEMPLATES_KEY, array.toString()).apply()
    }

    private fun defaultRegexTemplates(): List<RegexTemplate> = listOf(
        RegexTemplate(
            name = "默认（严格）",
            regex = "^(第(\\s{0,1}[一二三四五六七八九十百千万零〇\\d]+\\s{0,1})(章|卷|节|部|篇|回|本)|番外\\s{0,2}[一二三四五六七八九十百千万零〇\\d]*)(.{0,30})$"
        ),
        RegexTemplate(
            name = "默认（宽松）",
            regex = "^(\\s*[第]?(\\s*[一二三四五六七八九十百千万零〇\\d]+\\s*)(章|卷|节|部|篇|回|本)|番外\\s*[一二三四五六七八九十百千万零〇\\d]*)(.{0,30})$"
        ),
        RegexTemplate(
            name = "英文 Chapter",
            regex = "^\\s*(Chapter|CHAPTER)\\s+(\\d+)\\s*.*$"
        ),
        RegexTemplate(
            name = "中文数字序号",
            regex = "^\\s*([一二三四五六七八九十百千万零〇]+)[、.\\s]+(.*)$"
        ),
        RegexTemplate(
            name = "阿拉伯数字序号",
            regex = "^\\s*(\\d+)[、.\\s]+(.*)$"
        )
    )

    fun dismissImportReport() {
        importReportState.value = null
    }

    fun dismissLargeChapterWarning() {
        largeChapterWarningState.value = null
    }

    private suspend fun findOversizedChapterWarningMessage(
        state: ImportState,
        splitMethod: String,
        noSplit: Boolean,
        wordsPerChapter: Int,
        customRegex: String
    ): String? {
        for (file in state.files) {
            if (file.fileFormat !in listOf("txt", "docx", "mobi")) continue

            val estimatedMaxChapterBytes = estimateMaxChapterBytes(
                uri = file.uri,
                fileFormat = file.fileFormat,
                splitMethod = splitMethod,
                noSplit = noSplit,
                wordsPerChapter = wordsPerChapter,
                customRegex = customRegex
            ) ?: continue

            if (estimatedMaxChapterBytes > ChapterSplitter.MAX_CHAPTER_SIZE_BYTES) {
                return buildString {
                    append("检测到单章节内容可能超过 5MB（文件：${file.bookName}）。\n这可能会导致传输时不停重启，无法完成该过大章节传输。\n")
                    append("请尝试修改分章方式，或自行编写正则表达式，或按字数分章。")
                }
            }
        }

        return null
    }

    private suspend fun estimateMaxChapterBytes(
        uri: Uri,
        fileFormat: String,
        splitMethod: String,
        noSplit: Boolean,
        wordsPerChapter: Int,
        customRegex: String
    ): Int? {
        val context = application.applicationContext
        val textContent = when (fileFormat) {
            "txt" -> ChapterSplitter.readTextFromUri(context, uri)
            "docx" -> DocxParser.extractPlainText(context, uri)
            "mobi" -> {
                if (splitMethod == ChapterSplitter.METHOD_DEFAULT) {
                    val chapters = MobiParser.extractChapters(context, uri) { _, _ -> }
                    if (chapters.isNotEmpty()) {
                        chapters.maxOfOrNull { it.content.trim().toByteArray().size } ?: 0
                    } else {
                        val plainText = MobiParser.extractPlainText(context, uri) { _, _ -> }
                        plainText
                    }
                } else {
                    MobiParser.extractPlainText(context, uri) { _, _ -> }
                }
            }

            else -> return null
        }

        if (textContent is Int) {
            return textContent
        }

        val content = textContent as String
        if (noSplit) {
            return content.trim().toByteArray().size
        }

        return ChapterSplitter.estimateMaxChapterBytesFromText(
            content = content,
            method = splitMethod,
            wordsPerChapter = wordsPerChapter,
            customRegex = if (splitMethod == ChapterSplitter.METHOD_CUSTOM) customRegex else null
        )
    }
}
