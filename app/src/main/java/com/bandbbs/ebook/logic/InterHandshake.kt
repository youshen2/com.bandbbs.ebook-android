package com.bandbbs.ebook.logic

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.xiaomi.xms.wearable.message.OnMessageReceivedListener
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable

//握握手，握握双手

class InterHandshake(context: Context,val scope: CoroutineScope) : Interconn(context) {
    companion object {
        private const val TYPE = "__hs__"
        private const val TIMEOUT = 3000L
    }

    private var promise: Deferred<Unit>? = null
    private var connected = false
    private var resolve: (() -> Unit)? = null
    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null

    override val onMessageListener = OnMessageReceivedListener{ _, message -> // 收到手表端应用发来的消息
        //重置计时器
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        timeoutRunnable = Runnable {
            promise = null
            resolve = null
            connected = false
            onDisconnected.invoke()
        }
        handler.postDelayed(timeoutRunnable!!, TIMEOUT)
        val message = message.decodeToString()
        Log.d("InterconnIn",message)
        val msg = json.decodeFromString<Message>(message)
        onMessage[msg.tag]?.invoke(message)
    }

    init {
        // 注册握手消息监听器
        addListener(TYPE) { payload ->
            // 解析 payload 中的 count（假设 payload 是 JSON 格式）
            val data: HandshakePayload = json.decodeFromString( payload)
            val currentCount = data.count

            // 处理握手计数逻辑
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

            // 递增计数并继续握手（最多 2 次）
            val newCount = currentCount + 1
            if (newCount < 3) {
                super.sendMessage("{\"tag\":\"$TYPE\",\"count\":$newCount}")
            }
        }
    }
    override fun sendMessage(message: String): CompletableDeferred<Unit> {
        return CompletableDeferred<Unit>().apply {
            val cpe= {e: Exception->
                completeExceptionally(e)
            }
            scope.launch {
                if(!connected)
                if (promise != null) {
                    promise?.await()
                } else {
                    // 初始化握手流程
                    promise = CompletableDeferred<Unit>().apply {
                        val timeoutCb = Runnable {
                            promise = null
                            resolve = null
                            connected = false
                            cpe(Exception("握手超时"))
                        }
                        handler.postDelayed(timeoutCb, TIMEOUT)
                        resolve = {
                            complete(Unit)
                            Log.i("handShake","success")
                            handler.removeCallbacks(timeoutCb)
                            connected = true
                        }
                        // 发送初始握手消息（count=0）
                        super.sendMessage("{\"tag\":\"$TYPE\",\"count\":0}").await()
                    }
                    promise?.await()
                }
                complete(super.sendMessage(message).await())
            }
        }
        // 等待握手完成

    }

    @Serializable
    private data class HandshakePayload(val count: Int,val tag: String)

    private var onConnected={}
    fun setOnConnected(callback: () -> Unit) {
        onConnected = callback
    }
    private var onDisconnected={}
    fun setOnDisconnected(callback: () -> Unit) {
        onDisconnected = callback
    }
}