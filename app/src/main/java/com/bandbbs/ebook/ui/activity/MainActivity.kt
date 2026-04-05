package com.bandbbs.ebook.ui.activity

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.bandbbs.ebook.App
import com.bandbbs.ebook.logic.InterHandshake
import com.bandbbs.ebook.notifications.ForegroundTransferService
import com.bandbbs.ebook.notifications.LiveNotificationManager
import com.bandbbs.ebook.ui.components.FirstSyncConfirmDialog
import com.bandbbs.ebook.ui.components.UpdateCheckDialog
import com.bandbbs.ebook.ui.screens.BandSettingsScreen
import com.bandbbs.ebook.ui.screens.BuiltinFileManagerScreen
import com.bandbbs.ebook.ui.screens.ChapterListScreen
import com.bandbbs.ebook.ui.screens.MainScreen
import com.bandbbs.ebook.ui.screens.PushScreen
import com.bandbbs.ebook.ui.screens.ReaderScreen
import com.bandbbs.ebook.ui.screens.SettingsScreen
import com.bandbbs.ebook.ui.screens.StatisticsScreen
import com.bandbbs.ebook.ui.screens.SyncOptionsScreen
import com.bandbbs.ebook.ui.theme.EbookTheme
import com.bandbbs.ebook.ui.viewmodel.MainViewModel
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.FabPosition
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Sort
import top.yukonga.miuix.kmp.icon.extended.VerticalSplit
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.pressable
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed interface Screen : NavKey {
    data object HomePager : Screen
    data object BandSettings : Screen
    data object SyncOptions : Screen
    data object Push : Screen
    data object ChapterList : Screen
    data object Reader : Screen
    data object BuiltinFileManager : Screen
}

class MainActivity : ComponentActivity() {

    private val TEST_APRIL_FOOLS = false

