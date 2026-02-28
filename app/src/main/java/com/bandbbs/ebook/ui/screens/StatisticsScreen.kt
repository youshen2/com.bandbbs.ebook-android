package com.bandbbs.ebook.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bandbbs.ebook.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Months
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Recent
import top.yukonga.miuix.kmp.icon.extended.Report
import top.yukonga.miuix.kmp.icon.extended.Timer
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class ReadingTimeStats(
    val totalSeconds: Long = 0L,
    val totalFormatted: String = "0分钟",
    val averageDailySeconds: Long = 0L,
    val averageDailyFormatted: String = "0分钟",
    val sessionCount: Int = 0,
    val firstReadDate: String = "",
    val lastReadDate: String = "",
    val dailyStats: List<DailyStat> = emptyList(),
    val bookStats: List<BookStat> = emptyList(),
    val weeklyStats: List<DailyStat> = emptyList(),
    val readingDays: Int = 0,
    val longestSessionSeconds: Long = 0L,
    val longestSessionFormatted: String = "0分钟",
    val averageSessionSeconds: Long = 0L,
    val averageSessionFormatted: String = "0分钟",
    val totalBooks: Int = 0
)

data class BookStat(
    val bookName: String,
    val totalSeconds: Long,
    val totalFormatted: String,
    val sessionCount: Int,
    val firstReadDate: String,
    val lastReadDate: String,
    val dailyStats: List<DailyStat> = emptyList(),
    val weeklyStats: List<DailyStat> = emptyList(),
    val averageSessionSeconds: Long = 0L,
    val averageSessionFormatted: String = "0分钟",
    val longestSessionSeconds: Long = 0L,
    val longestSessionFormatted: String = "0分钟",
    val readingDays: Int = 0,
    val averageDailySeconds: Long = 0L,
    val averageDailyFormatted: String = "0分钟"
)

data class DailyStat(
    val date: String,
    val seconds: Long
)

