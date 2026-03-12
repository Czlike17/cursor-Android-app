package com.example.myapp.util

import android.content.Context
import androidx.work.*
import com.example.myapp.data.worker.EnvironmentDataWorker
import com.example.myapp.data.worker.HabitExtractionWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * WorkManager 调度器
 * 负责启动和管理后台任务
 */
object WorkManagerScheduler {
    
    private const val ENVIRONMENT_WORK_NAME = "environment_data_work"
    private const val HABIT_EXTRACTION_WORK_NAME = "habit_extraction_work"
    
    /**
     * 启动环境数据采集任务
     * 每3分钟执行一次
     */
    fun scheduleEnvironmentDataCollection(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // 需要网络连接
            .build()
        
        val workRequest = PeriodicWorkRequestBuilder<EnvironmentDataWorker>(
            repeatInterval = 3,
            repeatIntervalTimeUnit = TimeUnit.MINUTES,
            flexTimeInterval = 1, // 允许1分钟的弹性时间
            flexTimeIntervalUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            ENVIRONMENT_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // 如果已存在则保持
            workRequest
        )
    }
    
    /**
     * 启动习惯提取任务
     * 每日凌晨2点执行（设备充电时）
     */
    fun scheduleHabitExtraction(context: Context) {
        // 计算到凌晨2点的延迟时间
        val currentTime = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 2)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            
            // 如果今天的2点已过，则设置为明天2点
            if (before(currentTime)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        
        val initialDelay = targetTime.timeInMillis - currentTime.timeInMillis
        
        val constraints = Constraints.Builder()
            .setRequiresCharging(true) // 需要充电
            .build()
        
        val workRequest = PeriodicWorkRequestBuilder<HabitExtractionWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            HABIT_EXTRACTION_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
    
    /**
     * 取消环境数据采集任务
     */
    fun cancelEnvironmentDataCollection(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(ENVIRONMENT_WORK_NAME)
    }
    
    /**
     * 取消习惯提取任务
     */
    fun cancelHabitExtraction(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(HABIT_EXTRACTION_WORK_NAME)
    }
    
    /**
     * 立即执行一次环境数据采集
     */
    fun runEnvironmentDataCollectionNow(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<EnvironmentDataWorker>()
            .build()
        
        WorkManager.getInstance(context).enqueue(workRequest)
    }
    
    /**
     * 立即执行一次习惯提取
     */
    fun runHabitExtractionNow(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<HabitExtractionWorker>()
            .build()
        
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}

