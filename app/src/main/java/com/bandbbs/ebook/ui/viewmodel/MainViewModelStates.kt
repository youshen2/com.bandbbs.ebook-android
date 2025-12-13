package com.bandbbs.ebook.ui.viewmodel

import android.net.Uri
import com.bandbbs.ebook.database.ChapterInfo
import com.bandbbs.ebook.ui.model.Book

data class ConnectionState(
    val statusText: String = "手环连接中",
    val descriptionText: String = "请确保小米运动健康后台运行",
    val isConnected: Boolean = false,
    val deviceName: String? = null
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

data class ImportFileInfo(
    val uri: Uri,
    val bookName: String,
    val fileSize: Long,
    val fileFormat: String
)

data class ImportState(
    val uris: List<Uri>,
    val files: List<ImportFileInfo>,
    val splitMethod: String = com.bandbbs.ebook.utils.ChapterSplitter.METHOD_DEFAULT,
    val noSplit: Boolean = false,
    val wordsPerChapter: Int = 5000,
    val selectedCategory: String? = null,
    val enableChapterMerge: Boolean = false,
    val mergeMinWords: Int = 500,
    val enableChapterRename: Boolean = false,
    val renamePattern: String = "",
    val customRegex: String = ""
) {
    
    val uri: Uri get() = uris.first()
    val bookName: String get() = files.first().bookName
    val fileSize: Long get() = files.first().fileSize
    val fileFormat: String get() = files.first().fileFormat
    
    val isMultipleFiles: Boolean get() = uris.size > 1
}

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

data class CategoryState(
    val categories: List<String>,
    val selectedCategory: String?,
    val book: Book?,
    val onCategorySelectedForEdit: ((String?) -> Unit)? = null
)

data class EditBookInfoState(
    val book: com.bandbbs.ebook.database.BookEntity,
    val isResyncing: Boolean = false
)

enum class SyncMode {
    AUTO,      
    BAND_ONLY, 
    PHONE_ONLY 
}

data class SyncReadingDataState(
    val isSyncing: Boolean = false,
    val statusText: String = "",
    val progress: Float = 0f,
    val currentBook: String = "",
    val totalBooks: Int = 0,
    val syncedBooks: Int = 0,
    val showModeDialog: Boolean = false,
    val progressSyncMode: SyncMode = SyncMode.AUTO,
    val readingTimeSyncMode: SyncMode = SyncMode.AUTO
)

data class VersionIncompatibleState(
    val currentVersion: Int,
    val requiredVersion: Int
)

data class UpdateCheckState(
    val isChecking: Boolean = false,
    val updateInfo: com.bandbbs.ebook.utils.VersionChecker.UpdateInfo? = null,
    val updateInfoList: List<com.bandbbs.ebook.utils.VersionChecker.UpdateInfo> = emptyList(),
    val errorMessage: String? = null,
    val deviceName: String? = null,
    val showSheet: Boolean = false,
    val isAutoCheck: Boolean = false
)

data class IpCollectionPermissionState(
    val showSheet: Boolean = false,
    val isFirstTime: Boolean = true
)
