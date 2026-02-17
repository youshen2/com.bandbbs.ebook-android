package com.bandbbs.ebook.ui.screens

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bandbbs.ebook.database.BookmarkEntity
import com.bandbbs.ebook.ui.components.BookmarkListBottomSheet
import com.bandbbs.ebook.ui.components.PageTurnMode
import com.bandbbs.ebook.ui.components.PdfPageViewer
import com.bandbbs.ebook.ui.components.ReaderSettingsBottomSheet
import com.bandbbs.ebook.ui.components.loadReaderSettings
import com.bandbbs.ebook.ui.viewmodel.MainViewModel
import com.bandbbs.ebook.utils.BookmarkManager
import com.bandbbs.ebook.utils.ReadingTimeStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private const val PREFS_NAME = "chapter_reader_prefs"
private const val KEY_READING_POSITION = "reading_position_index_"
private const val KEY_READING_OFFSET = "reading_position_offset_"
private const val KEY_LAST_READ_CHAPTER = "last_read_chapter_"

fun saveReadingPosition(context: Context, chapterId: Int, index: Int, offset: Int) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit()
        .putInt("$KEY_READING_POSITION$chapterId", index)
        .putInt("$KEY_READING_OFFSET$chapterId", offset)
        .apply()
}

fun loadReadingPosition(context: Context, chapterId: Int): Pair<Int, Int> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val index = prefs.getInt("$KEY_READING_POSITION$chapterId", 0)
    val offset = prefs.getInt("$KEY_READING_OFFSET$chapterId", 0)
    return index to offset
}

