package com.bandbbs.ebook.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.extra.CheckboxLocation
import top.yukonga.miuix.kmp.extra.SuperCheckbox
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun CategoryDialog(
    show: MutableState<Boolean>,
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    onCategoryCreated: (String) -> Unit,
    onCategoryDeleted: (String) -> Unit
) {
    val showCreateDialog = remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    var localSelectedCategory by remember(selectedCategory) { mutableStateOf(selectedCategory) }

    SuperDialog(
        title = "选择分类",
        show = show,
        onDismissRequest = {
            show.value = false
        }
    ) {
        Card(
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = 300.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                SuperCheckbox(
                    title = "未分类",
                    checked = localSelectedCategory.isNullOrBlank(),
                    onCheckedChange = { if (it) localSelectedCategory = null },
                    checkboxLocation = CheckboxLocation.Start
                )

                categories.forEach { category ->
                    SuperCheckbox(
                        title = category,
                        checked = localSelectedCategory == category,
                        onCheckedChange = { if (it) localSelectedCategory = category },
                        checkboxLocation = CheckboxLocation.Start,
                        endActions = {
                            IconButton(
                                onClick = { onCategoryDeleted(category) }
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.Delete,
                                    contentDescription = "删除",
                                    tint = MiuixTheme.colorScheme.error
                                )
                            }
                        }
                    )
                }

                BasicComponent(
                    title = "新建分类",
                    titleColor = BasicComponentDefaults.titleColor(
                        color = MiuixTheme.colorScheme.primary
                    ),
                    startAction = {
                        Icon(
                            imageVector = MiuixIcons.Add,
                            contentDescription = "新建",
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
                    onCategorySelected(localSelectedCategory)
                    show.value = false
                },
                modifier = Modifier.weight(1f)
            )
        }
    }

    SuperDialog(
        title = "新建分类",
        show = showCreateDialog,
        onDismissRequest = {
            showCreateDialog.value = false
            newCategoryName = ""
        }
    ) {
        TextField(
            value = newCategoryName,
            onValueChange = { newCategoryName = it },
            label = "分类名称",
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
                    newCategoryName = ""
                },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            TextButton(
                text = "创建",
                onClick = {
                    if (newCategoryName.isNotBlank()) {
                        onCategoryCreated(newCategoryName.trim())
                        newCategoryName = ""
                        showCreateDialog.value = false
                    }
                },
                enabled = newCategoryName.isNotBlank(),
                colors = ButtonDefaults.textButtonColorsPrimary(),
                modifier = Modifier.weight(1f)
            )
        }
    }
}
