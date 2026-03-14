package com.example.myapp.engine

import com.example.myapp.BuildConfig
import com.example.myapp.data.local.entity.UserHabit
import com.example.myapp.data.local.entity.UserHabitLog
import com.google.gson.Gson
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * 企业级 AI 习惯提取引擎
 * 基于一维环形时序 DBSCAN 密度聚类算法
 */
@Singleton
class HabitExtractionEngine @Inject constructor() {

    private val gson = Gson()

    // 聚类核心超参数配置
    private val epsMinutes = 15 // 邻域半径：15分钟容差

    // 【动态降级策略】：Debug 下连续3次操作即可触发成簇；Release 下要求30天内达20次
    private val minPts = if (BuildConfig.DEBUG) 3 else 20
    private val timeWindowMs = if (BuildConfig.DEBUG) {
        1 * 60 * 60 * 1000L // 测试环境：仅抓取近 1 小时的日志以防干扰
    } else {
        30L * 24 * 60 * 60 * 1000L // 生产环境：分析近 30 天日志
    }

    /**
     * 执行聚类提取，返回新生成的习惯规则（草稿状态）
     */
    fun extractHabits(logs: List<UserHabitLog>, username: String): List<UserHabit> {
        val generatedHabits = mutableListOf<UserHabit>()
        val now = System.currentTimeMillis()

        // 过滤有效期内的数据
        val validLogs = logs.filter { (now - it.operateTime) <= timeWindowMs }

        // 1. 按 "设备_动作" 分组 (例如：设备A + 开灯，必须是一个独立的行为轨迹)
        val groupedLogs = validLogs.groupBy { "${it.deviceId}_${it.action}" }

        for ((_, logGroup) in groupedLogs) {
            // 快速剪枝：如果总数连最低成簇阈值都达不到，直接跳过
            if (logGroup.size < minPts) continue

            val deviceId = logGroup.first().deviceId
            val action = logGroup.first().action

            // 2. 执行一维环形 DBSCAN 聚类
            val clusters = dbscan(logGroup)

            // 3. 将有效的“操作簇”提取封装为习惯规则
            for (cluster in clusters) {
                if (cluster.size < minPts) continue

                val habit = createHabitFromCluster(cluster, deviceId, action, username)
                if (habit != null) {
                    generatedHabits.add(habit)
                }
            }
        }

        return generatedHabits
    }

    /**
     * DBSCAN 聚类实现
     */
    private fun dbscan(logs: List<UserHabitLog>): List<List<UserHabitLog>> {
        val clusters = mutableListOf<MutableList<UserHabitLog>>()
        val visited = mutableSetOf<UserHabitLog>()
        val isClustered = mutableSetOf<UserHabitLog>()

        for (log in logs) {
            if (visited.contains(log)) continue
            visited.add(log)

            val neighbors = getNeighbors(log, logs)
            if (neighbors.size < minPts) {
                // 标记为噪声点 (Noise)，忽略
            } else {
                val cluster = mutableListOf<UserHabitLog>()
                clusters.add(cluster)
                expandCluster(log, neighbors, cluster, visited, isClustered, logs)
            }
        }
        return clusters
    }

    private fun expandCluster(
        log: UserHabitLog,
        neighbors: MutableList<UserHabitLog>,
        cluster: MutableList<UserHabitLog>,
        visited: MutableSet<UserHabitLog>,
        isClustered: MutableSet<UserHabitLog>,
        allLogs: List<UserHabitLog>
    ) {
        cluster.add(log)
        isClustered.add(log)

        var i = 0
        while (i < neighbors.size) {
            val currentNeighbor = neighbors[i]
            if (!visited.contains(currentNeighbor)) {
                visited.add(currentNeighbor)
                val currentNeighbors = getNeighbors(currentNeighbor, allLogs)
                if (currentNeighbors.size >= minPts) {
                    // 只添加尚未在邻域中的节点
                    for (n in currentNeighbors) {
                        if (n !in neighbors) neighbors.add(n)
                    }
                }
            }
            if (!isClustered.contains(currentNeighbor)) {
                cluster.add(currentNeighbor)
                isClustered.add(currentNeighbor)
            }
            i++
        }
    }

    private fun getNeighbors(core: UserHabitLog, allLogs: List<UserHabitLog>): MutableList<UserHabitLog> {
        val neighbors = mutableListOf<UserHabitLog>()
        val coreMinute = getMinuteOfDay(core.operateTime)

        for (log in allLogs) {
            val logMinute = getMinuteOfDay(log.operateTime)
            // 使用环形距离判定是否在邻域内
            if (circularDistance(coreMinute, logMinute) <= epsMinutes) {
                neighbors.add(log)
            }
        }
        return neighbors
    }

    /**
     * 【企业级核心】：计算圆周最短距离 (0~1439分钟)，完美解决 23:55 和 00:05 的距离计算问题
     */
    private fun circularDistance(m1: Int, m2: Int): Int {
        val diff = abs(m1 - m2)
        return Math.min(diff, 1440 - diff)
    }