@Composable
fun StatisticsScreen(
    viewModel: MainViewModel = viewModel(),
    onBackClick: () -> Unit,
    onBookStatClick: (String) -> Unit = {},
    scrollState: androidx.compose.foundation.ScrollState? = null
) {
    val context = LocalContext.current
    val stats = remember { mutableStateOf<ReadingTimeStats?>(null) }
    val isLoading = remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())

    LaunchedEffect(Unit) {
        scope.launch {
            isLoading.value = true
            val calculatedStats = withContext(Dispatchers.IO) {
                calculateReadingTimeStats(context)
            }
            stats.value = calculatedStats
            isLoading.value = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = "阅读统计",
                scrollBehavior = scrollBehavior
            )
        },
        popupHost = {}
    ) { paddingValues ->
        if (isLoading.value) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            stats.value?.let { readingStats ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = paddingValues.calculateTopPadding() + 16.dp,
                        end = 16.dp,
                        bottom = paddingValues.calculateBottomPadding() + 40.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (readingStats.totalSeconds == 0L) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.defaultColors(
                                    color = MiuixTheme.colorScheme.primaryContainer
                                ),
                                insideMargin = PaddingValues(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = MiuixIcons.Info,
                                        contentDescription = null,
                                        tint = MiuixTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "暂无阅读数据？\n请前往主页右上角菜单点击“同步数据”以获取手环记录。",
                                        style = MiuixTheme.textStyles.body2,
                                        color = MiuixTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            insideMargin = PaddingValues(24.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.Timer,
                                    contentDescription = null,
                                    tint = MiuixTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "总阅读时长",
                                    style = MiuixTheme.textStyles.body2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantActions
                                )
                                Text(
                                    text = readingStats.totalFormatted,
                                    style = MiuixTheme.textStyles.title1,
                                    fontWeight = FontWeight.Bold,
                                    color = MiuixTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    item {
                        SmallTitle(text = "概览")
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            insideMargin = PaddingValues(16.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    GridStatItem(
                                        icon = MiuixIcons.Recent,
                                        label = "阅读次数",
                                        value = "${readingStats.sessionCount}次",
                                        modifier = Modifier.weight(1f)
                                    )
                                    GridStatItem(
                                        icon = MiuixIcons.Months,
                                        label = "日均阅读",
                                        value = readingStats.averageDailyFormatted,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    GridStatItem(
                                        icon = MiuixIcons.Notes,
                                        label = "书籍数量",
                                        value = "${readingStats.totalBooks}本",
                                        modifier = Modifier.weight(1f)
                                    )
                                    GridStatItem(
                                        icon = MiuixIcons.Timer,
                                        label = "最长单次",
                                        value = readingStats.longestSessionFormatted,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    if (readingStats.weeklyStats.isNotEmpty()) {
                        item {
                            SmallTitle(text = "本周趋势")
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                insideMargin = PaddingValues(16.dp)
                            ) {
                                WeeklyStatsChart(weeklyStats = readingStats.weeklyStats)
                            }
                        }
                    }

                    if (readingStats.dailyStats.isNotEmpty()) {
                        item {
                            SmallTitle(text = "每日记录")
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                insideMargin = PaddingValues(16.dp)
                            ) {
                                DailyStatsChart(dailyStats = readingStats.dailyStats)
                            }
                        }
                    }

                    if (readingStats.bookStats.isNotEmpty()) {
                        item {
                            SmallTitle(text = "书籍排行")
                            Card(modifier = Modifier.fillMaxWidth()) {
                                readingStats.bookStats.forEachIndexed { index, bookStat ->
                                    BookStatRow(
                                        index = index + 1,
                                        bookStat = bookStat,
                                        onClick = { onBookStatClick(bookStat.bookName) }
                                    )
                                }
                            }
                        }
                    }
                }
            } ?: run {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Report,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MiuixTheme.colorScheme.onSurfaceVariantActions
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "暂无阅读记录",
                            style = MiuixTheme.textStyles.body1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantActions
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GridStatItem(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        insideMargin = PaddingValues(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MiuixTheme.textStyles.title4,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantActions
            )
        }
    }
}

@Composable
fun BookStatRow(
    index: Int,
    bookStat: BookStat,
    onClick: () -> Unit
) {
    SuperArrow(
        title = bookStat.bookName,
        summary = "${bookStat.totalFormatted} · ${bookStat.sessionCount}次阅读",
        startAction = {
            Text(
                text = "$index",
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                modifier = Modifier
                    .width(32.dp)
                    .padding(end = 8.dp),
                fontWeight = FontWeight.Bold
            )
        },
        onClick = onClick
    )
}

@Composable
fun WeeklyStatsChart(weeklyStats: List<DailyStat>) {
    val maxSeconds = weeklyStats.maxOfOrNull { it.seconds } ?: 1L
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val weekDayFormat = SimpleDateFormat("E", Locale.getDefault())

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        weeklyStats.forEach { stat ->
            val heightPercent = if (maxSeconds > 0) {
                (stat.seconds.toFloat() / maxSeconds.toFloat()).coerceIn(0.05f, 1f)
            } else {
                0.05f
            }

            val date = try {
                dateFormat.parse(stat.date)
            } catch (e: Exception) {
                null
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height((100.dp * heightPercent))
                            .background(
                                color = if (stat.seconds > 0) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                            )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (date != null) {
                    Text(
                        text = weekDayFormat.format(date),
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantActions
                    )
                }
            }
        }
    }
}

@Composable
fun DailyStatsChart(dailyStats: List<DailyStat>) {
    val maxSeconds = dailyStats.maxOfOrNull { it.seconds } ?: 1L
    val recentStats = dailyStats.takeLast(30)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.Start),
        verticalAlignment = Alignment.Bottom
    ) {
        recentStats.forEach { stat ->
            val heightPercent = if (maxSeconds > 0) {
                (stat.seconds.toFloat() / maxSeconds.toFloat()).coerceIn(0.05f, 1f)
            } else {
                0.05f
            }

            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(80.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((80.dp * heightPercent))
                        .background(
                            color = MiuixTheme.colorScheme.primary.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                        )
                )
            }
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantActions
        )
        Text(
            text = value,
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.Medium,
            color = MiuixTheme.colorScheme.onSurface
        )
    }
}