fun saveLastReadChapter(context: Context, bookId: Int, chapterId: Int) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putInt("$KEY_LAST_READ_CHAPTER$bookId", chapterId).apply()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: MainViewModel,
    onClose: () -> Unit,
    onChapterChange: (Int) -> Unit,
    onTableOfContents: () -> Unit,
    loadChapterContent: suspend (Int) -> String
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()


    val chapter by viewModel.chapterToPreview.collectAsState()
    val allChapters by viewModel.chaptersForPreview.collectAsState()
    val books by viewModel.books.collectAsState()


    val bookId = remember(chapter, allChapters, books) {
        if (allChapters.isNotEmpty()) {
            allChapters[0].bookId
        } else null
    }

    val bookName = remember(chapter, allChapters, books) {
        if (allChapters.isNotEmpty()) {
            val id = allChapters[0].bookId
            books.find { it.id == id }?.name
        } else null
    }

    val currentBook = remember(bookId, books) {
        if (bookId != null) books.find { it.id == bookId } else null
    }

    val isPdf = remember(currentBook) { currentBook?.format == "pdf" }


    var showControls by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }
    var showBookmarks by remember { mutableStateOf(false) }
    var bookmarks by remember { mutableStateOf<List<BookmarkEntity>>(emptyList()) }
    var readerSettings by remember { mutableStateOf(loadReaderSettings(context)) }
    val settingsSheetState = rememberModalBottomSheetState()
    val bookmarkSheetState = rememberModalBottomSheetState()


    val currentChapterIndex = remember(chapter, allChapters) {
        if (chapter != null) {
            allChapters.indexOfFirst { it.id == chapter!!.id }.coerceAtLeast(0)
        } else 0
    }

    val paragraphs = remember(chapter) {
        chapter?.content?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()
    }


    var previousChapterId by remember { mutableIntStateOf(-1) }

    var currentTime by remember { mutableStateOf("") }
    var batteryLevel by remember { mutableIntStateOf(0) }

    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
    val density = LocalDensity.current


    val isDragged by listState.interactionSource.collectIsDraggedAsState()
    val firstVisibleItemIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    val firstVisibleItemScrollOffset by remember { derivedStateOf { listState.firstVisibleItemScrollOffset } }

    var turnInProgress by remember { mutableStateOf(false) }

    fun changeChapterByDelta(delta: Int) {
        if (turnInProgress) return
        if (allChapters.isEmpty()) return
        val targetIndex = (currentChapterIndex + delta).coerceIn(0, allChapters.lastIndex)
        if (targetIndex == currentChapterIndex) return

        turnInProgress = true
        scope.launch {
            delay(450)
            turnInProgress = false
        }

        chapter?.let { ch ->
            if (!isPdf) {
                saveReadingPosition(
                    context,
                    ch.id,
                    firstVisibleItemIndex,
                    firstVisibleItemScrollOffset
                )
            }
        }

        if (bookName != null) {
            scope.launch(Dispatchers.IO) {
                ReadingTimeStorage.recordReadingEnd(context, bookName)
            }
        }

        onChapterChange(allChapters[targetIndex].id)
    }

    LaunchedEffect(bookId) {
        if (bookId != null) {
            bookmarks = BookmarkManager.getBookmarks(context, bookId)
        }
    }

    BackHandler(enabled = true) {

        chapter?.let { ch ->
            if (!isPdf) {
                saveReadingPosition(
                    context,
                    ch.id,
                    firstVisibleItemIndex,
                    firstVisibleItemScrollOffset
                )
            }
        }

        if (bookName != null) {
            ReadingTimeStorage.recordReadingEnd(context, bookName)
        }

        onClose()
    }


    LaunchedEffect(chapter, bookName) {
        if (chapter == null) return@LaunchedEffect


        if (allChapters.isNotEmpty()) {
            val bookId = allChapters[0].bookId
            withContext(Dispatchers.IO) {
                saveLastReadChapter(context, bookId, chapter!!.id)
            }
        }

        if (bookName != null) {
            withContext(Dispatchers.IO) {

                if (previousChapterId != -1 && previousChapterId != chapter!!.id) {
                    ReadingTimeStorage.recordReadingEnd(context, bookName)
                }

                ReadingTimeStorage.recordReadingStart(context, bookName)
                previousChapterId = chapter!!.id
            }
        }


        val (index, offset) = withContext(Dispatchers.IO) {
            loadReadingPosition(context, chapter!!.id)
        }

        if (!isPdf) {
            listState.scrollToItem(0, 0)
            if (index < paragraphs.size + 1) {
                listState.scrollToItem(index, offset)
            }
        }
    }


    LaunchedEffect(firstVisibleItemIndex, firstVisibleItemScrollOffset) {


        if (!isPdf) {
            delay(1000)
            if (chapter != null) {
                withContext(Dispatchers.IO) {
                    saveReadingPosition(
                        context,
                        chapter!!.id,
                        firstVisibleItemIndex,
                        firstVisibleItemScrollOffset
                    )
                }
            }
        }
    }


    DisposableEffect(lifecycleOwner, bookName) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    chapter?.let { ch ->
                        saveReadingPosition(
                            context,
                            ch.id,
                            firstVisibleItemIndex,
                            firstVisibleItemScrollOffset
                        )
                    }
                    if (bookName != null) {
                        ReadingTimeStorage.recordReadingEnd(context, bookName)
                    }
                }

                Lifecycle.Event.ON_RESUME -> {
                    if (bookName != null) {
                        ReadingTimeStorage.recordReadingStart(context, bookName)
                    }
                }

                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (bookName != null) {
                ReadingTimeStorage.recordReadingEnd(context, bookName)
            }
        }
    }


    DisposableEffect(readerSettings.keepScreenOn) {
        val window = (context as? Activity)?.window
        if (readerSettings.keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val window = (context as? Activity)?.window
    val originalBrightness = remember(window) { window?.attributes?.screenBrightness ?: -1f }

    DisposableEffect(window) {
        onDispose {
            if (window != null) {
                val lp = window.attributes
                lp.screenBrightness = originalBrightness
                window.attributes = lp
            }
        }
    }

    LaunchedEffect(window, readerSettings.customBrightnessEnabled, readerSettings.customBrightness) {
        if (window != null) {
            val lp = window.attributes
            lp.screenBrightness = if (readerSettings.customBrightnessEnabled) {
                readerSettings.customBrightness.coerceIn(0.01f, 1f)
            } else {
                -1f
            }
            window.attributes = lp
        }
    }

    val swipeThresholdPx = remember(readerSettings.pageTurnSensitivity, density) {
        val baseDp = 180f
        val minDp = 60f
        val t = (readerSettings.pageTurnSensitivity.coerceIn(1, 10) - 1) / 9f
        val thresholdDp = baseDp - (baseDp - minDp) * t
        with(density) { thresholdDp.dp.toPx() }
    }

    val overscrollThresholdPx = remember(readerSettings.pageTurnSensitivity, density) {
        val baseDp = 140f
        val minDp = 50f
        val t = (readerSettings.pageTurnSensitivity.coerceIn(1, 10) - 1) / 9f
        val thresholdDp = baseDp - (baseDp - minDp) * t
        with(density) { thresholdDp.dp.toPx() }
    }

    val overscrollConnection = remember(
        readerSettings.pageTurnMode,
        overscrollThresholdPx,
        currentChapterIndex,
        allChapters.size,
        isPdf,
        turnInProgress
    ) {
        object : NestedScrollConnection {
            private var acc = 0f

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (readerSettings.pageTurnMode != PageTurnMode.OVERSCROLL) return Offset.Zero
                if (isPdf) return Offset.Zero
                if (source != NestedScrollSource.Drag) return Offset.Zero
                if (turnInProgress) return Offset.Zero

                val atTop = !listState.canScrollBackward
                val atBottom = !listState.canScrollForward

                if (available.y > 0 && atTop && currentChapterIndex > 0) {
                    acc += available.y
                    if (acc >= overscrollThresholdPx) {
                        acc = 0f
                        changeChapterByDelta(-1)
                    }
                } else if (available.y < 0 && atBottom && currentChapterIndex < allChapters.size - 1) {
                    acc += -available.y
                    if (acc >= overscrollThresholdPx) {
                        acc = 0f
                        changeChapterByDelta(1)
                    }
                } else {
                    acc = 0f
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                acc = 0f
                return Velocity.Zero
            }
        }
    }


    LaunchedEffect(readerSettings.autoScrollSpeed, isDragged, showControls) {
        if (readerSettings.autoScrollSpeed > 0 && !isDragged && !showControls) {
            while (isActive) {


                val scrollAmount = (readerSettings.autoScrollSpeed / 20f) * density.density
                listState.scrollBy(scrollAmount)
                delay(16)
            }
        }
    }


    LaunchedEffect(Unit) {
        while (true) {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            currentTime = sdf.format(Date())
            delay(1000 * 60)
        }
    }


    DisposableEffect(context) {
        val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                batteryLevel = (level * 100 / scale.toFloat()).toInt()
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(batteryReceiver, filter)
        onDispose {
            context.unregisterReceiver(batteryReceiver)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(readerSettings.backgroundColor))
            .nestedScroll(overscrollConnection)
            .pointerInput(
                readerSettings.pageTurnMode,
                readerSettings.pageTurnSensitivity,
                showSettings,
                showBookmarks,
                turnInProgress,
                currentChapterIndex,
                allChapters.size
            ) {
                if (readerSettings.pageTurnMode == PageTurnMode.SWIPE && !showSettings && !showBookmarks) {
                    var dragX = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dragX = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            dragX += dragAmount
                            change.consume()
                        },
                        onDragCancel = { dragX = 0f },
                        onDragEnd = {
                            if (!turnInProgress && abs(dragX) >= swipeThresholdPx) {
                                if (dragX < 0) {
                                    if (currentChapterIndex < allChapters.size - 1) changeChapterByDelta(1)
                                } else {
                                    if (currentChapterIndex > 0) changeChapterByDelta(-1)
                                }
                            }
                            dragX = 0f
                        }
                    )
                }
            }
    ) {
        if (chapter != null) {
            if (isPdf && currentBook != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showControls = !showControls }
                ) {
                    PdfPageViewer(
                        pdfPath = currentBook.path,
                        pageIndex = currentChapterIndex,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showControls = !showControls },
                    contentPadding = PaddingValues(
                        top = systemBarsPadding.calculateTopPadding() + 16.dp,
                        bottom = systemBarsPadding.calculateBottomPadding() + 16.dp,
                        start = 24.dp,
                        end = 24.dp
                    )
                ) {
                    item {
                        Text(
                            text = chapter!!.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(readerSettings.textColor),
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                    }

                    items(paragraphs) { paragraph ->
                        Text(
                            text = "        $paragraph",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = readerSettings.fontSize.sp,
                                lineHeight = (readerSettings.fontSize * readerSettings.lineHeight).sp
                            ),
                            color = Color(readerSettings.textColor),
                            textAlign = TextAlign.Justify,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "本章完",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(readerSettings.textColor).copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            if (readerSettings.pageTurnMode == PageTurnMode.BUTTON) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    if (currentChapterIndex > 0) {
                                        TextButton(onClick = { changeChapterByDelta(-1) }) {
                                            Text("上一章", color = Color(readerSettings.textColor))
                                        }
                                    }

                                    if (currentChapterIndex < allChapters.size - 1) {
                                        TextButton(onClick = { changeChapterByDelta(1) }) {
                                            Text("下一章", color = Color(readerSettings.textColor))
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(64.dp))
                        }
                    }
                }
            }
        }

        if (!showControls && chapter != null) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(
                        bottom = systemBarsPadding.calculateBottomPadding() + 4.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = currentTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(readerSettings.textColor).copy(alpha = 0.6f)
                )

                val progress = if (isPdf) {
                    if (allChapters.isNotEmpty()) {
                        (((currentChapterIndex + 1).toFloat() / allChapters.size.toFloat()) * 100).toInt()
                            .coerceIn(0, 100)
                    } else 0
                } else {
                    if (paragraphs.isNotEmpty()) {
                        ((firstVisibleItemIndex.toFloat() / paragraphs.size) * 100).toInt()
                            .coerceIn(0, 100)
                    } else 0
                }

                Text(
                    text = "$progress%",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(readerSettings.textColor).copy(alpha = 0.6f)
                )

                Text(
                    text = "$batteryLevel%",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(readerSettings.textColor).copy(alpha = 0.6f)
                )
            }
        }

        AnimatedVisibility(
            visible = showControls,
            enter = slideInVertically(initialOffsetY = { -it }),
            exit = slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(modifier = Modifier.background(Color(readerSettings.backgroundColor))) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = chapter?.name ?: "",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                color = Color(readerSettings.textColor)
                            )
                            Text(
                                text = "${currentChapterIndex + 1} / ${allChapters.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(readerSettings.textColor).copy(alpha = 0.7f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            chapter?.let {
                                saveReadingPosition(
                                    context,
                                    it.id,
                                    firstVisibleItemIndex,
                                    firstVisibleItemScrollOffset
                                )
                            }
                            if (bookName != null) {
                                ReadingTimeStorage.recordReadingEnd(context, bookName)
                            }
                            onClose()
                        }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "返回",
                                tint = Color(readerSettings.textColor)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            if (bookId != null) {
                                scope.launch {
                                    bookmarks = BookmarkManager.getBookmarks(context, bookId)
                                    showBookmarks = true
                                }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = "书签",
                                tint = Color(readerSettings.textColor)
                            )
                        }
                        IconButton(onClick = { showSettings = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "设置",
                                tint = Color(readerSettings.textColor)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }

        AnimatedVisibility(
            visible = showControls,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(readerSettings.backgroundColor))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .padding(bottom = systemBarsPadding.calculateBottomPadding()),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    if (readerSettings.pageTurnMode == PageTurnMode.BUTTON) {
                        TextButton(
                            onClick = {
                                if (currentChapterIndex > 0) {
                                    changeChapterByDelta(-1)
                                }
                            },
                            enabled = currentChapterIndex > 0
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "上一章",
                                modifier = Modifier.size(20.dp),
                                tint = if (currentChapterIndex > 0) Color(readerSettings.textColor) else Color.Gray
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(
                                "上一章",
                                color = if (currentChapterIndex > 0) Color(readerSettings.textColor) else Color.Gray
                            )
                        }
                    }


                    IconButton(onClick = onTableOfContents) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "目录",
                            tint = Color(readerSettings.textColor),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(onClick = {
                        if (bookId != null && chapter != null) {
                            scope.launch {
                                val bookmarkName = "书签${bookmarks.size + 1}"
                                val chapterName = chapter?.name ?: ""
                                BookmarkManager.addBookmark(
                                    context = context,
                                    bookId = bookId,
                                    name = bookmarkName,
                                    chapterIndex = currentChapterIndex,
                                    chapterName = chapterName,
                                    offsetInChapter = if (isPdf) 0 else firstVisibleItemIndex,
                                    scrollOffset = if (isPdf) 0 else firstVisibleItemScrollOffset
                                )
                                bookmarks = BookmarkManager.getBookmarks(context, bookId)
                            }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.BookmarkAdd,
                            contentDescription = "添加书签",
                            tint = Color(readerSettings.textColor),
                            modifier = Modifier.size(24.dp)
                        )
                    }


                    if (readerSettings.pageTurnMode == PageTurnMode.BUTTON) {
                        TextButton(
                            onClick = {
                                if (currentChapterIndex < allChapters.size - 1) {
                                    changeChapterByDelta(1)
                                }
                            },
                            enabled = currentChapterIndex < allChapters.size - 1
                        ) {
                            Text(
                                "下一章",
                                color = if (currentChapterIndex < allChapters.size - 1) Color(
                                    readerSettings.textColor
                                ) else Color.Gray
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "下一章",
                                modifier = Modifier.size(20.dp),
                                tint = if (currentChapterIndex < allChapters.size - 1) Color(
                                    readerSettings.textColor
                                ) else Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            sheetState = settingsSheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            ReaderSettingsBottomSheet(
                currentSettings = readerSettings,
                onSettingsChanged = { newSettings ->
                    readerSettings = newSettings
                }
            )
        }
    }

    if (showBookmarks) {
        ModalBottomSheet(
            onDismissRequest = { showBookmarks = false },
            sheetState = bookmarkSheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            BookmarkListBottomSheet(
                bookmarks = bookmarks,
                onBookmarkClick = { bookmark ->
                    scope.launch {
                        val targetChapter = allChapters.find { it.index == bookmark.chapterIndex }
                        if (targetChapter != null) {
                            onChapterChange(targetChapter.id)
                            kotlinx.coroutines.delay(300)
                            listState.scrollToItem(bookmark.offsetInChapter, bookmark.scrollOffset)
                        }
                        showBookmarks = false
                    }
                },
                onDeleteBookmark = { bookmark ->
                    scope.launch {
                        BookmarkManager.deleteBookmark(context, bookmark.id)
                        bookmarks = BookmarkManager.getBookmarks(context, bookId ?: 0)
                    }
                },
                onEditBookmark = { updatedBookmark ->
                    scope.launch {
                        BookmarkManager.updateBookmark(context, updatedBookmark)
                        bookmarks = BookmarkManager.getBookmarks(context, bookId ?: 0)
                    }
                }
            )
        }
    }
}
