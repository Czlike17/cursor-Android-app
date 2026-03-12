package com.example.myapp.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.myapp.data.local.AppDatabase
import com.example.myapp.data.mqtt.MqttClientManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 网络状态监听器
 * 当网络恢复时，自动重试发送离线指令
 */
@AndroidEntryPoint
class NetworkStateReceiver : BroadcastReceiver() {

    @Inject
    lateinit var mqttClientManager: MqttClientManager
    
    @Inject
    lateinit var preferencesManager: PreferencesManager
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ConnectivityManager.CONNECTIVITY_ACTION) {
            if (isNetworkAvailable(context)) {
                // 网络已恢复，重试离线指令
                retryOfflineCommands(context)
            }
        }
    }
    
    /**
     * 检查网络是否可用
     */
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    
    /**
     * 重试发送离线指令
     */
    private fun retryOfflineCommands(context: Context) {
        scope.launch {
            try {
                val username = preferencesManager.getUsernameSync() ?: return@launch
                val database = AppDatabase.getDatabase(context)
                val offlineCommandDao = database.offlineCommandDao()
                
                // 获取所有离线指令
                val offlineCommands = offlineCommandDao.getAllByUsername(username)
                
                for (command in offlineCommands) {
                    try {
                        // 发送MQTT指令
                        mqttClientManager.publish(
                            topic = command.topic,
                            payload = command.payload,
                            qos = command.qos
                        )
                        
                        // 发送成功，删除缓存
                        offlineCommandDao.deleteById(command.id)
                        
                    } catch (e: Exception) {
                        e.printStackTrace()
                        
                        // 更新重试次数
                        val newRetryCount = command.retryCount + 1
                        if (newRetryCount >= 3) {
                            // 重试3次后仍失败，删除指令
                            offlineCommandDao.deleteById(command.id)
                        } else {
                            offlineCommandDao.updateRetryCount(command.id, newRetryCount)
                        }
                    }
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

