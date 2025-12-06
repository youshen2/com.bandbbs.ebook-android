package com.bandbbs.ebook.utils

import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

object VersionChecker {
    private const val TAG = "VersionChecker"
    private const val API_URL = "https://api.ikortex.top/v1/sinebook_vela/version"
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Serializable
    data class VersionResponse(
        val device: List<DeviceVersion>
    )

    @Serializable
    data class DeviceVersion(
        val type: String,
        val device_name: String = "",
        val version_code: Int,
        val version_name: String,
        val update_log: List<String> = emptyList()
    )

    data class UpdateInfo(
        val hasUpdate: Boolean,
        val versionCode: Int,
        val versionName: String,
        val updateLog: List<String>,
        val deviceType: String
    )

    private suspend fun fetchVersionResponse(): Result<VersionResponse> {
        return try {
            val url = URL(API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("User-Agent", "Sine-Android")

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return Result.failure(Exception("HTTP错误: $responseCode"))
            }

            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val versionResponse = json.decodeFromString<VersionResponse>(responseBody)
            Result.success(versionResponse)
        } catch (e: Exception) {
            Log.e(TAG, "获取版本信息失败", e)
            return Result.failure(e)
        }
    }

    suspend fun checkUpdate(
        currentVersionCode: Int
    ): Result<UpdateInfo> {
        return try {
            val versionResponseResult = fetchVersionResponse()
            val versionResponse = versionResponseResult.getOrElse {
                return Result.failure(it)
            }

            val androidUpdate = versionResponse.device.find { it.type == "android" }
            val androidUpdateInfo = androidUpdate?.let {
                if (it.version_code > currentVersionCode) {
                    UpdateInfo(
                        hasUpdate = true,
                        versionCode = it.version_code,
                        versionName = it.version_name,
                        updateLog = it.update_log,
                        deviceType = "android"
                    )
                } else {
                    UpdateInfo(
                        hasUpdate = false,
                        versionCode = it.version_code,
                        versionName = it.version_name,
                        updateLog = it.update_log,
                        deviceType = "android"
                    )
                }
            }


            if (androidUpdateInfo != null) {
                Result.success(androidUpdateInfo)
            } else {
                Result.success(
                    UpdateInfo(
                        hasUpdate = false,
                        versionCode = currentVersionCode,
                        versionName = "",
                        updateLog = emptyList(),
                        deviceType = "android"
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "检查更新失败", e)
            Result.failure(e)
        }
    }

    suspend fun checkBandUpdate(
        currentDeviceName: String,
        currentVersionCode: Int? = null
    ): Result<UpdateInfo> {
        return try {
            val versionResponseResult = fetchVersionResponse()
            val versionResponse = versionResponseResult.getOrElse {
                return Result.failure(it)
            }

            val bandUpdate = versionResponse.device.find { device ->
                device.type != "android" && device.device_name.isNotEmpty() &&
                        currentDeviceName.matches(Regex(device.device_name))
            }

            if (bandUpdate != null) {
                val hasUpdate = currentVersionCode != null && bandUpdate.version_code > currentVersionCode
                Result.success(
                    UpdateInfo(
                        hasUpdate = hasUpdate,
                        versionCode = bandUpdate.version_code,
                        versionName = bandUpdate.version_name,
                        updateLog = bandUpdate.update_log,
                        deviceType = "band"
                    )
                )
            } else {
                Result.success(
                    UpdateInfo(
                        hasUpdate = false,
                        versionCode = currentVersionCode ?: 0,
                        versionName = "",
                        updateLog = emptyList(),
                        deviceType = "band"
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "检查手环更新失败", e)
            Result.failure(e)
        }
    }
}
