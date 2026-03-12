package com.example.myapp.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 协程扩展函数
 */

// 在 IO 线程执行
suspend fun <T> ioContext(block: suspend CoroutineScope.() -> T): T {
    return withContext(Dispatchers.IO, block)
}

// 在主线程执行
suspend fun <T> mainContext(block: suspend CoroutineScope.() -> T): T {
    return withContext(Dispatchers.Main, block)
}

// 在默认线程执行
suspend fun <T> defaultContext(block: suspend CoroutineScope.() -> T): T {
    return withContext(Dispatchers.Default, block)
}

