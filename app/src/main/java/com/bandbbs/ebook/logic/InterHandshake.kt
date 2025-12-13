package com.bandbbs.ebook.logic

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.xiaomi.xms.wearable.message.OnMessageReceivedListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

//握握手，握握双手

class InterHandshake(context: Context, val scope: CoroutineScope) : Interconn(context) {
    companion object {
        private const val TYPE = "__hs__"
        private const val TIMEOUT = 10000L
        private const val PHONE_VERSION_CODE = 40610
        private const val MIN_BAND_VERSION_CODE = 251213
    }

    private var promise: Deferred<Unit>? = null
    private var connected = false
    private var resolve: (() -> Unit)? = null
    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null

    var connectedBandVersion: Int? = null
        private set

    override val onMessageListener = OnMessageReceivedListener { _, message ->
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        timeoutRunnable = Runnable {
            promise = null
            resolve = null
            connected = false
            onDisconnected.invoke()
        }
        handler.postDelayed(timeoutRunnable!!, TIMEOUT)
        val messageStr = message.decodeToString()
        Log.d("Interconn <<<", messageStr)
        val msg = json.decodeFromString<Message>(messageStr)
        onMessage[msg.tag]?.invoke(messageStr)
    }

    init {
        addListener(TYPE) { payload ->
            val data: HandshakePayload = json.decodeFromString(payload)
            val currentCount = data.count
            val bandVersion = data.version

            connectedBandVersion = bandVersion

            if (bandVersion != null) {
                onBandVersionReceived.invoke(bandVersion)

                if (bandVersion < MIN_BAND_VERSION_CODE){
                    Log.w("Handshake", "Band version $bandVersion is incompatible, required: $MIN_BAND_VERSION_CODE")
                    onVersionIncompatible.invoke(bandVersion, MIN_BAND_VERSION_CODE)
                    return@addListener
                }
            } else {
                Log.w("Handshake", "Band version is incompatible, required: $MIN_BAND_VERSION_CODE")
                onVersionIncompatible.invoke(0, MIN_BAND_VERSION_CODE)
            }

            if (currentCount > 0) {
                if (promise != null) {
                    resolve?.invoke()
                    resolve = null
                    promise = null
                    onConnected.invoke()
                } else {
                    promise = CompletableDeferred(Unit)
                }
            }

            val newCount = currentCount + 1
            if (newCount < 3) {
                scope.launch {
                    try {
                        super.sendMessage("{\"tag\":\"$TYPE\",\"count\":$newCount,\"version\":$PHONE_VERSION_CODE}")
                            .await()
                    } catch (e: Exception) {
                        Log.e("Handshake", "Failed to send handshake reply", e)
                    }
                }
            }
        }
    }

    override fun sendMessage(message: String): CompletableDeferred<Unit> {
        return CompletableDeferred<Unit>().apply {
            val cpe = { e: Exception ->
                completeExceptionally(e)
            }
            scope.launch {
                if (!connected) {
                    if (promise != null) {
                        try {
                            promise?.await()
                        } catch (e: Exception) {
                            cpe(e)
                            return@launch
                        }
                    } else {
                        val handshakePromise = CompletableDeferred<Unit>()
                        promise = handshakePromise
                        val timeoutCb = Runnable {
                            promise = null
                            resolve = null
                            connected = false
                            handshakePromise.completeExceptionally(Exception("握手超时，可尝试重启手机端"))
                        }
                        handler.postDelayed(timeoutCb, TIMEOUT)
                        resolve = {
                            handshakePromise.complete(Unit)
                            Log.i("handShake", "success")
                            handler.removeCallbacks(timeoutCb)
                            connected = true
                        }
                        try {
                            super.sendMessage("{\"tag\":\"$TYPE\",\"count\":0,\"version\":$PHONE_VERSION_CODE}")
                                .await()
                            handshakePromise.await()
                        } catch (e: Exception) {
                            cpe(e)
                            return@launch
                        }
                    }
                }
                try {
                    complete(super.sendMessage(message).await())
                } catch (e: Exception) {
                    cpe(e)
                }
            }
        }
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
}
