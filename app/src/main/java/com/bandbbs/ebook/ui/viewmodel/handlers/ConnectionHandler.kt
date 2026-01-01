package com.bandbbs.ebook.ui.viewmodel.handlers

import android.util.Log
import com.bandbbs.ebook.logic.InterHandshake
import com.bandbbs.ebook.logic.InterconnetFile
import com.bandbbs.ebook.ui.viewmodel.ConnectionErrorState
import com.bandbbs.ebook.ui.viewmodel.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class ConnectionHandler(
    private val scope: CoroutineScope,
    private val connectionState: MutableStateFlow<ConnectionState>,
    private val connectionErrorState: MutableStateFlow<ConnectionErrorState?>,
    private val showConnectionError: StateFlow<Boolean>,
    private val versionIncompatibleState: MutableStateFlow<com.bandbbs.ebook.ui.viewmodel.VersionIncompatibleState?>,
) {

    private var interHandshake: InterHandshake? = null
    private var fileConnection: InterconnetFile? = null
    var onBandConnected: ((String) -> Unit)? = null
    var onBandVersionReceived: ((Int) -> Unit)? = null

    fun setConnection(connection: InterHandshake) {
        interHandshake = connection
        fileConnection = InterconnetFile(connection)
        connection.setOnVersionIncompatible { currentVersion, requiredVersion ->
            versionIncompatibleState.value =
                com.bandbbs.ebook.ui.viewmodel.VersionIncompatibleState(
                    currentVersion = currentVersion,
                    requiredVersion = requiredVersion
                )
        }
        connection.setOnBandVersionReceived { version ->
            onBandVersionReceived?.invoke(version)
        }
        reconnect()
    }

    fun dismissConnectionError() {
        connectionErrorState.value = null
    }

    fun getHandshake(): InterHandshake {
        return interHandshake ?: throw IllegalStateException("连接未初始化")
    }

    fun getFileConnection(): InterconnetFile {
        return fileConnection ?: throw IllegalStateException("文件连接未初始化")
    }

    fun isConnected(): Boolean {
        return connectionState.value.isConnected
    }

    fun getDeviceName(): String? {
        return connectionState.value.deviceName
    }

    fun getConnectedBandVersion(): Int? {
        return interHandshake?.connectedBandVersion
    }

    fun reconnect() {
        val connection = interHandshake ?: return
        scope.launch {
            connectionState.update {
                it.copy(
                    statusText = "手环连接中",
                    descriptionText = "请确保小米运动健康后台运行",
                    isConnected = false,
                    deviceName = null
                )
            }
            try {
                withTimeout(3000L) {
                    connection.destroy().await()
                    val deviceName = connection.connect().await().replace(" ", "")

                    val unsupportedDevices = listOf("小米手环8")
                    val isUnsupported = unsupportedDevices.any { deviceName == it }

                    if (isUnsupported) {
                        connectionState.update {
                            it.copy(
                                statusText = "设备不受支持",
                                descriptionText = "$deviceName 不受支持",
                                isConnected = false,
                                deviceName = deviceName
                            )
                        }
                        delay(300)
                        if (showConnectionError.value) {
                            connectionErrorState.value = ConnectionErrorState(
                                deviceName = deviceName,
                                isUnsupportedDevice = true
                            )
                        }
                        return@withTimeout
                    }

                    connection.auth().await()
                    try {
                        if (!connection.getAppState().await()) {
                            connectionState.update {
                                it.copy(
                                    statusText = "弦电子书未安装",
                                    descriptionText = "请在手环上安装小程序",
                                    isConnected = false,
                                    deviceName = deviceName
                                )
                            }
                            delay(300)
                            if (showConnectionError.value) {
                                connectionErrorState.value = ConnectionErrorState(
                                    deviceName = deviceName,
                                    isUnsupportedDevice = false
                                )
                            }
                            return@withTimeout
                        }
                    } catch (_: Exception) {
                        connectionState.update {
                            it.copy(
                                statusText = "弦电子书未安装",
                                descriptionText = "请在手环上安装小程序",
                                isConnected = false,
                                deviceName = deviceName
                            )
                        }
                        delay(300)
                        connectionErrorState.value = ConnectionErrorState(
                            deviceName = deviceName,
                            isUnsupportedDevice = false
                        )
                        return@withTimeout
                    }
                    connection.openApp().await()
                    connection.registerListener().await()
                    connectionState.update {
                        it.copy(
                            statusText = "设备连接成功",
                            descriptionText = "$deviceName 已连接",
                            isConnected = true,
                            deviceName = deviceName
                        )
                    }

                    onBandConnected?.invoke(deviceName)
                }
            } catch (_: TimeoutCancellationException) {
                Log.e("MainViewModel", "connect timeout")
                connectionState.update {
                    it.copy(
                        statusText = "手环连接失败",
                        descriptionText = "连接超时",
                        isConnected = false,
                        deviceName = null
                    )
                }
                delay(300)
                if (showConnectionError.value) {
                    connectionErrorState.value = ConnectionErrorState(
                        deviceName = null,
                        isUnsupportedDevice = false
                    )
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "connect fail ${e.message}")
                connectionState.update {
                    it.copy(
                        statusText = "手环连接失败",
                        descriptionText = e.message ?: "未知错误",
                        isConnected = false,
                        deviceName = null
                    )
                }
                delay(300)
                if (showConnectionError.value) {
                    connectionErrorState.value = ConnectionErrorState(
                        deviceName = null,
                        isUnsupportedDevice = false
                    )
                }
            }
        }
    }
}
