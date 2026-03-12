package com.example.myapp.constants

/**
 * 应用常量
 */
object AppConstants {

    // SharedPreferences 键名
    object SPKey {
        const val USER_TOKEN = "user_token"
        const val USER_ID = "user_id"
        const val IS_FIRST_LAUNCH = "is_first_launch"
        const val LANGUAGE = "language"
    }

    // Intent 键名
    object IntentKey {
        const val DATA = "data"
        const val ID = "id"
        const val TITLE = "title"
        const val TYPE = "type"
    }

    // 网络请求相关
    object Network {
        const val CONNECT_TIMEOUT = 30L
        const val READ_TIMEOUT = 30L
        const val WRITE_TIMEOUT = 30L
        const val SUCCESS_CODE = 200
    }

    // 分页相关
    object Page {
        const val DEFAULT_PAGE = 1
        const val DEFAULT_PAGE_SIZE = 20
    }

    // 数据库相关
    object Database {
        const val NAME = "app_database"
        const val VERSION = 1
    }

    // MQTT 相关
    object Mqtt {
        const val QOS_0 = 0
        const val QOS_1 = 1
        const val QOS_2 = 2
        const val KEEP_ALIVE_INTERVAL = 60
        const val CONNECTION_TIMEOUT = 30
    }
}

