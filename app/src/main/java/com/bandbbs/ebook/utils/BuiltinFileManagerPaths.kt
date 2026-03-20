package com.bandbbs.ebook.utils

import android.content.Context
import android.os.Environment
import java.io.File

data class ImportPathPreset(
    val id: String,
    val label: String,
    val resolver: (Context) -> File?
)

object BuiltinFileManagerPaths {
    val defaultPresets: List<ImportPathPreset> = listOf(
        ImportPathPreset(
            id = "qq_download",
            label = "QQ 下载目录",
            resolver = {
                val root = Environment.getExternalStorageDirectory()
                File(root, "Download/QQ")
            }
        ),
        ImportPathPreset(
            id = "quark_cloud_drive_download_1",
            label = "夸克网盘下载目录",
            resolver = {
                val root = Environment.getExternalStorageDirectory()
                File(root, "Download/QuarkCloudDrive")
            }
        ),
        ImportPathPreset(
            id = "quark_cloud_drive_download_2",
            label = "夸克网盘下载目录",
            resolver = {
                val root = Environment.getExternalStorageDirectory()
                File(root, "Download/QuarkDownloads/CloudDrive")
            }
        ),
        ImportPathPreset(
            id = "quark_cloud_drive_download_2",
            label = "百度网盘下载目录",
            resolver = {
                val root = Environment.getExternalStorageDirectory()
                File(root, "Download/BaiduNetdisk")
            }
        ),
        ImportPathPreset(
            id = "download",
            label = "下载",
            resolver = { Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) }
        ),
        ImportPathPreset(
            id = "documents",
            label = "文档",
            resolver = { Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) }
        ),
        ImportPathPreset(
            id = "storage_root",
            label = "内部存储",
            resolver = { Environment.getExternalStorageDirectory() }
        )
    )

    fun resolvedDefaults(context: Context): List<Pair<ImportPathPreset, File>> {
        return defaultPresets.mapNotNull { preset ->
            val file = preset.resolver(context)
            if (file != null && file.exists() && file.canRead()) preset to file else null
        }
    }
}
