package com.bandbbs.ebook.logic

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.xiaomi.xms.wearable.message.OnMessageReceivedListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class InterHandshake(context: Context, val scope: CoroutineScope) : Interconn(context) {
    companion object {
        private const val TYPE = "__hs__"
        private const val TIMEOUT = 10000L
        private const val PHONE_VERSION_CODE = 126200
        private const val MIN_BAND_VERSION_CODE = 260228
    }

    private var promise: CompletableDeferred<Unit>? = null
    private var connected = false
    private var isHandshaking = false
    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null

    var connectedBandVersion: Int? = null
        private set

    override val onMessageListener = OnMessageReceivedListener { _, message ->
        resetTimeout()
        val messageStr = message.decodeToString()
        try {
            val msg = json.decodeFromString<Message>(messageStr)
            onMessage[msg.tag]?.invoke(messageStr)
        } catch (e: Exception) {
            Log.e("Handshake", "Error parsing message: $messageStr", e)
        }
    }

    private fun resetTimeout() {
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        timeoutRunnable = Runnable {
            Log.w("Handshake", "Connection timeout, resetting state")
            cleanup()
            onDisconnected.invoke()
        }
        handler.postDelayed(timeoutRunnable!!, TIMEOUT)
    }

    private fun cleanup() {
        promise?.cancel()
        promise = null
        connected = false
        isHandshaking = false
        timeoutRunnable?.let { handler.removeCallbacks(it) }
    }

    init {
        addListener(TYPE) { payload ->
            try {
                val data: HandshakePayload = json.decodeFromString(payload)
                val currentCount = data.count
                val bandVersion = data.version

                connectedBandVersion = bandVersion

                if (bandVersion != null) {
                    onBandVersionReceived.invoke(bandVersion)
                    if (bandVersion < MIN_BAND_VERSION_CODE) {
                        onVersionIncompatible.invoke(bandVersion, MIN_BAND_VERSION_CODE)
                    }
                }

                if (currentCount > 0) {
                    if (promise != null && !connected) {
                        connected = true
                        isHandshaking = false
                        promise?.complete(Unit)
                        onConnected.invoke()
                    }
                }

                if (currentCount < 3) {
                    scope.launch {
                        try {
                            super.sendMessage("{\"tag\":\"$TYPE\",\"count\":${currentCount + 1},\"version\":$PHONE_VERSION_CODE}")
                                .await()
                        } catch (e: Exception) {
                            Log.e("Handshake", "Failed to send handshake reply", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("Handshake", "Error handling handshake payload", e)
            }
        }
    }

    override fun sendMessage(message: String): CompletableDeferred<Unit> {
        val result = CompletableDeferred<Unit>()
        scope.launch {
            try {
                if (!connected) {
                    if (isHandshaking) {
                        promise?.await()
                    } else {
                        isHandshaking = true
                        val handshakePromise = CompletableDeferred<Unit>()
                        promise = handshakePromise

                        val timeoutCb = Runnable {
                            if (!connected) {
                                isHandshaking = false
                                promise = null
                                handshakePromise.completeExceptionally(Exception("握手超时，请检查设备连接状态"))
                            }
                        }
                        handler.postDelayed(timeoutCb, TIMEOUT)

                        try {
                            super.sendMessage("{\"tag\":\"$TYPE\",\"count\":0,\"version\":$PHONE_VERSION_CODE}").await()
                            handshakePromise.await()
                            handler.removeCallbacks(timeoutCb)
                        } catch (e: Exception) {
                            isHandshaking = false
                            promise = null
                            handler.removeCallbacks(timeoutCb)
                            result.completeExceptionally(e)
                            return@launch
                        }
                    }
                }

                resetTimeout()
                val sendResult = super.sendMessage(message).await()
                result.complete(sendResult)
            } catch (e: Exception) {
                result.completeExceptionally(e)
            }
        }
        return result
    }

    @Serializable
    private data class HandshakePayload(val count: Int, val tag: String, val version: Int? = null)

    private var onConnected = {}
    fun setOnConnected(callback: () -> Unit) {
        onConnected = callback
    }

    private var onDisconnected = {}
    fun setOnDisconnected(callback: () -> Unit) {
        onDisconnected = callback
    }

    private var onVersionIncompatible: (currentVersion: Int, requiredVersion: Int) -> Unit = { _, _ -> }
    fun setOnVersionIncompatible(callback: (currentVersion: Int, requiredVersion: Int) -> Unit) {
        onVersionIncompatible = callback
    }

    private var onBandVersionReceived: (version: Int) -> Unit = { }
    fun setOnBandVersionReceived(callback: (version: Int) -> Unit) {
        onBandVersionReceived = callback
    }

    override suspend fun init() {
        if (!connected && !isHandshaking) {
            scope.launch {
                try {
                    sendMessage("{\"tag\":\"$TYPE\",\"ping\":true}").await()
                } catch (e: Exception) {
                    Log.e("Handshake", "Manual init failed", e)
                }
            }
        }
    }
}
