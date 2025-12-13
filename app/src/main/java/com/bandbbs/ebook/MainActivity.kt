package com.bandbbs.ebook

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bandbbs.ebook.logic.InterHandshake
import com.bandbbs.ebook.ui.components.ChapterContentEditorPanel
import com.bandbbs.ebook.ui.components.ChapterListBottomSheet
import com.bandbbs.ebook.ui.screens.BookStatisticsScreen
import com.bandbbs.ebook.ui.screens.MainScreen
import com.bandbbs.ebook.ui.screens.ReaderScreen
import com.bandbbs.ebook.ui.screens.SettingsScreen
import com.bandbbs.ebook.ui.screens.StatisticsScreen
import com.bandbbs.ebook.ui.theme.EbookTheme
import com.bandbbs.ebook.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

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

    @OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val conn = InterHandshake(this, lifecycleScope)
        (application as App).conn = conn
        viewModel.setConnection(conn)



        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pushState.collect { pushState ->
                    if (pushState.isTransferring && !pushState.isFinished) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }
            }
        }

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                MainViewModel.ThemeMode.LIGHT -> false
                MainViewModel.ThemeMode.DARK -> true
                MainViewModel.ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            EbookTheme(darkTheme = darkTheme) {
                val chapterToPreview by viewModel.chapterToPreview.collectAsState()

                val selectedBookForChapters by viewModel.selectedBookForChapters.collectAsState()
                val chaptersForSelectedBook by viewModel.chaptersForSelectedBook.collectAsState()
                val chapterEditorContent by viewModel.chapterEditorContent.collectAsState()

                val scope = rememberCoroutineScope()
                val chapterListSheetState = rememberModalBottomSheetState()

                var currentScreen by remember { mutableStateOf("home") }
                var selectedBookForStats by remember { mutableStateOf<String?>(null) }
                val isReaderOpen = chapterToPreview != null
                val statisticsScrollState = rememberScrollState()

                Scaffold(
                    bottomBar = {
                        if (!isReaderOpen) {
                            NavigationBar {
                                NavigationBarItem(
                                    icon = {
                                        Icon(
                                            Icons.Filled.Home,
                                            contentDescription = "主页"
                                        )
                                    },
                                    label = { androidx.compose.material3.Text("主页") },
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
                                    label = { androidx.compose.material3.Text("统计") },
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
                                    label = { androidx.compose.material3.Text("设置") },
                                    selected = currentScreen == "settings",
                                    onClick = { currentScreen = "settings" }
                                )
                            }
                        }
                    },
                    floatingActionButton = {
                        if (!isReaderOpen && currentScreen == "home" && selectedBookForStats == null) {
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
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AnimatedContent(
                            targetState = if (isReaderOpen) "reader" else if (selectedBookForStats != null) "book_statistics" else currentScreen,
                            transitionSpec = {
                                val isEnteringReader = targetState == "reader"
                                val isExitingReader = initialState == "reader"
                                val isEnteringBookStats = targetState == "book_statistics"
                                val isExitingBookStats = initialState == "book_statistics"

                                if (isEnteringReader) {

                                    slideInHorizontally(
                                        initialOffsetX = { it },
                                        animationSpec = tween(300)
                                    ) with

                                            slideOutHorizontally(
                                                targetOffsetX = { -it },
                                                animationSpec = tween(300)
                                            )
                                } else if (isExitingReader) {


                                    slideInHorizontally(
                                        initialOffsetX = { -it },
                                        animationSpec = tween(300)
                                    ) with
                                            slideOutHorizontally(
                                                targetOffsetX = { it },
                                                animationSpec = tween(300)
                                            )
                                } else if (isEnteringBookStats) {

                                    slideInHorizontally(
                                        initialOffsetX = { it },
                                        animationSpec = tween(300)
                                    ) with
                                            slideOutHorizontally(
                                                targetOffsetX = { -it },
                                                animationSpec = tween(300)
                                            )
                                } else if (isExitingBookStats) {

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
                            label = "ScreenTransition"
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
                                            selectedBookForStats = bookName
                                        },
                                        scrollState = statisticsScrollState
                                    )
                                }

                                "book_statistics" -> {
                                    selectedBookForStats?.let { bookName ->
                                        BookStatisticsScreen(
                                            bookName = bookName,
                                            onBackClick = { selectedBookForStats = null }
                                        )
                                    }
                                }

                                "settings" -> {
                                    SettingsScreen(
                                        viewModel = viewModel,
                                        onBackClick = { currentScreen = "home" }
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
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.reconnect()
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