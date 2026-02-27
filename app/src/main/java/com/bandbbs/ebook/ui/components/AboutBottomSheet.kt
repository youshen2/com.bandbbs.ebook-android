package com.bandbbs.ebook.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.extra.SuperBottomSheet
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Community
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AboutBottomSheet(
    show: MutableState<Boolean>,
    onCheckUpdate: () -> Unit = {}
) {
    val context = LocalContext.current

    val versionName = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "未知"
        } catch (e: PackageManager.NameNotFoundException) {
            "未知"
        }
    }

    SuperBottomSheet(
        show = show,
        title = "关于",
        onDismissRequest = { show.value = false },
        backgroundColor = MiuixTheme.colorScheme.secondaryVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "弦电子书",
                    style = MiuixTheme.textStyles.title2,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Android 手机端",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantActions
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = MiuixTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        text = "Version $versionName",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            SmallTitle(text = "开发信息")
            Card(
                insideMargin = PaddingValues(0.dp)
            ) {
                BasicComponent(
                    title = "开发者",
                    summary = "爅峫"
                )
                BasicComponent(
                    title = "《喵喵电子书》开发人员",
                    summary = "NEORUAA, 乐色桶, 无源流沙"
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            SmallTitle(text = "相关链接")
            Card(
                modifier = Modifier.padding(bottom = 40.dp),
                insideMargin = PaddingValues(0.dp)
            ) {
                SuperArrow(
                    title = "QQ交流群",
                    summary = "1067415278",
                    startAction = {
                        Icon(
                            imageVector = MiuixIcons.Community,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    },
                    onClick = {
                        val clipboard =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("QQ Group", "1067415278")
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "已复制群号", Toast.LENGTH_SHORT).show()
                    }
                )
                SuperArrow(
                    title = "官网",
                    summary = "vs.lucky-e.top",
                    startAction = {
                        Icon(
                            imageVector = MiuixIcons.Link,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    },
                    onClick = {
                        try {
                            val intent =
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://vs.lucky-e.top"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "无法打开浏览器", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                SuperArrow(
                    title = "更多资源请访问",
                    summary = "bandbbs.cn",
                    startAction = {
                        Icon(
                            imageVector = MiuixIcons.Link,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    },
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://bandbbs.cn"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "无法打开浏览器", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}
