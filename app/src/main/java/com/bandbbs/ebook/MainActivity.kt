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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bandbbs.ebook.logic.InterHandshake
import com.bandbbs.ebook.ui.components.ChapterContentEditorPanel
import com.bandbbs.ebook.ui.components.ChapterListBottomSheet
import com.bandbbs.ebook.ui.screens.MainScreen
import com.bandbbs.ebook.ui.screens.ReaderScreen
import com.bandbbs.ebook.ui.theme.EbookTheme
import com.bandbbs.ebook.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.startImport(it)
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
            EbookTheme {
                val chapterToPreview by viewModel.chapterToPreview.collectAsState()

                val selectedBookForChapters by viewModel.selectedBookForChapters.collectAsState()
                val chaptersForSelectedBook by viewModel.chaptersForSelectedBook.collectAsState()
                val chapterEditorContent by viewModel.chapterEditorContent.collectAsState()

                val scope = rememberCoroutineScope()
                val chapterListSheetState = rememberModalBottomSheetState()


                val isReaderOpen = chapterToPreview != null

                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = isReaderOpen,
                        transitionSpec = {
                            if (targetState) {

                                slideInHorizontally(
                                    initialOffsetX = { it },
                                    animationSpec = tween(300)
                                ) + fadeIn(animationSpec = tween(300)) with
                                        slideOutHorizontally(
                                            targetOffsetX = { -it / 3 },
                                            animationSpec = tween(300)
                                        ) + fadeOut(animationSpec = tween(300))
                            } else {

                                slideInHorizontally(
                                    initialOffsetX = { -it / 3 },
                                    animationSpec = tween(300)
                                ) + fadeIn(animationSpec = tween(300)) with
                                        slideOutHorizontally(
                                            targetOffsetX = { it },
                                            animationSpec = tween(300)
                                        ) + fadeOut(animationSpec = tween(300))
                            }
                        },
                        label = "ReaderTransition"
                    ) { readerOpen ->
                        if (readerOpen) {
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
                        } else {
                            MainScreen(
                                viewModel = viewModel,
                                onImportClick = {
                                    filePickerLauncher.launch(
                                        arrayOf(
                                            "text/plain",
                                            "application/epub+zip",
                                            "application/octet-stream"
                                        )
                                    )
                                },
                                onImportCoverClick = {
                                    coverPickerLauncher.launch(arrayOf("image/*"))
                                }
                            )
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
