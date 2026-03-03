package com.bandbbs.ebook.utils

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object BandAppExporter {

    enum class BandModel {
        REDMI_WATCH_5_6,
        MI_BAND_8PRO_9PRO,
        MI_BAND_9,
        MI_BAND_10
    }

    fun getAssetPath(model: BandModel): String {
        return when (model) {
            BandModel.REDMI_WATCH_5_6 -> "RW/rw.rpk"
            BandModel.MI_BAND_8PRO_9PRO -> "BandPro/pro.rpk"
            BandModel.MI_BAND_9 -> "Band9/9.rpk"
            BandModel.MI_BAND_10 -> "Band10/10.rpk"
        }
    }

    fun getFileName(model: BandModel): String {
        return when (model) {
            BandModel.REDMI_WATCH_5_6 -> "rw.rpk"
            BandModel.MI_BAND_8PRO_9PRO -> "pro.rpk"
            BandModel.MI_BAND_9 -> "9.rpk"
            BandModel.MI_BAND_10 -> "10.rpk"
        }
    }

    suspend fun exportBandPackage(
        context: Context,
        model: BandModel,
        uri: Uri
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val assetPath = getAssetPath(model)
                context.assets.open(assetPath).use { input ->
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                        }
                        output.flush()
                    } ?: return@withContext Result.failure(IllegalStateException("无法打开目标文件"))
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

