package com.example.myapp.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapp.data.repository.UserHabitRepository
import com.example.myapp.engine.HabitExtractionEngine
import com.example.myapp.util.PreferencesManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Calendar

/**
 * AI 习惯提取 Worker
 * 每日后台运行，接入一维环形时序 DBSCAN 算法，静默生成用户习惯草稿
 */
@HiltWorker
class HabitExtractionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val preferencesManager: PreferencesManager,
    private val habitExtractionEngine: HabitExtractionEngine,
    private val userHabitRepository: UserHabitRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val username = preferencesManager.getUsernameSync() ?: return@withContext Result.failure()
            Timber.i("AI Sourcing: Worker started for user $username")

            // 1. 定义时间窗口 (固定抓取近30天的矿脉，引擎内部会根据 DEBUG 标志做降级裁决)
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -30)
            val startTime = calendar.timeInMillis
            val endTime = System.currentTimeMillis()

            // 2. 抓取“矿脉”数据
            val logsResult = userHabitRepository.getLogsByTimeRangeOnce(startTime, endTime, username)
            val logs = logsResult.getOrNull()

            if (logs.isNullOrEmpty()) {
                Timber.i("AI Sourcing: No logs found, aborting extraction")
                return@withContext Result.success()
            }

            // 3. 将数据送入 DBSCAN 核心引擎进行高维聚类
            val generatedHabits = habitExtractionEngine.extractHabits(logs, username)

            if (generatedHabits.isEmpty()) {
                Timber.i("AI Sourcing: No obvious patterns found by DBSCAN engine")
                return@withContext Result.success()
            }

            // 4. 智能去重并持久化落盘（由 Repository 处理草稿和合并逻辑）
            userHabitRepository.mergeAndSaveAIHabits(generatedHabits, username)

            Timber.i("AI Sourcing: Successfully extracted and merged ${generatedHabits.size} habit patterns")
            Result.success()

        } catch (e: Exception) {
            Timber.e(e, "AI Sourcing: Habit extraction failed")
            Result.retry() // 发生异常时可以尝试重试
        }
    }
}