    private fun isAprilFools(): Boolean {
        if (TEST_APRIL_FOOLS) return true
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.MONTH) == Calendar.APRIL && calendar.get(Calendar.DAY_OF_MONTH) == 1
    }

    private val viewModel: MainViewModel by viewModels()

    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris.isNotEmpty()) {
                if (uris.size == 1) viewModel.startImport(uris[0]) else viewModel.startImportBatch(
                    uris
                )
            }
        }

    private val coverPickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { selectedUri ->
                val destUri = Uri.fromFile(
                    File(cacheDir, "cover_crop_${System.currentTimeMillis()}.jpg")
                )
                val options = UCrop.Options().apply {
                    setCompressionFormat(android.graphics.Bitmap.CompressFormat.JPEG)
                    setCompressionQuality(95)
                    setHideBottomControls(false)
                    setFreeStyleCropEnabled(true)
                }
                val uCrop = UCrop.of(selectedUri, destUri)
                    .withAspectRatio(3f, 4f)
                    .withMaxResultSize(1200, 1600)
                    .withOptions(options)
                cropCoverLauncher.launch(uCrop.getIntent(this))
            }
        }

    private val cropCoverLauncher =
        registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val outputUri = result.data?.let { UCrop.getOutput(it) }
                outputUri?.let { viewModel.importCoverForBook(it) }
            } else if (result.resultCode == UCrop.RESULT_ERROR) {
                val error = result.data?.let { UCrop.getError(it) }
                Toast.makeText(
                    this,
                    error?.message ?: "裁剪失败",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private val createDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            uri?.let { viewModel.backupData(it) }
        }

    private val openDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { viewModel.restoreData(it) }
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    private var openBuiltinFileManagerAction: (() -> Unit)? = null

    private val storagePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                openBuiltinFileManagerAction?.invoke()
            } else {
                Toast.makeText(this, "未授予存储读取权限", Toast.LENGTH_SHORT).show()
            }
        }

    private val allFilesAccessSettingsLauncher =
        registerForActivityResult(StartActivityForResult()) {
            if (hasStoragePermission()) {
                openBuiltinFileManagerAction?.invoke()
            } else {
                Toast.makeText(this, "请在系统设置中授予“所有文件访问权限”", Toast.LENGTH_SHORT)
                    .show()
            }
        }

    private fun getRequiredStoragePermission(): String? {
        return when {
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2 -> Manifest.permission.READ_EXTERNAL_STORAGE
            else -> null
        }
    }

    private fun hasStoragePermission(): Boolean {
        val permission = getRequiredStoragePermission()
        val hasReadPermission = permission != null &&
                ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) == PackageManager.PERMISSION_GRANTED

        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                Environment.isExternalStorageManager() || hasReadPermission
            }

            else -> hasReadPermission
        }
    }

    private fun requestAllFilesAccessPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:$packageName")
            )
            allFilesAccessSettingsLauncher.launch(intent)
        }
    }

    private fun openBuiltinFileManager(navigateToFileManager: () -> Unit) {
        navigateToFileManager()
    }

    private fun openImportPicker(navigateToFileManager: () -> Unit) {
        val useBuiltin = viewModel.useBuiltinFileManager.value
        if (useBuiltin) {
            openBuiltinFileManagerAction = { openBuiltinFileManager(navigateToFileManager) }
            if (hasStoragePermission()) {
                openBuiltinFileManagerAction?.invoke()
            } else {
                val permission = getRequiredStoragePermission()
                if (permission != null) {
                    storagePermissionLauncher.launch(permission)
                } else {
                    requestAllFilesAccessPermission()
                }
            }
        } else {
            filePickerLauncher.launch(
                arrayOf(
                    "text/plain",
                    "application/epub+zip",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/msword",
                    "application/pdf",
                    "application/x-pdf",
                    "application/x-mobipocket-ebook",
                    "application/vnd.amazon.ebook",
                    "application/octet-stream"
                )
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val conn = InterHandshake(this, lifecycleScope)
        (application as App).conn = conn
        viewModel.setConnection(conn)

        val notifManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        LiveNotificationManager.initialize(applicationContext, notifManager)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val firstLaunchTutorial = !prefs.getBoolean("tutorial_shown", false)
        val markTutorialShown = { prefs.edit().putBoolean("tutorial_shown", true).apply() }

        observeViewModelStates()

        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val themeMode by viewModel.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                MainViewModel.ThemeMode.LIGHT -> false
                MainViewModel.ThemeMode.DARK -> true
                MainViewModel.ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            EbookTheme(darkTheme = darkTheme) {
                var showAprilFools by remember { mutableStateOf(isAprilFools()) }

                val chapterToPreview by viewModel.chapterToPreview.collectAsState()
                val selectedBookForChapters by viewModel.selectedBookForChapters.collectAsState()
                val chaptersForSelectedBook by viewModel.chaptersForSelectedBook.collectAsState()
                val updateCheckState by viewModel.updateCheckState.collectAsState()
                val globalLoadingState by viewModel.globalLoadingState.collectAsState()
                val syncOptionsState by viewModel.syncOptionsState.collectAsState()
                val pushState by viewModel.pushState.collectAsState()
                val firstSyncConfirmState by viewModel.firstSyncConfirmState.collectAsState()
                val useFloatingNavigationBar by viewModel.useFloatingNavigationBar.collectAsState()

                val backStack = remember { mutableStateListOf<NavKey>(Screen.HomePager) }
                val currentScreen = backStack.lastOrNull() ?: Screen.HomePager
                val statisticsScrollState = rememberScrollState()

                val pagerState = rememberPagerState(pageCount = { 3 })

                val navigateTo = { screen: Screen ->
                    if (backStack.lastOrNull() != screen) {
                        backStack.add(screen)
                    }
                }

                val navigateBack = {
                    if (backStack.size > 1) {
                        backStack.removeAt(backStack.size - 1)
                    }
                }

                val navigateToHome = {
                    backStack.clear()
                    backStack.add(Screen.HomePager)
                }

                LaunchedEffect(syncOptionsState) {
                    if (syncOptionsState != null) navigateTo(Screen.SyncOptions)
                }

                LaunchedEffect(selectedBookForChapters) {
                    if (selectedBookForChapters != null) navigateTo(Screen.ChapterList)
                }

                LaunchedEffect(pushState.book) {
                    if (pushState.book != null) navigateTo(Screen.Push)
                }

                LaunchedEffect(chapterToPreview) {
                    if (chapterToPreview != null) navigateTo(Screen.Reader)
                }

                val showFirstSyncConfirmDialog = remember { mutableStateOf(false) }
                LaunchedEffect(firstSyncConfirmState) {
                    showFirstSyncConfirmDialog.value = firstSyncConfirmState != null
                }

                val showBottomBar = currentScreen is Screen.HomePager

                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        bottomBar = {
                            if (showBottomBar) {
                                if (useFloatingNavigationBar) {
                                    FloatingNavigationBar {
                                        FloatingNavigationBarItem(
                                            selected = pagerState.currentPage == 0,
                                            onClick = {
                                                scope.launch {
                                                    pagerState.animateScrollToPage(
                                                        0
                                                    )
                                                }
                                            },
                                            icon = MiuixIcons.VerticalSplit,
                                            label = "主页"
                                        )
                                        FloatingNavigationBarItem(
                                            selected = pagerState.currentPage == 1,
                                            onClick = {
                                                scope.launch {
                                                    pagerState.animateScrollToPage(
                                                        1
                                                    )
                                                }
                                            },
                                            icon = MiuixIcons.Sort,
                                            label = "统计"
                                        )
                                        FloatingNavigationBarItem(
                                            selected = pagerState.currentPage == 2,
                                            onClick = {
                                                scope.launch {
                                                    pagerState.animateScrollToPage(
                                                        2
                                                    )
                                                }
                                            },
                                            icon = MiuixIcons.Settings,
                                            label = "设置"
                                        )
                                    }
                                } else {
                                    NavigationBar {
                                        NavigationBarItem(
                                            selected = pagerState.currentPage == 0,
                                            onClick = {
                                                scope.launch {
                                                    pagerState.animateScrollToPage(
                                                        0
                                                    )
                                                }
                                            },
                                            icon = MiuixIcons.VerticalSplit,
                                            label = "主页"
                                        )
                                        NavigationBarItem(
                                            selected = pagerState.currentPage == 1,
                                            onClick = {
                                                scope.launch {
                                                    pagerState.animateScrollToPage(
                                                        1
                                                    )
                                                }
                                            },
                                            icon = MiuixIcons.Sort,
                                            label = "统计"
                                        )
                                        NavigationBarItem(
                                            selected = pagerState.currentPage == 2,
                                            onClick = {
                                                scope.launch {
                                                    pagerState.animateScrollToPage(
                                                        2
                                                    )
                                                }
                                            },
                                            icon = MiuixIcons.Settings,
                                            label = "设置"
                                        )
                                    }
                                }
                            }
                        },
                        floatingActionButton = {
                            if (currentScreen is Screen.HomePager && pagerState.currentPage == 0) {
                                FloatingActionButton(
                                    modifier = Modifier
                                        .pressable(
                                            interactionSource = null,
                                            indication = SinkFeedback()
                                        ),
                                    onClick = {
                                        openImportPicker { navigateTo(Screen.BuiltinFileManager) }
                                    }
                                ) {
                                    Icon(
                                        imageVector = MiuixIcons.Add,
                                        tint = MiuixTheme.colorScheme.onPrimary,
                                        contentDescription = "导入书籍"
                                    )
                                }
                            }
                        },
                        floatingActionButtonPosition = FabPosition.End
                    ) { paddingValues ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    bottom = if (showBottomBar && !useFloatingNavigationBar) {
                                        paddingValues.calculateBottomPadding()
                                    } else {
                                        0.dp
                                    }
                                )
                        ) {
                            val entryProvider = remember(backStack) {
                                entryProvider<NavKey> {
                                    entry<Screen.HomePager> {
                                        HorizontalPager(
                                            state = pagerState,
                                            modifier = Modifier.fillMaxSize()
                                        ) { page ->
                                            when (page) {
                                                0 -> MainScreen(
                                                    viewModel = viewModel,
                                                    onImportCoverClick = {
                                                        coverPickerLauncher.launch(
                                                            arrayOf("image/*")
                                                        )
                                                    },
                                                    onNavigateToSyncOptions = { navigateTo(Screen.SyncOptions) }
                                                )

                                                1 -> StatisticsScreen(
                                                    onBackClick = {
                                                        scope.launch {
                                                            pagerState.animateScrollToPage(
                                                                0
                                                            )
                                                        }
                                                    },
                                                    onBookStatClick = { bookName ->
                                                        BookStatisticsActivity.start(
                                                            this@MainActivity,
                                                            bookName
                                                        )
                                                    },
                                                    scrollState = statisticsScrollState
                                                )

                                                2 -> SettingsScreen(
                                                    viewModel = viewModel,
                                                    onBackClick = {
                                                        scope.launch {
                                                            pagerState.animateScrollToPage(
                                                                0
                                                            )
                                                        }
                                                    },
                                                    onBackupClick = {
                                                        val dateStr = SimpleDateFormat(
                                                            "yyyyMMdd_HHmmss",
                                                            Locale.getDefault()
                                                        ).format(Date())
                                                        createDocumentLauncher.launch("SineEbook_Backup_$dateStr.json")
                                                    },
                                                    onRestoreClick = {
                                                        openDocumentLauncher.launch(
                                                            arrayOf("application/json")
                                                        )
                                                    },
                                                    onBandSettingsClick = {
                                                        if (viewModel.connectionState.value.isConnected) {
                                                            viewModel.loadBandSettings()
                                                            navigateTo(Screen.BandSettings)
                                                        } else {
                                                            Toast.makeText(
                                                                context,
                                                                "请先连接手环",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    entry<Screen.BandSettings> {
                                        BandSettingsScreen(
                                            viewModel = viewModel,
                                            onBackClick = navigateBack
                                        )
                                    }

                                    entry<Screen.SyncOptions> {
                                        syncOptionsState?.let { state ->
                                            SyncOptionsScreen(
                                                state = state,
                                                onBackClick = {
                                                    viewModel.cancelPush()
                                                    navigateBack()
                                                },
                                                onConfirm = { selectedChapters, syncCover ->
                                                    viewModel.confirmPush(
                                                        state.book,
                                                        selectedChapters,
                                                        syncCover
                                                    )
                                                },
                                                onResyncCoverOnly = {
                                                    viewModel.cancelPush()
                                                    viewModel.syncCoverOnly(state.book)
                                                },
                                                onDeleteChapters = { chapterIndices ->
                                                    viewModel.deleteBandChapters(
                                                        state.book,
                                                        chapterIndices
                                                    )
                                                }
                                            )
                                        }
                                    }

                                    entry<Screen.Push> {
                                        PushScreen(
                                            pushState = pushState,
                                            onBackClick = {
                                                viewModel.cancelPush()
                                                navigateToHome()
                                            },
                                            onCancelOrDone = {
                                                viewModel.cancelPush()
                                                navigateToHome()
                                            }
                                        )
                                    }

                                    entry<Screen.ChapterList> {
                                        selectedBookForChapters?.let { book ->
                                            ChapterListScreen(
                                                book = book,
                                                chapters = chaptersForSelectedBook,
                                                readOnly = chapterToPreview != null,
                                                onBackClick = {
                                                    viewModel.closeChapterList()
                                                    navigateBack()
                                                },
                                                onPreviewChapter = { chapterId ->
                                                    scope.launch {
                                                        viewModel.showChapterPreview(chapterId)
                                                    }
                                                },
                                                onEditContent = { chapterId ->
                                                    viewModel.openChapterEditor(
                                                        chapterId
                                                    )
                                                },
                                                onSaveChapterContent = { chapterId, title, content ->
                                                    viewModel.saveChapterContent(
                                                        chapterId,
                                                        title,
                                                        content
                                                    )
                                                },
                                                onRenameChapter = { chapterId, title ->
                                                    viewModel.renameChapter(
                                                        chapterId,
                                                        title
                                                    )
                                                },
                                                onAddChapter = { index, title, content ->
                                                    viewModel.addChapter(
                                                        index,
                                                        title,
                                                        content
                                                    )
                                                },
                                                onMergeChapters = { ids, title, insertBlank ->
                                                    viewModel.mergeChapters(
                                                        ids,
                                                        title,
                                                        insertBlank
                                                    )
                                                },
                                                onBatchRename = { ids, prefix, suffix, startNumber, padding ->
                                                    viewModel.batchRenameChapters(
                                                        ids,
                                                        prefix,
                                                        suffix,
                                                        startNumber,
                                                        padding
                                                    )
                                                },
                                                loadChapterContent = { chapterId ->
                                                    viewModel.loadChapterContent(
                                                        chapterId
                                                    )
                                                }
                                            )
                                        }
                                    }

                                    entry<Screen.Reader> {
                                        ReaderScreen(
                                            viewModel = viewModel,
                                            onClose = {
                                                viewModel.closeChapterPreview()
                                                navigateBack()
                                            },
                                            onChapterChange = { chapterId ->
                                                viewModel.showChapterPreview(
                                                    chapterId
                                                )
                                            },
                                            onTableOfContents = {
                                                viewModel.chapterToPreview.value?.let {
                                                    val bookId =
                                                        viewModel.chaptersForPreview.value.firstOrNull()?.bookId
                                                    val currentBook =
                                                        viewModel.books.value.find { it.id == bookId }
                                                    currentBook?.let { viewModel.showChapterList(it) }
                                                }
                                            },
                                            loadChapterContent = viewModel::loadChapterContent
                                        )
                                    }

                                    entry<Screen.BuiltinFileManager> {
                                        BuiltinFileManagerScreen(
                                            onBackClick = navigateBack,
                                            onSelectFiles = { files ->
                                                if (files.size == 1) {
                                                    viewModel.startImport(Uri.fromFile(files.first()))
                                                } else {
                                                    viewModel.startImportBatch(files.map {
                                                        Uri.fromFile(
                                                            it
                                                        )
                                                    })
                                                }
                                                navigateBack()
                                            }
                                        )
                                    }
                                }
                            }

                            NavDisplay(
                                backStack = backStack,
                                entryProvider = entryProvider,
                                onBack = {
                                    if (backStack.size > 1) {
                                        navigateBack()
                                    } else {
                                        finish()
                                    }
                                }
                            )

                            val showTutorialState = remember { mutableStateOf(firstLaunchTutorial) }
                            if (showTutorialState.value) {
                                SuperDialog(
                                    title = "首次使用提示",
                                    summary = "请将手机端同步器的电源选项设置为无限制，以保证传输不中断。\n\nColorOS16及以上用户：前往应用的通知管理，开启“流体云显示实时通知”以启用流体云显示。\n澎湃OS3.0.300及以上用户请保证此应用的通知设置中已开启“实时通知”。",
                                    show = showTutorialState,
                                    onDismissRequest = {
                                        markTutorialShown()
                                        showTutorialState.value = false
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        TextButton(
                                            text = "关闭",
                                            onClick = {
                                                markTutorialShown()
                                                showTutorialState.value = false
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(20.dp))
                                        TextButton(
                                            text = "我知道了",
                                            onClick = {
                                                markTutorialShown()
                                                showTutorialState.value = false
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.textButtonColorsPrimary()
                                        )
                                    }
                                }
                            }

                            val showUpdateDialogState = remember { mutableStateOf(false) }
                            LaunchedEffect(updateCheckState.showSheet) {
                                showUpdateDialogState.value = updateCheckState.showSheet
                            }
                            UpdateCheckDialog(
                                showDialog = showUpdateDialogState,
                                isChecking = updateCheckState.isChecking,
                                updateInfo = updateCheckState.updateInfo,
                                updateInfoList = updateCheckState.updateInfoList,
                                errorMessage = updateCheckState.errorMessage,
                                deviceName = updateCheckState.deviceName,
                                onDismiss = {
                                    viewModel.dismissUpdateCheck()
                                },
                                onOpenWebsite = {
                                    val url =
                                        updateCheckState.updateInfoList.firstOrNull()?.downloadUrl?.takeIf { it.isNotEmpty() }
                                            ?: "https://bandbbs.cn"
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(url)
                                    )
                                    context.startActivity(intent)
                                }
                            )

                            val showLoadingState = remember { mutableStateOf(false) }
                            LaunchedEffect(globalLoadingState.isLoading) {
                                showLoadingState.value = globalLoadingState.isLoading
                            }

                            if (showLoadingState.value) {
                                SuperDialog(
                                    title = "处理中",
                                    summary = globalLoadingState.message,
                                    show = showLoadingState,
                                    onDismissRequest = {}
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }

                            FirstSyncConfirmDialog(
                                show = showFirstSyncConfirmDialog,
                                onConfirm = { viewModel.confirmFirstSync() },
                                onCancel = { viewModel.cancelFirstSyncConfirm() }
                            )
                        }
                    }

                    if (showAprilFools) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MiuixTheme.colorScheme.background)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {},
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "欢迎打开原神启动器",
                                    style = MiuixTheme.textStyles.title1.copy(fontSize = 36.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = MiuixTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(48.dp))
                                Button(
                                    onClick = {
                                        val packageNames = listOf(
                                            "com.miHoYo.GenshinImpact",
                                            "com.miHoYo.Yuanshen",
                                            "com.miHoYo.Ys.bilibili"
                                        )
                                        var launched = false
                                        for (pkg in packageNames) {
                                            val launchIntent =
                                                context.packageManager.getLaunchIntentForPackage(pkg)
                                            if (launchIntent != null) {
                                                context.startActivity(launchIntent)
                                                launched = true
                                                break
                                            }
                                        }
                                        if (!launched) {
                                            val webIntent = Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse("https://ys.mihoyo.com/")
                                            )
                                            context.startActivity(webIntent)
                                        }
                                        showAprilFools = false
                                    },
                                    colors = ButtonDefaults.buttonColorsPrimary(),
                                    modifier = Modifier
                                        .fillMaxWidth(0.6f)
                                        .pressable(
                                            interactionSource = null,
                                            indication = SinkFeedback()
                                        )
                                ) {
                                    Text("原神，启动！", color = MiuixTheme.colorScheme.onPrimary)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                TextButton(
                                    text = "不玩原神，我是来用电子书的",
                                    onClick = { showAprilFools = false },
                                    modifier = Modifier
                                        .fillMaxWidth(0.6f)
                                        .pressable(
                                            interactionSource = null,
                                            indication = SinkFeedback()
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun observeViewModelStates() {
        var wasTransferring = false
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.pushState.collect { pushState ->
                    if (pushState.isTransferring && !pushState.isFinished) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }

                    if (pushState.isTransferring) {
                        val progressPercent =
                            if (pushState.progress > 0.0) (pushState.progress * 100).toInt() else null
                        val title = if (progressPercent != null) "$progressPercent%" else "传输中"
                        ForegroundTransferService.startService(
                            applicationContext,
                            title,
                            pushState.preview,
                            progressPercent
                        )
                        if (!wasTransferring) {
                            if (viewModel.autoMinimizeOnTransfer.value && !pushState.isFinished) {
                                moveTaskToBack(true)
                            }
                            wasTransferring = true
                        }
                    } else {
                        wasTransferring = false
                        ForegroundTransferService.stopService(applicationContext)
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.syncReadingDataState.collect { syncState ->
                    val pushActive = viewModel.pushState.value.isTransferring
                    if (syncState.isSyncing && !pushActive) {
                        val progressPercent = (syncState.progress * 100).toInt()
                        ForegroundTransferService.startService(
                            applicationContext,
                            "$progressPercent%",
                            "数据同步中",
                            progressPercent
                        )
                    } else if (!pushActive) {
                        ForegroundTransferService.stopService(applicationContext)
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.backupRestoreState.collect { state ->
                    state?.let {
                        if (it.message.isNotEmpty()) {
                            Toast.makeText(this@MainActivity, it.message, Toast.LENGTH_SHORT).show()
                            viewModel.clearBackupRestoreState()
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let { viewModel.startImport(it) }
    }

    override fun onDestroy() {
        lifecycleScope.launch {
            (application as App).conn.destroy().await()
        }
        super.onDestroy()
    }
}
