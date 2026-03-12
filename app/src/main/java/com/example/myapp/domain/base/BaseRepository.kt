package com.example.myapp.domain.base

/**
 * Repository 基类
 * 提供通用的数据操作封装
 */
abstract class BaseRepository {

    /**
     * 安全执行操作，捕获异常并返回 Result
     */
    protected suspend fun <T> safeApiCall(
        apiCall: suspend () -> T
    ): Result<T> {
        return try {
            Result.success(apiCall())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 执行数据库操作
     */
    protected suspend fun <T> safeDatabaseCall(
        databaseCall: suspend () -> T
    ): Result<T> {
        return try {
            Result.success(databaseCall())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

