package com.bandbbs.ebook.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bandbbs.ebook.ui.viewmodel.ImportState
import com.bandbbs.ebook.ui.viewmodel.RegexTemplate
import com.bandbbs.ebook.utils.ChapterSplitter
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.extra.CheckboxLocation
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.extra.SuperCheckbox
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.extra.SuperDropdown
import top.yukonga.miuix.kmp.extra.SuperSwitch
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.pressable

sealed class RenamePreviewResult {
    data class Success(val text: String) : RenamePreviewResult()
    data class Error(val text: String) : RenamePreviewResult()
}

@Composable
fun ImportBookBottomSheet(
    state: ImportState,
    categories: List<String>,
    existingBookNames: List<String>,
    onCancel: () -> Unit,
    onConfirm: (bookName: String, splitMethod: String, noSplit: Boolean, wordsPerChapter: Int, selectedCategory: String?, enableChapterMerge: Boolean, mergeMinWords: Int, enableChapterRename: Boolean, renamePattern: String, customRegex: String, selectedCustomRegexTemplateName: String?, customRegexTemplates: List<RegexTemplate>) -> Unit,
    onShowCategorySelector: () -> Unit
) {
    var bookName by remember { mutableStateOf(state.bookName) }
    var splitMethod by remember { mutableStateOf(state.splitMethod) }
    var noSplit by remember { mutableStateOf(state.noSplit) }
    var wordsPerChapterText by remember { mutableStateOf(state.wordsPerChapter.toString()) }
    var selectedCategory by remember { mutableStateOf(state.selectedCategory) }
    var enableChapterMerge by remember { mutableStateOf(state.enableChapterMerge) }
    var mergeMinWordsText by remember { mutableStateOf(state.mergeMinWords.toString()) }
    var enableChapterRename by remember { mutableStateOf(state.enableChapterRename) }
    var renamePattern by remember { mutableStateOf(state.renamePattern) }
    var customRegex by remember { mutableStateOf(state.customRegex) }
    var customRegexTemplates by remember { mutableStateOf(state.customRegexTemplates) }
    var selectedCustomRegexTemplateName by remember { mutableStateOf(state.selectedCustomRegexTemplateName) }
    val showRegexTemplateDialog = remember { mutableStateOf(false) }
    val showQuickSaveTemplateDialog = remember { mutableStateOf(false) }
    var quickSaveTemplateName by remember { mutableStateOf("") }

    val isMultipleFiles = state.isMultipleFiles
    val hasEpubOrNvb =
        remember(state.files) { state.files.any { it.fileFormat == "epub" || it.fileFormat == "nvb" } }
    val hasTxt =
        remember(state.files) { state.files.any { it.fileFormat == "txt" || it.fileFormat == "docx" || it.fileFormat == "pdf" } }
    val hasPdf =
        remember(state.files) { state.files.any { it.fileFormat == "pdf" } }

    val isBookNameExists = remember(bookName, existingBookNames) {
        !isMultipleFiles && bookName.trim()
            .isNotEmpty() && existingBookNames.contains(bookName.trim())
    }

    val renamePreview = remember(renamePattern) {
        if (renamePattern.isBlank()) null else {
            val parts = renamePattern.split(" -> ", limit = 2)
            if (parts.size == 2) {
                try {
                    val regex = Regex(parts[0].trim())
                    val result = regex.replace("示例章节标题") { matchResult ->
                        var res = parts[1].trim()
                        matchResult.groupValues.forEachIndexed { index, group ->
                            if (index > 0) res = res.replace("\$$index", group)
                        }
                        res
                    }
                    RenamePreviewResult.Success("预览: \"示例章节标题\" -> \"$result\"")
                } catch (e: Exception) {
                    RenamePreviewResult.Error("正则表达式格式错误")
                }
            } else null
        }
    }

    LaunchedEffect(state.selectedCategory) { selectedCategory = state.selectedCategory }
    LaunchedEffect(state.customRegex) {
        if (splitMethod == ChapterSplitter.METHOD_CUSTOM && state.customRegex.isNotBlank()) customRegex =
            state.customRegex
    }
    LaunchedEffect(state.customRegexTemplates) { customRegexTemplates = state.customRegexTemplates }
    LaunchedEffect(state.selectedCustomRegexTemplateName) {
        selectedCustomRegexTemplateName = state.selectedCustomRegexTemplateName
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(top = 4.dp)
    ) {
        Text(
            text = if (isMultipleFiles) "批量导入 (${state.files.size})" else "导入书籍",
            style = MiuixTheme.textStyles.title2,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        SmallTitle(text = "基础信息")
        if (isMultipleFiles) {
            Card(
                colors = CardDefaults.defaultColors(
                    color = MiuixTheme.colorScheme.secondaryVariant
                )
            ) {
                BasicComponent(
                    title = "待导入文件",
                    summary = state.files.take(3)
                        .joinToString("\n") { "• ${it.bookName} (${it.fileFormat.uppercase()})" } +
                            if (state.files.size > 3) "\n... 还有 ${state.files.size - 3} 个文件" else ""
                )
            }
        } else {
            TextField(
                value = bookName,
                onValueChange = { bookName = it },
                label = "书名",
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
            )
            if (isBookNameExists) {
                Text(
                    text = "书名已存在，导入将覆盖原书",
                    color = MiuixTheme.colorScheme.error,
                    style = MiuixTheme.textStyles.footnote1,
                    modifier = Modifier.padding(start = 24.dp, top = 4.dp)
                )
            }
        }

        if (!isBookNameExists || isMultipleFiles) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.defaultColors(
                    color = MiuixTheme.colorScheme.secondaryVariant
                )
            ) {
                SuperArrow(
                    title = "分类",
                    summary = selectedCategory ?: "未分类",
                    onClick = onShowCategorySelector
                )
            }
            if (!hasPdf) {
                Spacer(modifier = Modifier.height(12.dp))
                SmallTitle(text = "章节设置")
                Card(
                    colors = CardDefaults.defaultColors(
                        color = MiuixTheme.colorScheme.secondaryVariant
                    )
                ) {
                    SuperSwitch(
                        title = "不分章",
                        summary = "整本书作为单个章节导入",
                        checked = noSplit,
                        onCheckedChange = { noSplit = it }
                    )
                }

                AnimatedVisibility(visible = !noSplit) {
                    Column {
                        if (hasTxt) {
                            SmallTitle(text = "分章方式")
                            val methodsList = ChapterSplitter.methods.toList()
                            val selectedIndex =
                                methodsList.indexOfFirst { it.first == splitMethod }
                                    .coerceAtLeast(0)

                            Card(
                                colors = CardDefaults.defaultColors(
                                    color = MiuixTheme.colorScheme.secondaryVariant
                                )
                            ) {
                                SuperDropdown(
                                    title = "选择分章方式",
                                    items = methodsList.map { it.second },
                                    selectedIndex = selectedIndex,
                                    onSelectedIndexChange = { splitMethod = methodsList[it].first }
                                )

                                if (splitMethod == ChapterSplitter.METHOD_BY_WORD_COUNT) {
                                    TextField(
                                        value = wordsPerChapterText,
                                        onValueChange = {
                                            if (it.all { c -> c.isDigit() }) wordsPerChapterText =
                                                it
                                        },
                                        label = "每章字数",
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                                    )
                                }

                                if (splitMethod == ChapterSplitter.METHOD_CUSTOM) {
                                    LaunchedEffect(splitMethod) {
                                        if (customRegex.isBlank()) customRegex =
                                            """^(第(\s{0,1}[一二三四五六七八九十百千万零〇\d]+\s{0,1})(章|卷|节|部|篇|回|本)|番外\s{0,2}[一二三四五六七八九十百千万零〇\d]*)(.{0,30})$"""
                                    }
                                    TextField(
                                        value = customRegex,
                                        onValueChange = {
                                            customRegex = it
                                            val matchedTemplate =
                                                customRegexTemplates.firstOrNull { tpl -> tpl.regex == it }
                                            selectedCustomRegexTemplateName = matchedTemplate?.name
                                        },
                                        label = "正则表达式",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        TextButton(
                                            text = selectedCustomRegexTemplateName?.let { "模板：$it" }
                                                ?: "选择模板",
                                            onClick = { showRegexTemplateDialog.value = true },
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        TextButton(
                                            text = "保存为模板",
                                            onClick = {
                                                if (customRegex.isBlank()) return@TextButton
                                                quickSaveTemplateName =
                                                    selectedCustomRegexTemplateName.orEmpty()
                                                showQuickSaveTemplateDialog.value = true
                                            },
                                            enabled = customRegex.isNotBlank(),
                                            colors = ButtonDefaults.textButtonColorsPrimary(),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (hasEpubOrNvb && !noSplit) {
                Spacer(modifier = Modifier.height(12.dp))
                SmallTitle(text = "高级处理")
                Card(
                    colors = CardDefaults.defaultColors(
                        color = MiuixTheme.colorScheme.secondaryVariant
                    )
                ) {
                    Column {
                        SuperSwitch(
                            title = "合并短章节",
                            summary = "将字数过少的章节合并",
                            checked = enableChapterMerge,
                            onCheckedChange = { enableChapterMerge = it }
                        )
                        AnimatedVisibility(visible = enableChapterMerge) {
                            Column {
                                TextField(
                                    value = mergeMinWordsText,
                                    onValueChange = {
                                        if (it.all { c -> c.isDigit() }) mergeMinWordsText = it
                                    },
                                    label = "最小字数阈值",
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                SuperSwitch(
                                    title = "重命名章节",
                                    summary = "使用正则替换章节标题",
                                    checked = enableChapterRename,
                                    onCheckedChange = { enableChapterRename = it }
                                )
                                AnimatedVisibility(visible = enableChapterRename) {
                                    Column {
                                        SmallTitle(text = "替换规则")
                                        TextField(
                                            value = renamePattern,
                                            onValueChange = { renamePattern = it },
                                            label = "正则 -> 替换",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                                        )
                                        renamePreview?.let { preview ->
                                            Text(
                                                text = when (preview) {
                                                    is RenamePreviewResult.Success -> preview.text
                                                    is RenamePreviewResult.Error -> preview.text
                                                },
                                                style = MiuixTheme.textStyles.footnote1,
                                                color = if (preview is RenamePreviewResult.Error) MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.primary,
                                                modifier = Modifier.padding(
                                                    start = 24.dp,
                                                    top = 4.dp,
                                                    bottom = 12.dp
                                                )
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
        Spacer(modifier = Modifier.height(32.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                text = "取消",
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .pressable(interactionSource = null, indication = SinkFeedback())
            )
            Spacer(Modifier.width(16.dp))
            TextButton(
                text = "导入",
                colors = ButtonDefaults.textButtonColorsPrimary(),
                enabled = isMultipleFiles || bookName.isNotBlank(),
                onClick = {
                    val wordsPerChapter =
                        wordsPerChapterText.toIntOrNull()?.coerceIn(100, 50000) ?: 5000
                    val mergeMinWords =
                        mergeMinWordsText.toIntOrNull()?.coerceIn(100, 10000) ?: 500
                    onConfirm(
                        bookName,
                        splitMethod,
                        noSplit,
                        wordsPerChapter,
                        selectedCategory,
                        enableChapterMerge,
                        mergeMinWords,
                        enableChapterRename,
                        renamePattern,
                        customRegex,
                        selectedCustomRegexTemplateName,
                        customRegexTemplates
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .pressable(interactionSource = null, indication = SinkFeedback())
            )
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }

    RegexTemplateDialog(
        show = showRegexTemplateDialog,
        templates = customRegexTemplates,
        selectedTemplateName = selectedCustomRegexTemplateName,
        currentRegex = customRegex,
        onTemplateSelected = { template ->
            selectedCustomRegexTemplateName = template?.name
            if (template != null) {
                customRegex = template.regex
            }
        },
        onTemplateDeleted = { name ->
            customRegexTemplates = customRegexTemplates.filterNot { it.name == name }
            if (selectedCustomRegexTemplateName == name) {
                selectedCustomRegexTemplateName = null
            }
        },
        onTemplateCreated = { name, regex ->
            val exists = customRegexTemplates.any { it.name == name }
            customRegexTemplates = if (exists) {
                customRegexTemplates.map {
                    if (it.name == name) it.copy(regex = regex) else it
                }
            } else {
                customRegexTemplates + RegexTemplate(name = name, regex = regex)
            }
            selectedCustomRegexTemplateName = name
            customRegex = regex
        }
    )

    SuperDialog(
        title = "保存正则模板",
        show = showQuickSaveTemplateDialog,
        onDismissRequest = {
            showQuickSaveTemplateDialog.value = false
            quickSaveTemplateName = ""
        }
    ) {
        val targetName = quickSaveTemplateName.trim()
        val isDuplicateName =
            targetName.isNotBlank() && customRegexTemplates.any { it.name == targetName }

        TextField(
            value = quickSaveTemplateName,
            onValueChange = { quickSaveTemplateName = it },
            label = "模板名称",
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        if (isDuplicateName) {
            Text(
                text = "已存在同名模板，保存后将覆盖原模板",
                color = MiuixTheme.colorScheme.error,
                style = MiuixTheme.textStyles.footnote1,
                modifier = Modifier.padding(start = 24.dp, bottom = 12.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(12.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                text = "取消",
                onClick = {
                    showQuickSaveTemplateDialog.value = false
                    quickSaveTemplateName = ""
                },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            TextButton(
                text = if (isDuplicateName) "覆盖保存" else "保存",
                onClick = {
                    val name = quickSaveTemplateName.trim()
                    if (name.isNotBlank() && customRegex.isNotBlank()) {
                        val exists = customRegexTemplates.any { it.name == name }
                        customRegexTemplates = if (exists) {
                            customRegexTemplates.map {
                                if (it.name == name) it.copy(regex = customRegex) else it
                            }
                        } else {
                            customRegexTemplates + RegexTemplate(name = name, regex = customRegex)
                        }
                        selectedCustomRegexTemplateName = name
                        showQuickSaveTemplateDialog.value = false
                        quickSaveTemplateName = ""
                    }
                },
                enabled = quickSaveTemplateName.trim().isNotBlank() && customRegex.isNotBlank(),
                colors = ButtonDefaults.textButtonColorsPrimary(),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RegexTemplateDialog(
    show: MutableState<Boolean>,
    templates: List<RegexTemplate>,
    selectedTemplateName: String?,
    currentRegex: String,
    onTemplateSelected: (RegexTemplate?) -> Unit,
    onTemplateDeleted: (String) -> Unit,
    onTemplateCreated: (name: String, regex: String) -> Unit
) {
    var localSelectedTemplateName by remember(selectedTemplateName) {
        mutableStateOf(selectedTemplateName)
    }
    val showCreateDialog = remember { mutableStateOf(false) }
    var newTemplateName by remember { mutableStateOf("") }

    SuperDialog(
        title = "选择正则模板",
        show = show,
        onDismissRequest = { show.value = false }
    ) {
        Card(
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .height(300.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                SuperCheckbox(
                    title = "不使用模板",
                    checked = localSelectedTemplateName.isNullOrBlank(),
                    onCheckedChange = {
                        if (it) localSelectedTemplateName = null
                    },
                    checkboxLocation = CheckboxLocation.Start
                )

                templates.forEach { template ->
                    SuperCheckbox(
                        title = template.name,
                        summary = template.regex,
                        checked = localSelectedTemplateName == template.name,
                        onCheckedChange = { if (it) localSelectedTemplateName = template.name },
                        checkboxLocation = CheckboxLocation.Start,
                        endActions = {
                            IconButton(onClick = { onTemplateDeleted(template.name) }) {
                                Icon(
                                    imageVector = MiuixIcons.Delete,
                                    contentDescription = "删除模板",
                                    tint = MiuixTheme.colorScheme.error
                                )
                            }
                        }
                    )
                }

                BasicComponent(
                    title = "新建模板",
                    titleColor = BasicComponentDefaults.titleColor(
                        color = MiuixTheme.colorScheme.primary
                    ),
                    startAction = {
                        Icon(
                            imageVector = MiuixIcons.Add,
                            contentDescription = "新建模板",
                            tint = MiuixTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    },
                    onClick = { showCreateDialog.value = true }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                text = "取消",
                onClick = { show.value = false },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(16.dp))
            TextButton(
                text = "确定",
                colors = ButtonDefaults.textButtonColorsPrimary(),
                onClick = {
                    val template = templates.firstOrNull { it.name == localSelectedTemplateName }
                    onTemplateSelected(template)
                    show.value = false
                },
                modifier = Modifier.weight(1f)
            )
        }
    }

    SuperDialog(
        title = "新建正则模板",
        show = showCreateDialog,
        onDismissRequest = {
            showCreateDialog.value = false
            newTemplateName = ""
        }
    ) {
        TextField(
            value = newTemplateName,
            onValueChange = { newTemplateName = it },
            label = "模板名称",
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                text = "取消",
                onClick = {
                    showCreateDialog.value = false
                    newTemplateName = ""
                },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            TextButton(
                text = "创建",
                onClick = {
                    val name = newTemplateName.trim()
                    if (name.isNotBlank() && currentRegex.isNotBlank()) {
                        onTemplateCreated(name, currentRegex)
                        localSelectedTemplateName = name
                        newTemplateName = ""
                        showCreateDialog.value = false
                    }
                },
                enabled = newTemplateName.trim().isNotBlank() && currentRegex.isNotBlank(),
                colors = ButtonDefaults.textButtonColorsPrimary(),
                modifier = Modifier.weight(1f)
            )
        }
    }
}