suspend fun calculateReadingTimeStats(context: Context): ReadingTimeStats {
    val readingTimePrefs = context.getSharedPreferences("reading_time_prefs", Context.MODE_PRIVATE)
    val allKeys = readingTimePrefs.all.keys

    var totalSeconds = 0L
    var sessionCount = 0
    val allSessions = mutableListOf<Pair<String, Long>>()
    var firstReadDate: String? = null
    var lastReadDate: String? = null
    val bookStatsList = mutableListOf<BookStat>()

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val displayDateFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())

    allKeys.forEach { key ->
        if (key.endsWith("_total_seconds")) {
            val bookName = key.removeSuffix("_total_seconds")
            val seconds = readingTimePrefs.getLong(key, 0L)
            totalSeconds += seconds

            var bookSessionCount = 0
            var bookFirstDate: String? = null
            var bookLastDate: String? = null

            val sessionsJson = readingTimePrefs.getString("${bookName}_sessions", null)
            if (sessionsJson != null) {
                try {
                    val sessionsArray = org.json.JSONArray(sessionsJson)
                    for (i in 0 until sessionsArray.length()) {
                        val session = sessionsArray.getJSONObject(i)
                        val duration = session.optLong("duration", 0L)
                        if (duration > 0) {
                            var dateStr = session.optString("date", "")

                            if (dateStr.isEmpty()) {
                                val startTime = session.optLong("startTime", 0L)
                                if (startTime > 0) {
                                    val date = java.util.Date(startTime)
                                    dateStr = dateFormat.format(date)
                                }
                            }

                            if (dateStr.isNotEmpty()) {
                                allSessions.add(dateStr to duration)
                                sessionCount++
                                bookSessionCount++

                                if (bookFirstDate == null || dateStr < bookFirstDate!!) {
                                    bookFirstDate = dateStr
                                }
                                if (bookLastDate == null || dateStr > bookLastDate!!) {
                                    bookLastDate = dateStr
                                }

                                if (firstReadDate == null || dateStr < firstReadDate!!) {
                                    firstReadDate = dateStr
                                }
                                if (lastReadDate == null || dateStr > lastReadDate!!) {
                                    lastReadDate = dateStr
                                }
                            } else if (duration > 0) {
                                sessionCount++
                                bookSessionCount++
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }

            val bookFirstDatePref = readingTimePrefs.getString("${bookName}_first_read_date", null)
            val bookLastDatePref = readingTimePrefs.getString("${bookName}_last_read_date", null)

            if (bookFirstDatePref != null) {
                if (bookFirstDate == null || bookFirstDatePref < bookFirstDate!!) {
                    bookFirstDate = bookFirstDatePref
                }
                if (firstReadDate == null || bookFirstDatePref < firstReadDate!!) {
                    firstReadDate = bookFirstDatePref
                }
            }
            if (bookLastDatePref != null) {
                if (bookLastDate == null || bookLastDatePref > bookLastDate!!) {
                    bookLastDate = bookLastDatePref
                }
                if (lastReadDate == null || bookLastDatePref > lastReadDate!!) {
                    lastReadDate = bookLastDatePref
                }
            }

            if (bookSessionCount == 0 && seconds > 0) {
                if (bookFirstDate == null && bookFirstDatePref != null) {
                    bookFirstDate = bookFirstDatePref
                }
                if (bookLastDate == null && bookLastDatePref != null) {
                    bookLastDate = bookLastDatePref
                }
            }

            if (seconds > 0) {
                bookStatsList.add(
                    BookStat(
                        bookName = bookName,
                        totalSeconds = seconds,
                        totalFormatted = formatDuration(seconds),
                        sessionCount = bookSessionCount,
                        firstReadDate = bookFirstDate?.let {
                            try {
                                dateFormat.parse(it)?.let { date -> displayDateFormat.format(date) }
                                    ?: it
                            } catch (e: Exception) {
                                it
                            }
                        } ?: "",
                        lastReadDate = bookLastDate?.let {
                            try {
                                dateFormat.parse(it)?.let { date -> displayDateFormat.format(date) }
                                    ?: it
                            } catch (e: Exception) {
                                it
                            }
                        } ?: ""
                    )
                )
            }
        }
    }

    val uniqueDays = allSessions.map { it.first }.distinct().size
    val averageDailySeconds = if (uniqueDays > 0) {
        totalSeconds / uniqueDays
    } else if (firstReadDate != null && lastReadDate != null) {
        try {
            val firstDate = dateFormat.parse(firstReadDate)
            val lastDate = dateFormat.parse(lastReadDate)
            if (firstDate != null && lastDate != null) {
                val daysDiff =
                    maxOf(1, ((lastDate.time - firstDate.time) / (1000 * 60 * 60 * 24)).toInt() + 1)
                totalSeconds / daysDiff
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    } else {
        0L
    }

    val allSessionDurations = allSessions.map { it.second }
    val longestSessionSeconds = allSessionDurations.maxOrNull() ?: 0L
    val averageSessionSeconds = if (sessionCount > 0) {
        totalSeconds / sessionCount
    } else {
        0L
    }

    val dailyStatsMap = mutableMapOf<String, Long>()
    allSessions.forEach { (date, duration) ->
        dailyStatsMap[date] = (dailyStatsMap[date] ?: 0L) + duration
    }

    val dailyStats = dailyStatsMap.map { (date, seconds) ->
        DailyStat(date = date, seconds = seconds)
    }.sortedBy { it.date }

    val calendar = Calendar.getInstance()
    val today = calendar.time
    calendar.add(Calendar.DAY_OF_YEAR, -6)
    val sevenDaysAgo = calendar.time
    val sevenDaysAgoStr = dateFormat.format(sevenDaysAgo)
    val todayStr = dateFormat.format(today)

    val weeklyStats = dailyStats.filter { stat ->
        stat.date >= sevenDaysAgoStr && stat.date <= todayStr
    }

    val weeklyStatsMap = mutableMapOf<String, Long>()
    weeklyStats.forEach { stat ->
        weeklyStatsMap[stat.date] = stat.seconds
    }

    val completeWeeklyStats = mutableListOf<DailyStat>()
    calendar.time = sevenDaysAgo
    for (i in 0..6) {
        val dateStr = dateFormat.format(calendar.time)
        completeWeeklyStats.add(
            DailyStat(
                date = dateStr,
                seconds = weeklyStatsMap[dateStr] ?: 0L
            )
        )
        calendar.add(Calendar.DAY_OF_YEAR, 1)
    }

    return ReadingTimeStats(
        totalSeconds = totalSeconds,
        totalFormatted = formatDuration(totalSeconds),
        averageDailySeconds = averageDailySeconds,
        averageDailyFormatted = formatDuration(averageDailySeconds),
        sessionCount = sessionCount,
        firstReadDate = firstReadDate?.let {
            try {
                dateFormat.parse(it)?.let { date -> displayDateFormat.format(date) } ?: it
            } catch (e: Exception) {
                it
            }
        } ?: "",
        lastReadDate = lastReadDate?.let {
            try {
                dateFormat.parse(it)?.let { date -> displayDateFormat.format(date) } ?: it
            } catch (e: Exception) {
                it
            }
        } ?: "",
        dailyStats = dailyStats,
        bookStats = bookStatsList.sortedByDescending { it.totalSeconds },
        weeklyStats = completeWeeklyStats,
        readingDays = uniqueDays,
        longestSessionSeconds = longestSessionSeconds,
        longestSessionFormatted = formatDuration(longestSessionSeconds),
        averageSessionSeconds = averageSessionSeconds,
        averageSessionFormatted = formatDuration(averageSessionSeconds),
        totalBooks = bookStatsList.size
    )
}

suspend fun calculateBookReadingTimeStats(context: Context, bookName: String): BookStat? {
    val readingTimePrefs = context.getSharedPreferences("reading_time_prefs", Context.MODE_PRIVATE)
    val totalSeconds = readingTimePrefs.getLong("${bookName}_total_seconds", 0L)

    if (totalSeconds == 0L) {
        return null
    }

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val displayDateFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())

    var sessionCount = 0
    var firstReadDate: String? = null
    var lastReadDate: String? = null
    val allSessions = mutableListOf<Pair<String, Long>>()
    val sessionDurations = mutableListOf<Long>()

    val sessionsJson = readingTimePrefs.getString("${bookName}_sessions", null)
    if (sessionsJson != null) {
        try {
            val sessionsArray = org.json.JSONArray(sessionsJson)
            for (i in 0 until sessionsArray.length()) {
                val session = sessionsArray.getJSONObject(i)
                val duration = session.optLong("duration", 0L)
                if (duration > 0) {
                    var dateStr = session.optString("date", "")

                    if (dateStr.isEmpty()) {
                        val startTime = session.optLong("startTime", 0L)
                        if (startTime > 0) {
                            val date = java.util.Date(startTime)
                            dateStr = dateFormat.format(date)
                        }
                    }

                    if (dateStr.isNotEmpty()) {
                        allSessions.add(dateStr to duration)
                        sessionDurations.add(duration)
                        sessionCount++

                        if (firstReadDate == null || dateStr < firstReadDate!!) {
                            firstReadDate = dateStr
                        }
                        if (lastReadDate == null || dateStr > lastReadDate!!) {
                            lastReadDate = dateStr
                        }
                    } else {
                        sessionDurations.add(duration)
                        sessionCount++
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    val bookFirstDate = readingTimePrefs.getString("${bookName}_first_read_date", null)
    val bookLastDate = readingTimePrefs.getString("${bookName}_last_read_date", null)

    if (bookFirstDate != null && (firstReadDate == null || bookFirstDate < firstReadDate!!)) {
        firstReadDate = bookFirstDate
    }
    if (bookLastDate != null && (lastReadDate == null || bookLastDate > lastReadDate!!)) {
        lastReadDate = bookLastDate
    }

    val dailyStatsMap = mutableMapOf<String, Long>()
    allSessions.forEach { (date, duration) ->
        dailyStatsMap[date] = (dailyStatsMap[date] ?: 0L) + duration
    }

    val dailyStats = dailyStatsMap.map { (date, seconds) ->
        DailyStat(date = date, seconds = seconds)
    }.sortedBy { it.date }

    val calendar = Calendar.getInstance()
    val today = calendar.time
    calendar.add(Calendar.DAY_OF_YEAR, -6)
    val sevenDaysAgo = calendar.time
    val sevenDaysAgoStr = dateFormat.format(sevenDaysAgo)
    val todayStr = dateFormat.format(today)

    val weeklyStats = dailyStats.filter { stat ->
        stat.date >= sevenDaysAgoStr && stat.date <= todayStr
    }

    val weeklyStatsMap = mutableMapOf<String, Long>()
    weeklyStats.forEach { stat ->
        weeklyStatsMap[stat.date] = stat.seconds
    }

    val completeWeeklyStats = mutableListOf<DailyStat>()
    calendar.time = sevenDaysAgo
    for (i in 0..6) {
        val dateStr = dateFormat.format(calendar.time)
        completeWeeklyStats.add(
            DailyStat(
                date = dateStr,
                seconds = weeklyStatsMap[dateStr] ?: 0L
            )
        )
        calendar.add(Calendar.DAY_OF_YEAR, 1)
    }

    val readingDays = allSessions.map { it.first }.distinct().size
    val longestSessionSeconds = sessionDurations.maxOrNull() ?: 0L
    val averageSessionSeconds = if (sessionCount > 0) {
        totalSeconds / sessionCount
    } else {
        0L
    }
    val averageDailySeconds = if (readingDays > 0) {
        totalSeconds / readingDays
    } else if (firstReadDate != null && lastReadDate != null) {
        try {
            val firstDate = dateFormat.parse(firstReadDate)
            val lastDate = dateFormat.parse(lastReadDate)
            if (firstDate != null && lastDate != null) {
                val daysDiff =
                    maxOf(1, ((lastDate.time - firstDate.time) / (1000 * 60 * 60 * 24)).toInt() + 1)
                totalSeconds / daysDiff
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    } else {
        0L
    }

    return BookStat(
        bookName = bookName,
        totalSeconds = totalSeconds,
        totalFormatted = formatDuration(totalSeconds),
        sessionCount = sessionCount,
        firstReadDate = firstReadDate?.let {
            try {
                dateFormat.parse(it)?.let { date -> displayDateFormat.format(date) } ?: it
            } catch (e: Exception) {
                it
            }
        } ?: "",
        lastReadDate = lastReadDate?.let {
            try {
                dateFormat.parse(it)?.let { date -> displayDateFormat.format(date) } ?: it
            } catch (e: Exception) {
                it
            }
        } ?: "",
        dailyStats = dailyStats,
        weeklyStats = completeWeeklyStats,
        averageSessionSeconds = averageSessionSeconds,
        averageSessionFormatted = formatDuration(averageSessionSeconds),
        longestSessionSeconds = longestSessionSeconds,
        longestSessionFormatted = formatDuration(longestSessionSeconds),
        readingDays = readingDays,
        averageDailySeconds = averageDailySeconds,
        averageDailyFormatted = formatDuration(averageDailySeconds)
    )
}

fun formatDuration(seconds: Long): String {
    if (seconds < 60) return "${seconds}秒"
    val minutes = seconds / 60
    if (minutes < 60) return "${minutes}分钟"
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return if (remainingMinutes > 0) {
        "${hours}小时${remainingMinutes}分钟"
    } else {
        "${hours}小时"
    }
}

@Composable
fun BookStatisticsScreen(
    bookName: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val stats = remember(bookName) { mutableStateOf<BookStat?>(null) }
    val isLoading = remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())

    BackHandler(enabled = true) {
        onBackClick()
    }

    LaunchedEffect(bookName) {
        scope.launch {
            isLoading.value = true
            val calculatedStats = withContext(Dispatchers.IO) {
                calculateBookReadingTimeStats(context, bookName)
            }
            stats.value = calculatedStats
            isLoading.value = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = bookName,
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.padding(start = 6.dp)
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = "返回"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        popupHost = {}
    ) { paddingValues ->
        if (isLoading.value) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            stats.value?.let { bookStats ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = paddingValues.calculateTopPadding() + 16.dp,
                        end = 16.dp,
                        bottom = paddingValues.calculateBottomPadding() + if (bookName.isEmpty()) 80.dp else 100.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            insideMargin = PaddingValues(24.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "总阅读时长",
                                    style = MiuixTheme.textStyles.body2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantActions
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = bookStats.totalFormatted,
                                    style = MiuixTheme.textStyles.title1,
                                    fontWeight = FontWeight.Bold,
                                    color = MiuixTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    item {
                        SmallTitle(text = "详情")
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            insideMargin = PaddingValues(16.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    GridStatItem(
                                        icon = MiuixIcons.Recent,
                                        label = "阅读次数",
                                        value = "${bookStats.sessionCount}次",
                                        modifier = Modifier.weight(1f)
                                    )
                                    GridStatItem(
                                        icon = MiuixIcons.Months,
                                        label = "阅读天数",
                                        value = "${bookStats.readingDays}天",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    GridStatItem(
                                        icon = MiuixIcons.Timer,
                                        label = "日均阅读",
                                        value = bookStats.averageDailyFormatted,
                                        modifier = Modifier.weight(1f)
                                    )
                                    GridStatItem(
                                        icon = MiuixIcons.Timer,
                                        label = "平均单次",
                                        value = bookStats.averageSessionFormatted,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                GridStatItem(
                                    icon = MiuixIcons.Timer,
                                    label = "最长单次",
                                    value = bookStats.longestSessionFormatted,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    if (bookStats.firstReadDate.isNotEmpty() || bookStats.lastReadDate.isNotEmpty()) {
                        item {
                            SmallTitle(text = "时间记录")
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                insideMargin = PaddingValues(16.dp)
                            ) {
                                Column {
                                    if (bookStats.firstReadDate.isNotEmpty()) {
                                        InfoRow("首次阅读", bookStats.firstReadDate)
                                        if (bookStats.lastReadDate.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                        }
                                    }
                                    if (bookStats.lastReadDate.isNotEmpty()) {
                                        InfoRow("最后阅读", bookStats.lastReadDate)
                                    }
                                }
                            }
                        }
                    }

                    if (bookStats.weeklyStats.isNotEmpty()) {
                        item {
                            SmallTitle(text = "本周趋势")
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                insideMargin = PaddingValues(16.dp)
                            ) {
                                WeeklyStatsChart(weeklyStats = bookStats.weeklyStats)
                            }
                        }
                    }

                    if (bookStats.dailyStats.isNotEmpty()) {
                        item {
                            SmallTitle(text = "每日记录")
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                insideMargin = PaddingValues(16.dp)
                            ) {
                                DailyStatsChart(dailyStats = bookStats.dailyStats)
                            }
                        }
                    }
                }
            } ?: run {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无阅读记录",
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantActions
                    )
                }
            }
        }
    }
}
