package com.bandbbs.ebook.ui.screens

import android.R
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.bandbbs.ebook.utils.BuiltinFileManagerPaths
import com.bandbbs.ebook.utils.ImportPathPreset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Clear
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.File
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.icon.extended.Send
import top.yukonga.miuix.kmp.theme.MiuixTheme

private data class FileItem(
    val file: File,
    val name: String,
    val isDirectory: Boolean,
    val summary: String
)

private val SUPPORTED_IMPORT_EXTENSIONS = setOf("txt", "epub", "nvb", "docx", "pdf", "mobi")

private fun isSupportedImportFile(file: File): Boolean {
    if (file.isDirectory) return true
    val extension = file.extension.lowercase()
    return extension in SUPPORTED_IMPORT_EXTENSIONS
}

@Composable
fun BuiltinFileManagerScreen(
    onBackClick: () -> Unit,
    onSelectFiles: (List<File>) -> Unit
) {
    val context = LocalContext.current
    val availablePresets = remember { BuiltinFileManagerPaths.resolvedDefaults(context) }

    var currentDir by remember { mutableStateOf<File?>(null) }
    var isPush by remember { mutableStateOf(true) }
    var selectedFiles by remember { mutableStateOf(setOf<File>()) }

    var isBackSwiping by remember { mutableStateOf(false) }
    var backSwipeProgress by remember { mutableFloatStateOf(0f) }
    val animatedBackProgress by animateFloatAsState(
        targetValue = if (isBackSwiping) backSwipeProgress else 0f,
        label = "backProgress"
    )

    val parentDir = remember(currentDir, availablePresets) {
        if (currentDir == null) null
        else {
            val isRootPreset = availablePresets.any { it.second.absolutePath == currentDir?.absolutePath }
            if (isRootPreset) null else currentDir?.parentFile
        }
    }

    val handleBack = {
        if (currentDir != null) {
            isPush = false
            currentDir = parentDir
        } else {
            onBackClick()
        }
    }

    PredictiveBackHandler(enabled = currentDir != null) { progress ->
        isBackSwiping = true
        try {
            progress.collect { backEvent ->
                backSwipeProgress = backEvent.progress
            }
            handleBack()
        } catch (_: CancellationException) {
        } finally {
            isBackSwiping = false
            backSwipeProgress = 0f
        }
    }

    Scaffold(popupHost = {}) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize(),

        ) {
            if (isBackSwiping) {
                FileManagerPage(
                    dir = parentDir,
                    availablePresets = availablePresets,
                    selectedFiles = selectedFiles,
                    onSelectFilesChange = {},
                    onOpenDir = {},
                    onBack = {},
                    onConfirm = {},
                    modifier = Modifier.graphicsLayer {
                        val scale = 0.95f + (animatedBackProgress * 0.05f)
                        scaleX = scale
                        scaleY = scale
                        alpha = 0.5f + (animatedBackProgress * 0.5f)
                    }
                )
            }

            AnimatedContent(
                targetState = currentDir,
                label = "FileManagerTransition",
                transitionSpec = {
                    val animationSpec = spring<IntOffset>(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                    if (isPush) {
                        (slideInHorizontally(animationSpec) { it } + fadeIn()) togetherWith
                                (slideOutHorizontally(animationSpec) { -it } + fadeOut())
                    } else {
                        (slideInHorizontally(animationSpec) { -it } + fadeIn()) togetherWith
                                (slideOutHorizontally(animationSpec) { it } + fadeOut())
                    }
                }
            ) { targetDir ->
                FileManagerPage(
                    dir = targetDir,
                    availablePresets = availablePresets,
                    selectedFiles = selectedFiles,
                    onSelectFilesChange = { selectedFiles = it },
                    onOpenDir = { file ->
                        isPush = true
                        currentDir = file
                    },
                    onBack = handleBack,
                    onConfirm = {
                        onSelectFiles(selectedFiles.toList())
                        selectedFiles = emptySet()
                    },
                    modifier = Modifier.graphicsLayer {
                        if (isBackSwiping && targetDir == currentDir) {
                            translationX = animatedBackProgress * size.width
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun FileManagerPage(
    dir: File?,
    availablePresets: List<Pair<ImportPathPreset , File>>,
    selectedFiles: Set<File>,
    onSelectFilesChange: (Set<File>) -> Unit,
    onOpenDir: (File) -> Unit,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember(dir) { mutableStateOf("") }
    var pathInput by remember(dir) { mutableStateOf("") }
    var fileItems by remember(dir) { mutableStateOf<List<FileItem>>(emptyList()) }
    var isLoading by remember(dir) { mutableStateOf(false) }

    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())

    LaunchedEffect(dir) {
        if (dir != null) {
            isLoading = true
            fileItems = withContext(Dispatchers.IO) {
                val files = dir.listFiles()
                    ?.filter { it.exists() && it.canRead() && isSupportedImportFile(it) }
                    ?: emptyList()
                files.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })
                    .map {
                        FileItem(
                            file = it,
                            name = it.name,
                            isDirectory = it.isDirectory,
                            summary = if (it.isDirectory) "文件夹" else Formatter.formatFileSize(context, it.length())
                        )
                    }
            }
            isLoading = false
        } else {
            fileItems = emptyList()
        }
    }

    val normalizedQuery = searchQuery.trim()
    val normalizedQueryLower = normalizedQuery.lowercase()
    val isSuffixFilter = normalizedQueryLower.startsWith(".") && normalizedQueryLower.length > 1

    val filteredPresets = remember(availablePresets, normalizedQuery) {
        if (normalizedQuery.isBlank()) availablePresets
        else availablePresets.filter { (preset, file) ->
            preset.label.contains(normalizedQuery, ignoreCase = true) ||
                    file.absolutePath.contains(normalizedQuery, ignoreCase = true)
        }
    }

    val filteredFileItems = remember(fileItems, normalizedQueryLower, isSuffixFilter) {
        if (normalizedQueryLower.isBlank()) fileItems
        else fileItems.filter { item ->
            item.name.contains(normalizedQueryLower, ignoreCase = true) ||
                    (isSuffixFilter && !item.isDirectory && item.name.lowercase().endsWith(normalizedQueryLower))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = if (dir == null) "选择文件" else dir.name,
                largeTitle = if (dir == null) "选择文件" else dir.name,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.padding(start = 6.dp)) {
                        Icon(imageVector = MiuixIcons.Back, contentDescription = "返回")
                    }
                },
                actions = {
                    AnimatedVisibility(
                        visible = selectedFiles.isNotEmpty(),
                        enter = fadeIn() + slideInHorizontally { it / 2 },
                        exit = fadeOut() + slideOutHorizontally { it / 2 },
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onSelectFilesChange(emptySet()) }) {
                                Icon(imageVector = MiuixIcons.Close, contentDescription = "清空选择")
                            }
                            IconButton(onClick = onConfirm) {
                                Icon(imageVector = MiuixIcons.Send, contentDescription = "导入")
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 8.dp,
                end = 16.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp
            )
        ) {
            if (dir == null) {
                item {
                    TextField(
                        value = pathInput,
                        onValueChange = { pathInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        label = "输入路径并打开",
                        leadingIcon = {
                            Icon(
                                MiuixIcons.Folder,
                                contentDescription = "路径",
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (pathInput.isNotBlank()) {
                                    IconButton(onClick = { pathInput = "" }, modifier = Modifier.padding(end = 6.dp)) {
                                        Icon(MiuixIcons.Close, contentDescription = "清除")
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        val input = pathInput.trim()
                                        if (input.isEmpty()) {
                                            Toast.makeText(context, "请输入路径", Toast.LENGTH_SHORT).show()
                                            return@IconButton
                                        }
                                        val target = File(input)
                                        when {
                                            !target.exists() -> Toast.makeText(context, "路径不存在", Toast.LENGTH_SHORT).show()
                                            !target.isDirectory -> Toast.makeText(context, "该路径不是文件夹", Toast.LENGTH_SHORT).show()
                                            !target.canRead() -> Toast.makeText(context, "无法读取该路径", Toast.LENGTH_SHORT).show()
                                            else -> {
                                                onOpenDir(target)
                                            }
                                        }
                                    }
                                ) {
                                    Icon(MiuixIcons.Send, contentDescription = "打开路径")
                                }
                            }
                        },
                        singleLine = true
                    )
                }
            }

            item {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    label = if (dir == null) "搜索路径/后缀（如 .epub）..." else "搜索文件/后缀（如 .epub）...",
                    leadingIcon = {
                        Icon(
                            MiuixIcons.Search,
                            contentDescription = "搜索",
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.padding(end = 12.dp)
                            ) {
                                Icon(MiuixIcons.Close, contentDescription = "清除")
                            }
                        }
                    } else null,
                    singleLine = true
                )
            }

            if (dir == null) {
                item { SmallTitle(text = "快捷路径") }
                item {
                    Card(
                        insideMargin = PaddingValues(0.dp)
                    ) {
                        if (filteredPresets.isEmpty()) {
                            BasicComponent(title = if (availablePresets.isEmpty()) "暂无可用路径" else "未找到匹配路径")
                        } else {
                            filteredPresets.forEach { (preset, file) ->
                                SuperArrow(
                                    title = preset.label,
                                    summary = file.absolutePath,
                                    startAction = {
                                        Icon(
                                            imageVector = MiuixIcons.Folder,
                                            contentDescription = "Folder",
                                            tint = MiuixTheme.colorScheme.primary,
                                            modifier = Modifier.padding(end = 16.dp)
                                        )
                                    },
                                    onClick = { onOpenDir(file) }
                                )
                            }
                        }
                    }
                }
            } else {
                item { SmallTitle(text = "当前路径: ${dir.absolutePath}") }

                if (isLoading) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 64.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "加载中...",
                                style = MiuixTheme.textStyles.body1,
                                color = MiuixTheme.colorScheme.onBackgroundVariant
                            )
                        }
                    }
                } else if (filteredFileItems.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 64.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Clear,
                                contentDescription = "Empty",
                                modifier = Modifier.size(64.dp),
                                tint = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (normalizedQueryLower.isBlank()) "这里什么都没有" else "未找到匹配文件",
                                style = MiuixTheme.textStyles.body1,
                                color = MiuixTheme.colorScheme.onBackgroundVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    item {
                        Column {
                            filteredFileItems.forEachIndexed { index, item ->
                                val isFirst = index == 0
                                val isLast = index == filteredFileItems.lastIndex

                                val shape = when {
                                    filteredFileItems.size == 1 -> RoundedCornerShape(16.dp)
                                    isFirst -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                                    isLast -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                                    else -> RectangleShape
                                }

                                val isSelected = selectedFiles.contains(item.file)

                                Card(
                                    modifier = Modifier.clip(shape),
                                    cornerRadius = 0.dp,
                                    onClick = {
                                        if (item.isDirectory) onOpenDir(item.file)
                                        else {
                                            val newSet = if (isSelected) selectedFiles - item.file else selectedFiles + item.file
                                            onSelectFilesChange(newSet)
                                        }
                                    }
                                ) {
                                    if (item.isDirectory) {
                                        SuperArrow(
                                            title = item.name,
                                            summary = item.summary,
                                            startAction = {
                                                Icon(
                                                    imageVector = MiuixIcons.Folder,
                                                    contentDescription = "Folder",
                                                    tint = MiuixTheme.colorScheme.onBackgroundVariant,
                                                    modifier = Modifier.padding(end = 16.dp)
                                                )
                                            },
                                            onClick = { onOpenDir(item.file) }
                                        )
                                    } else {
                                        BasicComponent(
                                            title = item.name,
                                            summary = item.summary,
                                            startAction = {
                                                Icon(
                                                    imageVector = MiuixIcons.File,
                                                    contentDescription = "File",
                                                    tint = if (isSelected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onBackgroundVariant,
                                                    modifier = Modifier.padding(end = 16.dp)
                                                )
                                            },
                                            endActions = {
                                                Checkbox(
                                                    checked = isSelected,
                                                    onCheckedChange = null
                                                )
                                            },
                                            onClick = {
                                                val newSet = if (isSelected) selectedFiles - item.file else selectedFiles + item.file
                                                onSelectFilesChange(newSet)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
