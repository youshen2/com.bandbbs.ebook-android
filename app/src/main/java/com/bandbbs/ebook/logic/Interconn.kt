package com.bandbbs.ebook.logic

import android.content.Context
import android.util.Log
import com.xiaomi.xms.wearable.Wearable.getAuthApi
import com.xiaomi.xms.wearable.Wearable.getMessageApi
import com.xiaomi.xms.wearable.Wearable.getNodeApi
import com.xiaomi.xms.wearable.auth.AuthApi
import com.xiaomi.xms.wearable.auth.Permission
import com.xiaomi.xms.wearable.message.MessageApi
import com.xiaomi.xms.wearable.message.OnMessageReceivedListener
import com.xiaomi.xms.wearable.node.Node
import com.xiaomi.xms.wearable.node.NodeApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

open class Interconn(context: Context) {
    val nodeApi: NodeApi = getNodeApi(context)
    val authApi: AuthApi = getAuthApi(context)
    val messageApi: MessageApi = getMessageApi(context)
    var currentNode: Node? = null
    val json = Json {
        ignoreUnknownKeys = true
    }
    val onMessage = mutableMapOf<String, (String) -> Unit>()
    open val onMessageListener = OnMessageReceivedListener { _, message ->
        Log.d("Interconn", message.decodeToString())
        val message = message.decodeToString()
        val msg = json.decodeFromString<Message>(message)
        onMessage[msg.tag]?.invoke(message)
    }

    fun connect(): CompletableDeferred<String> {
        return CompletableDeferred<String>().apply {
            nodeApi.connectedNodes.addOnSuccessListener { nodes ->
                if (nodes.isEmpty()) {
                    completeExceptionally(Exception("未找到设备！"))
                    return@addOnSuccessListener
                }
                currentNode = nodes[0]
                complete(nodes[0].name)
            }.addOnFailureListener {
                completeExceptionally(Exception("获取设备列表失败，请检查小米运动健康是否已连接！"))
            }
        }
    }

    fun auth(): CompletableDeferred<Unit> {
        val permissions = arrayOf<Permission?>(Permission.DEVICE_MANAGER)
        return CompletableDeferred<Unit>().apply {
            val node = currentNode
            if (node == null) {
                completeExceptionally(Exception("设备未连接！"))
                return@apply
            }

            authApi.checkPermissions(node.id, permissions).addOnSuccessListener { results ->
                for ((index, value) in results.withIndex()) {
                    if (!value) {
                        authApi.requestPermission(node.id, permissions[index])
                            .addOnFailureListener { error ->
                                Log.e("Auth", "Auth failed", error)
                            }
                    }
                }
                complete(Unit)
            }.addOnFailureListener {
                completeExceptionally(Exception("获取权限失败！"))
            }
        }
    }

    fun openApp(): CompletableDeferred<Unit> {
        return CompletableDeferred<Unit>().apply {
            val node = currentNode
            if (node == null) {
                completeExceptionally(Exception("设备未连接！"))
                return@apply
            }

            nodeApi.launchWearApp(node.id, "/pages/push").addOnSuccessListener {
                Log.d("OpenApp", "success")
                complete(Unit)
            }.addOnFailureListener {
                Log.e("OpenApp", "fail", it)
                completeExceptionally(Exception("打开应用失败！"))
            }
        }
    }

    fun registerListener(): CompletableDeferred<Unit> {
        return CompletableDeferred<Unit>().apply {
            val node = currentNode
            if (node == null) {
                completeExceptionally(Exception("设备未连接！"))
                return@apply
            }

            messageApi.addListener(node.id, onMessageListener)
                .addOnSuccessListener {
                    complete(Unit)
                }
                .addOnFailureListener { error ->
                    val msg = error.message.orEmpty()
                    if (msg.contains("You have registered", ignoreCase = true)) {
                        Log.w("RegisterListener", "Listener already registered, continue")
                        complete(Unit)
                    } else {
                        completeExceptionally(error)
                    }
                }
        }
    }

    open fun sendMessage(message: String): CompletableDeferred<Unit> {
        Log.d("Interconn >>>", message)
        return CompletableDeferred<Unit>().apply {
            val node = currentNode
            if (node == null) {
                completeExceptionally(Exception("设备未连接！"))
                return@apply
            }

            messageApi.sendMessage(node.id, message.toByteArray()).addOnSuccessListener {
                complete(Unit)
            }.addOnFailureListener {
                Log.e("Send", "fail", it)
                completeExceptionally(it)
            }
        }
    }

    fun addListener(type: String, callback: (String) -> Unit) {
        onMessage[type] = callback
    }

    fun removeListener(type: String) {
        onMessage.remove(type)
    }

    @Serializable
    data class Message(val tag: String)

    fun getAppState(): CompletableDeferred<Boolean> {
        return CompletableDeferred<Boolean>().apply {
            val node = currentNode
            if (node == null) {
                completeExceptionally(Exception("设备未连接！"))
                return@apply
            }

            nodeApi.isWearAppInstalled(node.id).addOnSuccessListener { complete(it) }
                .addOnFailureListener { completeExceptionally(it) }
        }
    }

    fun destroy(): CompletableDeferred<Unit> {
        return CompletableDeferred<Unit>().apply {
            val node = currentNode
            if (node == null) {
                complete(Unit)
            } else {
                messageApi.removeListener(node.id).addOnSuccessListener {
                    currentNode = null
                    complete(Unit)
                }.addOnFailureListener {
                    currentNode = null
                    complete(Unit)
                }
            }
        }
    }

    open suspend fun init() {
        if (currentNode != null) return
        connect().await()
        auth().await()
        openApp().await()
    }
}
