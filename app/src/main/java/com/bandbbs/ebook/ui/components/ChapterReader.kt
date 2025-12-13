package com.bandbbs.ebook.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bandbbs.ebook.database.ChapterInfo
import com.bandbbs.ebook.ui.model.ChapterWithContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PREFS_NAME = "chapter_reader_prefs"
private const val KEY_READING_POSITION = "reading_position_"
private const val CHARS_PER_PAGE = 1000

data class ChapterPage(
    val content: String,
    val pageIndex: Int,
    val totalPages: Int
)

fun splitContentIntoPages(content: String, charsPerPage: Int = CHARS_PER_PAGE): List<ChapterPage> {
    if (content.isEmpty()) return listOf(ChapterPage("", 0, 1))

    val pages = mutableListOf<ChapterPage>()
    val totalLength = content.length
    var currentIndex = 0
    var pageIndex = 0

    while (currentIndex < totalLength) {
        val endIndex = (currentIndex + charsPerPage).coerceAtMost(totalLength)
        val pageContent = content.substring(currentIndex, endIndex)
        pages.add(ChapterPage(pageContent, pageIndex, 0))
        currentIndex = endIndex
        pageIndex++
    }


    return pages.mapIndexed { index, page ->
        page.copy(totalPages = pages.size)
    }
}

fun saveReadingProgress(context: Context, chapterId: Int, pageIndex: Int) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putInt("$KEY_READING_POSITION$chapterId", pageIndex).apply()
}

fun loadReadingProgress(context: Context, chapterId: Int): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt("$KEY_READING_POSITION$chapterId", 0)
}

@Composable
fun ChapterReader(
    chapter: ChapterWithContent,
    allChapters: List<ChapterInfo>,
    onClose: () -> Unit,
    onChapterChange: (Int) -> Unit,
    loadChapterContent: suspend (Int) -> String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pages = remember(chapter.content) {
        splitContentIntoPages(chapter.content)
    }

    val savedPageIndex = remember(chapter.id) {
        loadReadingProgress(context, chapter.id)
    }.coerceIn(0, (pages.size - 1).coerceAtLeast(0))

    val pagerState = rememberPagerState(
        initialPage = savedPageIndex,
        pageCount = { pages.size }
    )

    var showControls by remember { mutableStateOf(false) }
    var currentChapterIndex by remember { mutableIntStateOf(0) }


    LaunchedEffect(chapter.id, allChapters) {
        currentChapterIndex = allChapters.indexOfFirst { it.id == chapter.id }
            .coerceAtLeast(0)
    }


    LaunchedEffect(pagerState.currentPage) {
        withContext(Dispatchers.IO) {
            saveReadingProgress(context, chapter.id, pagerState.currentPage)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        val screenWidth = size.width
                        val tapX = offset.x
                        when {
                            tapX < screenWidth / 3 -> {

                                if (pagerState.currentPage > 0) {
                                    scope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                } else if (currentChapterIndex > 0) {

                                    val prevChapter = allChapters[currentChapterIndex - 1]
                                    onChapterChange(prevChapter.id)
                                }
                            }

                            tapX > screenWidth * 2 / 3 -> {

                                if (pagerState.currentPage < pages.size - 1) {
                                    scope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                } else if (currentChapterIndex < allChapters.size - 1) {

                                    val nextChapter = allChapters[currentChapterIndex + 1]
                                    onChapterChange(nextChapter.id)
                                }
                            }

                            else -> {

                                showControls = !showControls
                            }
                        }
                    }
                )
            }
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {

                Column {
                    Text(
                        text = chapter.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "${page + 1} / ${pages.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))


                Text(
                    text = pages[page].content,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 18.sp,
                        lineHeight = 32.sp
                    ),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Justify
                )


                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${((page + 1) * 100 / pages.size)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }


        if (showControls) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                    .padding(16.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "${currentChapterIndex + 1} / ${allChapters.size}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.size(48.dp))
                }

                Spacer(modifier = Modifier.weight(1f))


                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            if (currentChapterIndex > 0) {
                                val prevChapter = allChapters[currentChapterIndex - 1]
                                onChapterChange(prevChapter.id)
                            }
                        },
                        enabled = currentChapterIndex > 0
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "上一章",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("上一章")
                    }

                    TextButton(
                        onClick = {
                            if (currentChapterIndex < allChapters.size - 1) {
                                val nextChapter = allChapters[currentChapterIndex + 1]
                                onChapterChange(nextChapter.id)
                            }
                        },
                        enabled = currentChapterIndex < allChapters.size - 1
                    ) {
                        Text("下一章")
                        Spacer(modifier = Modifier.size(8.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "下一章",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

