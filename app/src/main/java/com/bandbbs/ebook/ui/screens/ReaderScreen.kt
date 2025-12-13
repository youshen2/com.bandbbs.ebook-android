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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bandbbs.ebook.ui.components.ReaderSettingsBottomSheet
import com.bandbbs.ebook.ui.components.loadReaderSettings
import com.bandbbs.ebook.ui.viewmodel.MainViewModel
import com.bandbbs.ebook.utils.ReadingTimeStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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


    val bookName = remember(chapter, allChapters, books) {
        if (allChapters.isNotEmpty()) {
            val bookId = allChapters[0].bookId
            books.find { it.id == bookId }?.name
        } else null
    }


    var showControls by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }
    var readerSettings by remember { mutableStateOf(loadReaderSettings(context)) }
    val settingsSheetState = rememberModalBottomSheetState()


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

    BackHandler(enabled = true) {

        chapter?.let { ch ->
            saveReadingPosition(context, ch.id, firstVisibleItemIndex, firstVisibleItemScrollOffset)
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

        listState.scrollToItem(0, 0)
        if (index < paragraphs.size + 1) {
            listState.scrollToItem(index, offset)
        }
    }


    LaunchedEffect(firstVisibleItemIndex, firstVisibleItemScrollOffset) {


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
    ) {
        if (chapter != null) {
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            if (currentChapterIndex > 0) {
                                TextButton(onClick = {

                                    saveReadingPosition(
                                        context,
                                        chapter!!.id,
                                        firstVisibleItemIndex,
                                        firstVisibleItemScrollOffset
                                    )

                                    if (bookName != null) {
                                        scope.launch(Dispatchers.IO) {
                                            ReadingTimeStorage.recordReadingEnd(context, bookName)
                                        }
                                    }
                                    val prevChapter = allChapters[currentChapterIndex - 1]
                                    onChapterChange(prevChapter.id)
                                }) {
                                    Text("上一章", color = Color(readerSettings.textColor))
                                }
                            }

                            if (currentChapterIndex < allChapters.size - 1) {
                                TextButton(onClick = {

                                    saveReadingPosition(
                                        context,
                                        chapter!!.id,
                                        firstVisibleItemIndex,
                                        firstVisibleItemScrollOffset
                                    )

                                    if (bookName != null) {
                                        scope.launch(Dispatchers.IO) {
                                            ReadingTimeStorage.recordReadingEnd(context, bookName)
                                        }
                                    }
                                    val nextChapter = allChapters[currentChapterIndex + 1]
                                    onChapterChange(nextChapter.id)
                                }) {
                                    Text("下一章", color = Color(readerSettings.textColor))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(64.dp))
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

                val progress = if (paragraphs.isNotEmpty()) {
                    ((firstVisibleItemIndex.toFloat() / paragraphs.size) * 100).toInt()
                        .coerceIn(0, 100)
                } else 0

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

                    TextButton(
                        onClick = {
                            if (currentChapterIndex > 0) {
                                chapter?.let {
                                    saveReadingPosition(
                                        context,
                                        it.id,
                                        firstVisibleItemIndex,
                                        firstVisibleItemScrollOffset
                                    )
                                }

                                if (bookName != null) {
                                    scope.launch(Dispatchers.IO) {
                                        ReadingTimeStorage.recordReadingEnd(context, bookName)
                                    }
                                }
                                val prevChapter = allChapters[currentChapterIndex - 1]
                                onChapterChange(prevChapter.id)
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


                    IconButton(onClick = onTableOfContents) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "目录",
                            tint = Color(readerSettings.textColor),
                            modifier = Modifier.size(24.dp)
                        )
                    }


                    TextButton(
                        onClick = {
                            if (currentChapterIndex < allChapters.size - 1) {
                                chapter?.let {
                                    saveReadingPosition(
                                        context,
                                        it.id,
                                        firstVisibleItemIndex,
                                        firstVisibleItemScrollOffset
                                    )
                                }

                                if (bookName != null) {
                                    scope.launch(Dispatchers.IO) {
                                        ReadingTimeStorage.recordReadingEnd(context, bookName)
                                    }
                                }
                                val nextChapter = allChapters[currentChapterIndex + 1]
                                onChapterChange(nextChapter.id)
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
}
