package com.bandbbs.ebook.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bandbbs.ebook.utils.BandAppExporter
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.extra.CheckboxLocation
import top.yukonga.miuix.kmp.extra.SuperCheckbox
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun BandPackageExportDialog(
    show: MutableState<Boolean>,
    onModelSelected: (BandAppExporter.BandModel) -> Unit
) {
    val options = listOf(
        "REDMI Watch 5 / 6" to BandAppExporter.BandModel.REDMI_WATCH_5_6,
        "小米手环 8 Pro / 9 Pro" to BandAppExporter.BandModel.MI_BAND_8PRO_9PRO,
        "小米手环 9 / 9 NFC" to BandAppExporter.BandModel.MI_BAND_9,
        "小米手环 10 / 10 NFC" to BandAppExporter.BandModel.MI_BAND_10
    )

    val (selectedIndex, setSelectedIndex) = remember { mutableStateOf(0) }

    SuperDialog(
        title = "选择手环型号",
        show = show,
        onDismissRequest = { show.value = false }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "即将保存手环端安装包，请认真选择保存位置，避免保存后找不到。",
                style = MiuixTheme.textStyles.subtitle,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Card (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                options.forEachIndexed { index, (label, _) ->
                    SuperCheckbox(
                        title = label,
                        checked = selectedIndex == index,
                        onCheckedChange = { checked ->
                            if (checked) setSelectedIndex(index)
                        },
                        checkboxLocation = CheckboxLocation.Start
                    )
                }
            }

            Text(
                text = "仅对从 vs.lucky-e.top 获取的安装包负责，其他渠道可能存在恶意修改版，请注意分辨。",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            RowButtons(
                onCancel = { show.value = false },
                onConfirm = {
                    val model = options[selectedIndex].second
                    show.value = false
                    onModelSelected(model)
                }
            )
        }
    }
}

@Composable
private fun RowButtons(
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(
            text = "取消",
            onClick = onCancel,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(16.dp))
        TextButton(
            text = "确定",
            colors = ButtonDefaults.textButtonColorsPrimary(),
            onClick = onConfirm,
            modifier = Modifier.weight(1f)
        )
    }
}

