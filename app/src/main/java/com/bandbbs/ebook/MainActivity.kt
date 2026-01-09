package com.bandbbs.ebook

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import android.app.NotificationManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bandbbs.ebook.logic.InterHandshake
import com.bandbbs.ebook.ui.components.ChapterContentEditorPanel
import com.bandbbs.ebook.ui.components.ChapterListBottomSheet
import com.bandbbs.ebook.ui.components.IpCollectionPermissionBottomSheet
import com.bandbbs.ebook.ui.components.LoadingDialog
import com.bandbbs.ebook.ui.components.UpdateCheckBottomSheet
import com.bandbbs.ebook.ui.screens.BandSettingsScreen
import com.bandbbs.ebook.ui.screens.MainScreen
import com.bandbbs.ebook.ui.screens.ReaderScreen
import com.bandbbs.ebook.ui.screens.SettingsScreen
import com.bandbbs.ebook.ui.screens.StatisticsScreen
import com.bandbbs.ebook.ui.theme.EbookTheme
import com.bandbbs.ebook.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            if (uris.size == 1) {
                viewModel.startImport(uris[0])
            } else {
                viewModel.startImportBatch(uris)
            }
        }
    }

    private val coverPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importCoverForBook(it)
        }
    }

    private val createDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            viewModel.backupData(it)
        }
    }

    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.restoreData(it)
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
    }

    @OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val conn = InterHandshake(this, lifecycleScope)
        (application as App).conn = conn
        viewModel.setConnection(conn)
        val notifManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        com.bandbbs.ebook.notifications.LiveNotificationManager.initialize(
            applicationContext,
            notifManager
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val firstLaunchTutorial = !prefs.getBoolean("tutorial_shown", false)
        fun markTutorialShown() {
            prefs.edit().putBoolean("tutorial_shown", true).apply()
        }

        fun openAppNotificationSettings() {
            try {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                }
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
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
                        val preview = pushState.preview
                        val title = if (progressPercent != null) "$progressPercent%" else "传输中"
                        val content = preview
                        com.bandbbs.ebook.notifications.ForegroundTransferService.startService(
                            applicationContext,
                            title,
                            content,
                            progressPercent
                        )
                        if (!wasTransferring) {
                            val autoMinimize = viewModel.autoMinimizeOnTransfer.value
                            if (autoMinimize && !pushState.isFinished) {
                                moveTaskToBack(true)
                            }
                            wasTransferring = true
                        }
                    } else {
                        wasTransferring = false
                        com.bandbbs.ebook.notifications.ForegroundTransferService.stopService(
                            applicationContext
                        )
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
                        val title = "$progressPercent%"
                        com.bandbbs.ebook.notifications.ForegroundTransferService.startService(
                            applicationContext,
                            title,
                            "数据同步中",
                            progressPercent
                        )
                    } else {
                        if (!pushActive) {
                            com.bandbbs.ebook.notifications.ForegroundTransferService.stopService(
                                applicationContext
                            )
                        }
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

        setContent {
            val context = LocalContext.current
            var showTutorial by remember { mutableStateOf(firstLaunchTutorial) }
            val themeMode by viewModel.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                MainViewModel.ThemeMode.LIGHT -> false
                MainViewModel.ThemeMode.DARK -> true
                MainViewModel.ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            EbookTheme(darkTheme = darkTheme) {
                val chapterToPreview by viewModel.chapterToPreview.collectAsState()

                val selectedBookForChapters by viewModel.selectedBookForChapters.collectAsState()
                val chaptersForSelectedBook by viewModel.chaptersForSelectedBook.collectAsState()
                val chapterEditorContent by viewModel.chapterEditorContent.collectAsState()

                val scope = rememberCoroutineScope()
                val chapterListSheetState = rememberModalBottomSheetState()
                val ipCollectionPermissionSheetState =
                    rememberModalBottomSheetState(skipPartiallyExpanded = true)
                val updateCheckSheetState =
                    rememberModalBottomSheetState(skipPartiallyExpanded = true)

                val ipCollectionPermissionState by viewModel.ipCollectionPermissionState.collectAsState()
                val updateCheckState by viewModel.updateCheckState.collectAsState()
                val globalLoadingState by viewModel.globalLoadingState.collectAsState()

                var currentScreen by remember { mutableStateOf("home") }
                val isReaderOpen = chapterToPreview != null
                val statisticsScrollState = rememberScrollState()

                Scaffold(
                    bottomBar = {
                        if (!isReaderOpen && currentScreen != "band_settings") {
                            NavigationBar {
                                NavigationBarItem(
                                    icon = {
                                        Icon(
                                            Icons.Filled.Home,
                                            contentDescription = "主页"
                                        )
                                    },
                                    label = { Text("主页") },
                                    selected = currentScreen == "home",
                                    onClick = { currentScreen = "home" }
                                )
                                NavigationBarItem(
                                    icon = {
                                        Icon(
                                            Icons.Filled.BarChart,
                                            contentDescription = "统计"
                                        )
                                    },
                                    label = { Text("统计") },
                                    selected = currentScreen == "statistics",
                                    onClick = { currentScreen = "statistics" }
                                )
                                NavigationBarItem(
                                    icon = {
                                        Icon(
                                            Icons.Filled.Settings,
                                            contentDescription = "设置"
                                        )
                                    },
                                    label = { Text("设置") },
                                    selected = currentScreen == "settings",
                                    onClick = { currentScreen = "settings" }
                                )
                            }
                        }
                    },
                    floatingActionButton = {
                        if (!isReaderOpen && currentScreen == "home") {
                            FloatingActionButton(
                                onClick = {
                                    filePickerLauncher.launch(
                                        arrayOf(
                                            "text/plain",
                                            "application/epub+zip",
                                            "application/octet-stream"
                                        )
                                    )
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "导入书籍")
                            }
                        }
                    }
                ) { paddingValues ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (showTutorial) {
                            AlertDialog(
                                onDismissRequest = {
                                    markTutorialShown()
                                    showTutorial = false
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        markTutorialShown()
                                        showTutorial = false
                                    }) {
                                        Text("我知道了")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = {
                                        markTutorialShown()
                                        showTutorial = false
                                    }) {
                                        Text("关闭")
                                    }
                                },
                                title = {
                                    Text("首次使用提示")
                                },
                                text = {
                                    Text(
                                        "请将手机端同步器的电源选项设置为无限制，以保证传输不中断。\n\n" +
                                                "ColorOS14及以上用户：前往应用的通知管理，开启“流体云显示实时通知”以启用流体云显示。"
                                    )
                                }
                            )
                        }

                        val targetScreenState = if (isReaderOpen) "reader" else currentScreen

                        AnimatedContent(
                            targetState = targetScreenState,
                            transitionSpec = {
                                val isEnteringReader = targetState == "reader"
                                val isExitingReader = initialState == "reader"
                                val isEnteringBandSettings = targetState == "band_settings"
                                val isExitingBandSettings = initialState == "band_settings"

                                if (isEnteringReader || isEnteringBandSettings) {
                                    slideInHorizontally(
                                        initialOffsetX = { it },
                                        animationSpec = tween(300)
                                    ) with
                                            slideOutHorizontally(
                                                targetOffsetX = { -it },
                                                animationSpec = tween(300)
                                            )
                                } else if (isExitingReader || isExitingBandSettings) {
                                    slideInHorizontally(
                                        initialOffsetX = { -it },
                                        animationSpec = tween(300)
                                    ) with
                                            slideOutHorizontally(
                                                targetOffsetX = { it },
                                                animationSpec = tween(300)
                                            )
                                } else {
                                    val screenOrder = listOf("home", "statistics", "settings")
                                    val currentIndex = screenOrder.indexOf(initialState)
                                    val targetIndex = screenOrder.indexOf(targetState)

                                    if (targetIndex > currentIndex) {
                                        slideInHorizontally(
                                            initialOffsetX = { it },
                                            animationSpec = tween(300)
                                        ) with
                                                slideOutHorizontally(
                                                    targetOffsetX = { -it },
                                                    animationSpec = tween(300)
                                                )
                                    } else {
                                        slideInHorizontally(
                                            initialOffsetX = { -it },
                                            animationSpec = tween(300)
                                        ) with
                                                slideOutHorizontally(
                                                    targetOffsetX = { it },
                                                    animationSpec = tween(300)
                                                )
                                    }
                                }
                            },
                            label = "ScreenTransition",
                            modifier = if (targetScreenState != "reader" && targetScreenState != "band_settings")
                                Modifier.padding(bottom = paddingValues.calculateBottomPadding())
                            else Modifier
                        ) { screen ->
                            when (screen) {
                                "reader" -> {
                                    ReaderScreen(
                                        viewModel = viewModel,
                                        onClose = {
                                            viewModel.closeChapterPreview()
                                        },
                                        onChapterChange = { chapterId ->
                                            viewModel.showChapterPreview(chapterId)
                                        },
                                        onTableOfContents = {

                                            val currentChapter = viewModel.chapterToPreview.value
                                            if (currentChapter != null) {


                                                val bookId =
                                                    viewModel.chaptersForPreview.value.firstOrNull()?.bookId
                                                if (bookId != null) {
                                                    val currentBook =
                                                        viewModel.books.value.find { it.id == bookId }
                                                    if (currentBook != null) {
                                                        viewModel.showChapterList(currentBook)
                                                    }
                                                }
                                            }
                                        },
                                        loadChapterContent = viewModel::loadChapterContent
                                    )
                                }

                                "home" -> {
                                    MainScreen(
                                        viewModel = viewModel,
                                        onImportCoverClick = {
                                            coverPickerLauncher.launch(arrayOf("image/*"))
                                        }
                                    )
                                }

                                "statistics" -> {
                                    StatisticsScreen(
                                        onBackClick = { currentScreen = "home" },
                                        onBookStatClick = { bookName ->
                                            BookStatisticsActivity.start(
                                                this@MainActivity,
                                                bookName
                                            )
                                        },
                                        scrollState = statisticsScrollState
                                    )
                                }

                                "settings" -> {
                                    SettingsScreen(
                                        viewModel = viewModel,
                                        onBackClick = { currentScreen = "home" },
                                        onBackupClick = {
                                            val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                                            val dateStr = formatter.format(Date())
                                            createDocumentLauncher.launch("SineEbook_Backup_$dateStr.json")
                                        },
                                        onRestoreClick = {
                                            openDocumentLauncher.launch(arrayOf("application/json"))
                                        },
                                        onBandSettingsClick = {
                                            if (viewModel.connectionState.value.isConnected) {
                                                viewModel.loadBandSettings()
                                                currentScreen = "band_settings"
                                            } else {
                                                Toast.makeText(context, "请先连接手环", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }

                                "band_settings" -> {
                                    BandSettingsScreen(
                                        viewModel = viewModel,
                                        onBackClick = { currentScreen = "settings" }
                                    )
                                }
                            }
                        }

                        selectedBookForChapters?.let { book ->
                            ModalBottomSheet(
                                onDismissRequest = { viewModel.closeChapterList() },
                                sheetState = chapterListSheetState
                            ) {
                                ChapterListBottomSheet(
                                    book = book,
                                    chapters = chaptersForSelectedBook,
                                    readOnly = isReaderOpen,
                                    onPreviewChapter = { chapterId ->

                                        scope.launch {
                                            chapterListSheetState.hide()
                                            viewModel.closeChapterList()
                                            viewModel.showChapterPreview(chapterId)
                                        }
                                    },
                                    onEditContent = { chapterId ->
                                        viewModel.openChapterEditor(chapterId)
                                    },
                                    onSaveChapterContent = { chapterId, title, content ->
                                        viewModel.saveChapterContent(chapterId, title, content)
                                    },
                                    onRenameChapter = { chapterId, title ->
                                        viewModel.renameChapter(chapterId, title)
                                    },
                                    onAddChapter = { index, title, content ->
                                        viewModel.addChapter(index, title, content)
                                    },
                                    onMergeChapters = { ids, title, insertBlank ->
                                        viewModel.mergeChapters(ids, title, insertBlank)
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
                                        viewModel.loadChapterContent(chapterId)
                                    }
                                )
                            }
                        }

                        chapterEditorContent?.let { editorState ->
                            ChapterContentEditorPanel(
                                state = editorState,
                                onDismiss = { viewModel.closeChapterEditor() },
                                onSave = { title, content ->
                                    viewModel.saveChapterContent(editorState.id, title, content)
                                },
                                onSplit = { segments ->
                                    viewModel.splitChapter(editorState.id, segments)
                                }
                            )
                        }

                        if (ipCollectionPermissionState.showSheet) {
                            LaunchedEffect(ipCollectionPermissionState.showSheet) {
                                ipCollectionPermissionSheetState.show()
                            }
                            ModalBottomSheet(
                                onDismissRequest = {
                                    scope.launch {
                                        ipCollectionPermissionSheetState.hide()
                                        viewModel.dismissIpCollectionPermissionSheet()
                                    }
                                },
                                sheetState = ipCollectionPermissionSheetState
                            ) {
                                IpCollectionPermissionBottomSheet(
                                    isFirstTime = ipCollectionPermissionState.isFirstTime,
                                    onAllow = {
                                        scope.launch {
                                            ipCollectionPermissionSheetState.hide()
                                            viewModel.onIpCollectionPermissionResult(true)
                                        }
                                    },
                                    onDeny = {
                                        scope.launch {
                                            ipCollectionPermissionSheetState.hide()
                                            viewModel.onIpCollectionPermissionResult(false)
                                        }
                                    }
                                )
                            }
                        }

                        if (updateCheckState.showSheet) {
                            LaunchedEffect(updateCheckState.showSheet) {
                                updateCheckSheetState.show()
                            }
                            ModalBottomSheet(
                                onDismissRequest = {
                                    scope.launch {
                                        updateCheckSheetState.hide()
                                        viewModel.dismissUpdateCheck()
                                    }
                                },
                                sheetState = updateCheckSheetState
                            ) {
                                UpdateCheckBottomSheet(
                                    isChecking = updateCheckState.isChecking,
                                    updateInfo = updateCheckState.updateInfo,
                                    updateInfoList = updateCheckState.updateInfoList,
                                    errorMessage = updateCheckState.errorMessage,
                                    deviceName = updateCheckState.deviceName,
                                    onDismiss = {
                                        scope.launch {
                                            updateCheckSheetState.hide()
                                            viewModel.dismissUpdateCheck()
                                        }
                                    }
                                )
                            }
                        }

                        if (globalLoadingState.isLoading) {
                            LoadingDialog(
                                message = globalLoadingState.message
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let {
            viewModel.startImport(it)
        }
    }

    override fun onDestroy() {
        lifecycleScope.launch {
            (application as App).conn.destroy().await()
        }
        super.onDestroy()
    }
}