    private fun getMinuteOfDay(timestamp: Long): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    private fun createHabitFromCluster(
        cluster: List<UserHabitLog>,
        deviceId: Long,
        action: String,
        username: String
    ): UserHabit? {
        try {
            // 1. 计算时间窗口 (处理跨夜平均值)
            val (startMin, endMin) = calculateTimeWindow(cluster)
            val timeWindow = "${formatTime(startMin)}-${formatTime(endMin)}"

            // 2. 提取星期规律 (Bitmask位运算存储)
            var weekTypeMask = 0
            val dayCounts = cluster.groupBy { it.weekType }.mapValues { it.value.size }
            // 提取阈值：如果某一天出现频率超过 30%，则认为它是一个周期规律点
            val threshold = (cluster.size * 0.3).toInt().coerceAtLeast(1)
            for ((day, count) in dayCounts) {
                if (count >= threshold) {
                    weekTypeMask = weekTypeMask or (1 shl day)
                }
            }

            // 3. 提取环境规律
            val envThreshold = extractEnvironmentThreshold(cluster)

            // 4. 置信度计算 (满分 1.0)
            val confidence = if (BuildConfig.DEBUG) 0.9 else (cluster.size / 30.0).coerceAtMost(1.0)

            // 5. 解析友好的动作名称用于 UI 展示
            val actionName = try {
                val map = gson.fromJson(action, Map::class.java) as? Map<String, Any>
                if (map?.get("power") == "on") "打开" else "关闭"
            } catch (e: Exception) {
                "操作"
            }

            // 6. 构造草稿状态的习惯规则
            return UserHabit(
                id = 0, // 新增标识
                deviceId = deviceId,
                habitName = "AI挖掘: 自动$actionName",
                triggerCondition = "{}", // UI 侧主要靠解析独立字段，此处仅占位
                actionCommand = action,
                weekType = weekTypeMask,
                timeWindow = timeWindow,
                environmentThreshold = envThreshold,
                confidence = confidence,
                isEnabled = false, // 【关键交互】：以草稿形式存在，等待用户到界面开启
                username = username
            )

        } catch (e: Exception) {
            Timber.e(e, "AI Sourcing: 从操作簇生成习惯规则失败")
            return null
        }
    }

    /**
     * 【企业级核心】：利用极坐标向量求平均角度，处理跨夜取平均值的数学痛点
     */
    private fun calculateTimeWindow(cluster: List<UserHabitLog>): Pair<Int, Int> {
        var sumSin = 0.0
        var sumCos = 0.0
        for (log in cluster) {
            val min = getMinuteOfDay(log.operateTime)
            val angle = min * (2 * Math.PI / 1440.0) // 映射到 360 度
            sumSin += sin(angle)
            sumCos += cos(angle)
        }

        var avgAngle = atan2(sumSin / cluster.size, sumCos / cluster.size)
        if (avgAngle < 0) avgAngle += 2 * Math.PI

        val centerMinute = (avgAngle / (2 * Math.PI) * 1440).toInt()

        // 基于中心点上下浮动 15 分钟，形成 30 分钟安全窗口
        var startMin = centerMinute - 15
        var endMin = centerMinute + 15

        // 跨夜圆周处理
        if (startMin < 0) startMin += 1440
        if (endMin >= 1440) endMin -= 1440

        return Pair(startMin, endMin)
    }

    private fun formatTime(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return String.format("%02d:%02d", h, m)
    }

    private fun extractEnvironmentThreshold(cluster: List<UserHabitLog>): String? {
        val temps = mutableListOf<Double>()
        val humids = mutableListOf<Double>()

        for (log in cluster) {
            log.environmentData?.let { envStr ->
                try {
                    val map = gson.fromJson(envStr, Map::class.java) as? Map<String, Double>
                    map?.get("temperature")?.let { temps.add(it) }
                    map?.get("humidity")?.let { humids.add(it) }
                } catch (e: Exception) { }
            }
        }

        val thresholdMap = mutableMapOf<String, Any>()

        // 过滤比例：如果簇内超过 70% 的操作都带有环境数据，才去提取环境规律
        val ratio = 0.7
        if (temps.size >= cluster.size * ratio) {
            temps.sort()
            // 去除 10% 最高和最低极值，防止极端天气干扰
            val trimCount = (temps.size * 0.1).toInt()
            val validTemps = temps.subList(trimCount, temps.size - trimCount)
            if (validTemps.isNotEmpty()) {
                val minT = validTemps.minOrNull() ?: 0.0
                val maxT = validTemps.maxOrNull() ?: 0.0
                // 【业务逻辑断言】：操作期间温差波动小于 5 度，说明环境对操作有强引导性
                if (maxT - minT <= 5.0) {
                    thresholdMap["temperature"] = mapOf("min" to minT.toInt(), "max" to maxT.toInt())
                }
            }
        }

        if (humids.size >= cluster.size * ratio) {
            humids.sort()
            val trimCount = (humids.size * 0.1).toInt()
            val validHumids = humids.subList(trimCount, humids.size - trimCount)
            if (validHumids.isNotEmpty()) {
                val minH = validHumids.minOrNull() ?: 0.0
                val maxH = validHumids.maxOrNull() ?: 0.0
                if (maxH - minH <= 15.0) {
                    thresholdMap["humidity"] = mapOf("min" to minH.toInt(), "max" to maxH.toInt())
                }
            }
        }

        return if (thresholdMap.isNotEmpty()) gson.toJson(thresholdMap) else null
    }
}