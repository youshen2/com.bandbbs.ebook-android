package com.bandbbs.ebook.utils

import android.content.Context
import android.provider.Settings
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

object VersionChecker {
    private const val TAG = "VersionChecker"
    private const val API_CHECK_UPDATE = "https://api.luoxe.cn/sinebook/check-update"
    private const val API_NOTICES = "https://api.luoxe.cn/sinebook/notices"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Serializable
    data class CheckUpdateReq(val model: String, val versionCode: Int, val aid: String = "")

    @Serializable
    data class UpdateConfig(
        val model: String = "",
        val versionCode: Int = 0,
        val versionName: String = "",
        val status: String = "",
        val remark: String = "",
        val normalDownloadUrl: String = "",
        val normalBackupDownloadUrl: String = "",
        val iconlessDownloadUrl: String = "",
        val iconlessBackupDownloadUrl: String = "",
        val forumUrl: String = "",
        val regexps: List<String> = emptyList(),
        val updateLogs: List<String> = emptyList()
    )

    @Serializable
    data class CheckUpdateResp(
        val hasUpdate: Boolean = false,
        val update: UpdateConfig? = null
    )

    @Serializable
    data class Notice(
        val id: Int = 0,
        val title: String = "",
        val summary: String = "",
        val content: String = "",
        val time: String = "",
        val link: String = "",
        val created_at: Long = 0,
        val updated_at: Long = 0
    )

    data class UpdateInfo(
        val hasUpdate: Boolean,
        val versionCode: Int,
        val versionName: String,
        val updateLog: List<String>,
        val deviceType: String,
        val downloadUrl: String = ""
    )

    suspend fun checkUpdate(context: Context, currentVersionCode: Int): Result<UpdateInfo> {
        val aid = getAndroidId(context)
        return checkDeviceUpdate("Android", "android", currentVersionCode, aid)
    }

    suspend fun checkBandUpdate(
        context: Context,
        currentDeviceName: String,
        currentVersionCode: Int? = null
    ): Result<UpdateInfo> {
        val aid = getAndroidId(context)
        return checkDeviceUpdate(currentDeviceName, "band", currentVersionCode ?: 0, aid)
    }

    private suspend fun checkDeviceUpdate(
        model: String,
        deviceType: String,
        currentVersionCode: Int,
        aid: String
    ): Result<UpdateInfo> {
        return try {
            val url = URL(API_CHECK_UPDATE)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("User-Agent", "Sine-Android")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.doOutput = true

            connection.outputStream.use { os ->
                val req = json.encodeToString(CheckUpdateReq(model, currentVersionCode, aid))
                os.write(req.toByteArray())
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return Result.failure(Exception("HTTP错误: $responseCode"))
            }

            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val resp = json.decodeFromString<CheckUpdateResp>(responseBody)
            Log.e("resp", responseBody)

            if (resp.hasUpdate && resp.update != null) {
                val updateConfig = resp.update
                Result.success(
                    UpdateInfo(
                        hasUpdate = true,
                        versionCode = updateConfig.versionCode.takeIf { it > 0 }
                            ?: (currentVersionCode + 1),
                        versionName = updateConfig.versionName.ifEmpty { updateConfig.remark }
                            .ifEmpty { "新版本" },
                        updateLog = updateConfig.updateLogs,
                        deviceType = deviceType,
                        downloadUrl = updateConfig.normalDownloadUrl.ifEmpty { updateConfig.forumUrl }
                    )
                )
            } else {
                Result.success(
                    UpdateInfo(
                        hasUpdate = false,
                        versionCode = currentVersionCode,
                        versionName = "",
                        updateLog = emptyList(),
                        deviceType = deviceType,
                        downloadUrl = ""
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "检查更新失败", e)
            Result.failure(e)
        }
    }

    suspend fun getNotices(): Result<List<Notice>> {
        return try {
            val url = URL(API_NOTICES)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Sine-Android")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return Result.failure(Exception("HTTP错误: $responseCode"))
            }

            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val notices = json.decodeFromString<List<Notice>>(responseBody)
            Result.success(notices)
        } catch (e: Exception) {
            Log.e(TAG, "获取公告失败", e)
            Result.failure(e)
        }
    }

    private fun getAndroidId(context: Context): String {
        return runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                .orEmpty()
        }.getOrDefault("")
    }
}
