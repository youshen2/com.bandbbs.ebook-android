package com.bandbbs.ebook.logic
import android.content.Context
import android.util.Log
import com.xiaomi.xms.wearable.Wearable.*
import com.xiaomi.xms.wearable.auth.AuthApi
import com.xiaomi.xms.wearable.auth.Permission
import com.xiaomi.xms.wearable.message.MessageApi
import com.xiaomi.xms.wearable.message.OnMessageReceivedListener
import com.xiaomi.xms.wearable.node.Node
import com.xiaomi.xms.wearable.node.NodeApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

open class Interconn(context: Context){
    val nodeApi: NodeApi = getNodeApi(context)
    val authApi: AuthApi = getAuthApi(context)
    val messageApi: MessageApi = getMessageApi(context)
    var currentNode: Node? = null
    val json=Json{
        ignoreUnknownKeys = true
    }
    val onMessage = mutableMapOf<String,(String) -> Unit>()
    open val onMessageListener = OnMessageReceivedListener{ _, message -> // 收到手表端应用发来的消息
        Log.d("Interconn",message.decodeToString())
        val message = message.decodeToString()
        val msg = json.decodeFromString<Message>(message)
        onMessage[msg.tag]?.invoke(message)
    }
    fun connect(): CompletableDeferred<String> {
        return CompletableDeferred<String>().apply {
            nodeApi.connectedNodes.addOnSuccessListener {
                if (it.isEmpty()){
                    completeExceptionally(Exception("未找到设备！"))
                    return@addOnSuccessListener
                }
                currentNode = it[0]
                complete(currentNode!!.name)
            }.addOnFailureListener {
                completeExceptionally(Exception("获取设备列表失败，请检查小米运动健康是否已连接！"))
            }
        }
    }
    fun auth():CompletableDeferred<Unit>{
        val permissions = arrayOf<Permission?>(Permission.DEVICE_MANAGER)
        return CompletableDeferred<Unit>().apply {
            authApi.checkPermissions(currentNode!!.id, permissions).addOnSuccessListener {
                for ((index,value) in it.withIndex()){
                    if (!value){
                        authApi.requestPermission(currentNode!!.id, permissions[index]).addOnFailureListener  {
                            Log.e("Auth","Auth success")
                        }
                    }
                }
                complete(Unit)
            }.addOnFailureListener {
                completeExceptionally(Exception("获取权限失败！"))
            }
        }
    }
    fun openApp():CompletableDeferred<Unit> {
        return CompletableDeferred<Unit>().apply {
            nodeApi.launchWearApp(currentNode!!.id,"/pages/push").addOnSuccessListener {
                Log.d("OpenApp","success")
                complete(Unit)
            }.addOnFailureListener {
                Log.e("OpenApp","fail",it)
                completeExceptionally(Exception("打开应用失败！"))
            }
        }
    }
    fun registerListener(): CompletableDeferred<Unit> {
        return CompletableDeferred<Unit>().apply {
            messageApi.addListener(currentNode!!.id,onMessageListener).addOnSuccessListener {
                complete(Unit)
            }.addOnFailureListener { completeExceptionally(it) }
        }
    }
    open fun sendMessage(message : String): CompletableDeferred<Unit> {
        Log.d("Interconn",message)
        return CompletableDeferred<Unit>().apply {
            messageApi.sendMessage(currentNode!!.id,  message.toByteArray()).addOnSuccessListener {
                complete(Unit)
            }.addOnFailureListener {
                Log.e("Send","fail",it)
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
            nodeApi.isWearAppInstalled(currentNode!!.id).addOnSuccessListener { complete(it) }.addOnFailureListener { completeExceptionally(it) }
        }
    }
    fun destroy():CompletableDeferred<Unit>{
        return CompletableDeferred<Unit>().apply {
            if (currentNode==null) complete(Unit)
            else{
                messageApi.removeListener(currentNode!!.id).addOnSuccessListener {
                    currentNode=null
                    complete(Unit)
                }.addOnFailureListener {
                    complete(Unit)
                }
            }
        }
    }
    suspend fun init(){
        if(currentNode!=null) return
        connect().await()
        auth().await()
        openApp().await()
    }
}
