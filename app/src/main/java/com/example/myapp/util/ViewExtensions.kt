package com.example.myapp.util

import android.view.View

/**
 * View 扩展函数
 */

// 显示 View
fun View.visible() {
    visibility = View.VISIBLE
}

// 隐藏 View（占位）
fun View.invisible() {
    visibility = View.INVISIBLE
}

// 隐藏 View（不占位）
fun View.gone() {
    visibility = View.GONE
}

// 切换可见性
fun View.toggleVisibility() {
    visibility = if (visibility == View.VISIBLE) View.GONE else View.VISIBLE
}

// 根据条件设置可见性
fun View.visibleIf(condition: Boolean) {
    visibility = if (condition) View.VISIBLE else View.GONE
}

// 设置点击事件并防止快速点击
fun View.setOnSingleClickListener(interval: Long = 500, onClick: (View) -> Unit) {
    var lastClickTime = 0L
    setOnClickListener {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime > interval) {
            lastClickTime = currentTime
            onClick(it)
        }
    }
}

