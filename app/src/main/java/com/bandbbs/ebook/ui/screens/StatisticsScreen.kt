package com.bandbbs.ebook.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bandbbs.ebook.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

@OptIn(ExperimentalMaterial3Api::class)
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
    val localScrollState = rememberScrollState()
    val actualScrollState = scrollState ?: localScrollState

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
                title = { Text("统计") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 80.dp)
                        .verticalScroll(actualScrollState)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))


                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 0.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {

                            StatCard(
                                title = "总阅读时长",
                                value = readingStats.totalFormatted
                            )

                            Spacer(modifier = Modifier.height(16.dp))


                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatInfoCard(
                                    title = "阅读次数",
                                    value = "${readingStats.sessionCount}次",
                                    modifier = Modifier.weight(1f)
                                )
                                StatInfoCard(
                                    title = "日均阅读",
                                    value = readingStats.averageDailyFormatted,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatInfoCard(
                                    title = "阅读天数",
                                    value = "${readingStats.readingDays}天",
                                    modifier = Modifier.weight(1f)
                                )
                                StatInfoCard(
                                    title = "书籍数量",
                                    value = "${readingStats.totalBooks}本",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatInfoCard(
                                    title = "最长单次",
                                    value = readingStats.longestSessionFormatted,
                                    modifier = Modifier.weight(1f)
                                )
                                StatInfoCard(
                                    title = "平均单次",
                                    value = readingStats.averageSessionFormatted,
                                    modifier = Modifier.weight(1f)
                                )
                            }


                            if (readingStats.weeklyStats.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "最近一周阅读时长",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                WeeklyStatsChart(weeklyStats = readingStats.weeklyStats)
                            }

                            if (readingStats.dailyStats.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "每日阅读时长",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                DailyStatsChart(dailyStats = readingStats.dailyStats)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))


                    if (readingStats.bookStats.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 0.dp
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "书籍统计",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                readingStats.bookStats.forEachIndexed { index, bookStat ->
                                    if (index > 0) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }

                                    BookStatRow(
                                        bookStat = bookStat,
                                        onClick = {
                                            onBookStatClick(bookStat.bookName)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
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
                        Text(
                            text = "暂无阅读记录",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun StatInfoCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
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
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun WeeklyStatsChart(weeklyStats: List<DailyStat>) {
    val maxSeconds = weeklyStats.maxOfOrNull { it.seconds } ?: 1L
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val dayFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
    val weekDayFormat = SimpleDateFormat("E", Locale.getDefault())

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        weeklyStats.forEach { stat ->
            val heightPercent = if (maxSeconds > 0) {
                (stat.seconds.toFloat() / maxSeconds.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
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
                        .height(120.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height((120.dp * heightPercent).coerceAtLeast(2.dp))
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = MaterialTheme.shapes.small
                            )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (date != null) {
                    Text(
                        text = weekDayFormat.format(date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 10.sp
                    )
                    Text(
                        text = dayFormat.format(date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontSize = 9.sp
                    )
                } else {
                    Text(
                        text = stat.date.takeLast(5),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 10.sp
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
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        recentStats.forEach { stat ->
            val heightPercent = if (maxSeconds > 0) {
                (stat.seconds.toFloat() / maxSeconds.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
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
                            .height((100.dp * heightPercent).coerceAtLeast(2.dp))
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = MaterialTheme.shapes.small
                            )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stat.date.takeLast(5),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun BookStatRow(
    bookStat: BookStat,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = bookStat.bookName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${bookStat.totalFormatted} · ${bookStat.sessionCount}次",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = "查看详情",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun BookReadingTimeDetailDialog(
    bookName: String,
    onDismiss: () -> Unit,
    context: Context
) {
    val bookStats = remember(bookName) {
        mutableStateOf<BookStat?>(null)
    }
    val isLoading = remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(bookName) {
        scope.launch {
            isLoading.value = true
            val stats = withContext(Dispatchers.IO) {
                calculateBookReadingTimeStats(context, bookName)
            }
            bookStats.value = stats
            isLoading.value = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = bookName,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            if (isLoading.value) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                bookStats.value?.let { stat ->
                    Column {
                        InfoRow("总阅读时长", stat.totalFormatted)
                        Spacer(modifier = Modifier.height(8.dp))
                        InfoRow("阅读次数", "${stat.sessionCount}次")
                        if (stat.firstReadDate.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            InfoRow("首次阅读", stat.firstReadDate)
                        }
                        if (stat.lastReadDate.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            InfoRow("最后阅读", stat.lastReadDate)
                        }
                    }
                } ?: run {
                    Text("暂无阅读记录")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        }
    )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookStatisticsScreen(
    bookName: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val stats = remember(bookName) { mutableStateOf<BookStat?>(null) }
    val isLoading = remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()


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
                title = {
                    Text(
                        text = bookName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 80.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(16.dp))


                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 0.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            StatCard(
                                title = "总阅读时长",
                                value = bookStats.totalFormatted
                            )

                            Spacer(modifier = Modifier.height(16.dp))


                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatInfoCard(
                                    title = "阅读次数",
                                    value = "${bookStats.sessionCount}次",
                                    modifier = Modifier.weight(1f)
                                )
                                StatInfoCard(
                                    title = "阅读天数",
                                    value = "${bookStats.readingDays}天",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))


                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatInfoCard(
                                    title = "日均阅读",
                                    value = bookStats.averageDailyFormatted,
                                    modifier = Modifier.weight(1f)
                                )
                                StatInfoCard(
                                    title = "平均单次",
                                    value = bookStats.averageSessionFormatted,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))


                            StatInfoCard(
                                title = "最长单次阅读",
                                value = bookStats.longestSessionFormatted,
                                modifier = Modifier.fillMaxWidth()
                            )


                            if (bookStats.firstReadDate.isNotEmpty() || bookStats.lastReadDate.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                if (bookStats.firstReadDate.isNotEmpty()) {
                                    InfoRow("首次阅读", bookStats.firstReadDate)
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                if (bookStats.lastReadDate.isNotEmpty()) {
                                    InfoRow("最后阅读", bookStats.lastReadDate)
                                }
                            }


                            if (bookStats.weeklyStats.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "最近一周阅读时长",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                WeeklyStatsChart(weeklyStats = bookStats.weeklyStats)
                            }


                            if (bookStats.dailyStats.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "每日阅读时长",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                DailyStatsChart(dailyStats = bookStats.dailyStats)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
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
                        Text(
                            text = "暂无阅读记录",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}
