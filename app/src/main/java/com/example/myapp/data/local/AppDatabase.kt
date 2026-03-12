package com.example.myapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.myapp.data.local.dao.*
import com.example.myapp.data.local.entity.*

/**
 * 智能家居 App 数据库
 * 支持多账号数据隔离
 * 
 * 数据库版本: 1
 * 包含表:
 * - user: 用户账号表
 * - device: 设备表
 * - user_habit_log: 用户操作日志表
 * - user_habit: 用户习惯模型表
 * - environment_cache: 环境数据缓存表
 * - auto_control_log: 自动控制日志表
 */
@Database(
    entities = [
        User::class,
        Device::class,
        UserHabitLog::class,
        UserHabit::class,
        EnvironmentCache::class,
        AutoControlLog::class,
        OfflineCommand::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    /**
     * 用户 DAO
     */
    abstract fun userDao(): UserDao

    /**
     * 设备 DAO
     */
    abstract fun deviceDao(): DeviceDao

    /**
     * 用户操作日志 DAO
     */
    abstract fun userHabitLogDao(): UserHabitLogDao

    /**
     * 用户习惯模型 DAO
     */
    abstract fun userHabitDao(): UserHabitDao

    /**
     * 环境数据缓存 DAO
     */
    abstract fun environmentCacheDao(): EnvironmentCacheDao

    /**
     * 自动控制日志 DAO
     */
    abstract fun autoControlLogDao(): AutoControlLogDao

    /**
     * 离线指令 DAO
     */
    abstract fun offlineCommandDao(): OfflineCommandDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private const val DATABASE_NAME = "smart_home_database"

        /**
         * 获取数据库实例（单例模式）
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    // 在生产环境中应该使用 Migration 而不是 fallbackToDestructiveMigration
                    .fallbackToDestructiveMigration()
                    // 可选：添加数据库回调
                    // .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * 清除数据库实例（用于测试）
         */
        fun clearInstance() {
            INSTANCE = null
        }
    }

    /**
     * 数据库回调（可选）
     * 用于在数据库创建或打开时执行操作
     */
    /*
    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // 数据库首次创建时的操作
            Timber.d("Database created")
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            // 数据库打开时的操作
            Timber.d("Database opened")
        }
    }
    */
}

