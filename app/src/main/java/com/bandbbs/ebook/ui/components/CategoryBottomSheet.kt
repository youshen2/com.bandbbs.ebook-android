package com.bandbbs.ebook.ui.components

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
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
fun CategoryBottomSheet(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    onCategoryCreated: (String) -> Unit,
    onCategoryDeleted: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val showCreateDialog = remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    var localSelectedCategory by remember(selectedCategory) { mutableStateOf(selectedCategory) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "选择分类",
                style = MiuixTheme.textStyles.title2,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = { showCreateDialog.value = true }
            ) {
                Icon(
                    imageVector = MiuixIcons.Add,
                    contentDescription = "新建",
                    tint = MiuixTheme.colorScheme.onBackground
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                colors = CardDefaults.defaultColors(
                    color = MiuixTheme.colorScheme.secondaryVariant
                )
            ) {
                Column {
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
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                text = "取消",
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(16.dp))
            TextButton(
                text = "确定",
                colors = ButtonDefaults.textButtonColorsPrimary(),
                onClick = {
                    onCategorySelected(localSelectedCategory)
                    onDismiss()
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
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
