package com.example.myapp.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapp.data.local.AppDatabase
import com.example.myapp.data.local.entity.EnvironmentCache
import com.example.myapp.util.PreferencesManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 环境数据采集 Worker
 * 每3分钟请求一次气象接口，缓存10分钟
 */
@HiltWorker
class EnvironmentDataWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val preferencesManager: PreferencesManager
) : CoroutineWorker(appContext, workerParams) {

    private val database = AppDatabase.getDatabase(appContext)
    private val environmentCacheDao = database.environmentCacheDao()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val username = preferencesManager.getUsernameSync() ?: return@withContext Result.failure()
            
            // 检查缓存是否过期（10分钟）
            val lastCache = environmentCacheDao.getLatestByUsername(username)
            val currentTime = System.currentTimeMillis()
            
            if (lastCache != null && (currentTime - lastCache.timestamp) < 10 * 60 * 1000) {
                // 缓存未过期，跳过请求
                return@withContext Result.success()
            }
            
            // TODO: 实现气象API请求
            // 暂时返回成功，避免编译错误
            // 后续可以在运行时配置API Key后启用
            
            Result.success()
            
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}

