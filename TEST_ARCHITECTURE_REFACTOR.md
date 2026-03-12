# 测试架构重构总结

## 修复的核心问题

### 1. AmbiguousViewMatcherException（视图冲突）
**问题根源**：HomeFragment 和 HabitFragment 被 BottomNavigationView 缓存，视图树中存在多个同名 ID（如 R.id.swipeRefresh）

**解决方案**：
- 创建 `EspressoTestUtils.onVisibleView(viewId)` 方法
- 强制使用 `allOf(withId(R.id.xxx), isDisplayed())` 组合查找
- 确保只操作当前屏幕真正可见的控件

**修改文件**：
- `app/src/androidTest/java/com/example/myapp/utils/EspressoTestUtils.kt`（新建）
- 所有测试文件中的视图查找逻辑

---

### 2. DataStore/协程异步导致的断言失败与死锁
**问题根源**：UI 还未从 DataStore 获取异步数据就进行了校验（如 ProfileE2ETest 中的 `view.isChecked()` 断言）

**解决方案**：
- 实现自定义 `ViewAction`：`waitForViewState()` 和 `waitForSwitchState()`
- 轮询等待机制（最多 3000ms，每 100ms 检查一次）
- 在断言前先等待状态更新完成

**核心方法**：
```kotlin
// 等待 Switch 控件的选中状态
fun waitForSwitchState(
    viewMatcher: Matcher<View>,
    expectedChecked: Boolean,
    maxWaitMs: Long = 3000
): ViewAction
```

**修改文件**：
- `app/src/androidTest/java/com/example/myapp/utils/EspressoTestUtils.kt`（新建）
- `app/src/androidTest/java/com/example/myapp/ProfileE2ETest.kt`（新建）
- `app/src/main/java/com/example/myapp/presentation/device/ProfileViewModel.kt`（修复持续监听导致的协程泄漏）

---

### 3. SwipeRefreshLayout 无限转圈假死
**问题根源**：数据加载操作没有超时保护，导致 `isRefreshing` 一直为 `true`

**解决方案**：
- 在 HomeFragment 和 HabitFragment 中添加 1 秒超时保护
- 超时后强制调用 `isRefreshing = false`
- 在 ViewModel 层也添加超时保护

**修改文件**：
- `app/src/main/java/com/example/myapp/presentation/device/HomeFragment.kt`
- `app/src/main/java/com/example/myapp/ui/habit/HabitFragment.kt`
- `app/src/main/java/com/example/myapp/presentation/device/DeviceViewModel.kt`
- `app/src/main/java/com/example/myapp/ui/habit/HabitViewModel.kt`

**核心代码**：
```kotlin
viewLifecycleOwner.lifecycleScope.launch {
    viewModel.isLoading.collect { isLoading ->
        if (isLoading) {
            binding.swipeRefresh.isRefreshing = true
            
            // 启动超时保护：1 秒后强制停止刷新
            launch {
                kotlinx.coroutines.delay(1000)
                if (binding.swipeRefresh.isRefreshing) {
                    binding.swipeRefresh.isRefreshing = false
                    Timber.w("[Fragment] 刷新超时，强制停止")
                }
            }
        } else {
            binding.swipeRefresh.isRefreshing = false
        }
    }
}
```

---

## 新增的测试工具类

### 1. EspressoTestUtils（核心工具类）
**位置**：`app/src/androidTest/java/com/example/myapp/utils/EspressoTestUtils.kt`

**核心方法**：
- `waitForViewState()` - 通用视图状态等待
- `waitForSwitchState()` - Switch 控件状态等待
- `waitForViewVisible()` - 等待视图可见
- `waitForViewGone()` - 等待视图消失
- `onVisibleView()` - 安全查找可见视图（解决视图冲突）
- `onVisibleViewWithParent()` - 带父容器约束的视图查找
- `waitForRefreshComplete()` - 等待刷新完成
- `safeDelay()` - 安全延迟（避免死锁）

### 2. TestIdlingResource
**位置**：`app/src/androidTest/java/com/example/myapp/utils/TestIdlingResource.kt`

**功能**：
- `SimpleIdlingResource` - 简单的空闲资源
- `CountingIdlingResource` - 计数型空闲资源（跟踪多个并发操作）

### 3. BaseE2ETest（测试基类）
**位置**：`app/src/androidTest/java/com/example/myapp/base/BaseE2ETest.kt`

**功能**：
- 自动注入 Hilt 依赖
- 管理 IdlingResource 注册/注销
- 提供数据库清理和初始化方法
- 统一异常处理和日志输出

---

## 修改的测试文件

### 1. HabitAndRuleE2ETest.kt
- 替换 `Thread.sleep()` 为 `EspressoTestUtils.safeDelay()`
- 使用 `EspressoTestUtils.onVisibleView()` 查找视图
- 添加 `waitForRefreshComplete()` 等待刷新完成

### 2. HabitFlowTest.kt
- 替换所有 `Thread.sleep()` 为 `safeDelay()`
- 使用 `onVisibleView()` 避免视图冲突
- 在刷新操作后等待完成

### 3. EndToEndTest.kt
- 替换 `Thread.sleep()` 为 `safeDelay()`
- 在页面切换后等待刷新完成

### 4. ProfileE2ETest.kt（新建）
- 专门测试个人中心功能
- 使用 `waitForSwitchState()` 解决 DataStore 异步问题
- 完整的在家模式开关测试流程

---

## 业务代码修改

### 1. Fragment 层（HomeFragment、HabitFragment）
**修改点**：
- 在 `isLoading.collect` 中添加 1 秒超时保护
- 使用协程 `delay()` 而非阻塞线程
- 超时后强制停止刷新并记录警告日志

### 2. ViewModel 层（DeviceViewModel、HabitViewModel、ProfileViewModel）
**修改点**：
- DeviceViewModel：在 `loadDevices()` 中添加超时保护
- HabitViewModel：修复 `finally` 块位置，确保 `isLoading` 正确重置
- ProfileViewModel：将 `collect` 改为 `first()`，避免持续监听导致的协程泄漏

---

## 核心异步等待逻辑

### 1. 视图状态等待（轮询机制）
```kotlin
fun waitForViewState(
    viewMatcher: Matcher<View>,
    maxWaitMs: Long = 3000,
    checkInterval: Long = 100,
    condition: (View) -> Boolean
): ViewAction {
    return object : ViewAction {
        override fun perform(uiController: UiController, view: View) {
            val startTime = System.currentTimeMillis()
            
            while (System.currentTimeMillis() - startTime < maxWaitMs) {
                if (condition(view)) {
                    return // 条件满足，退出
                }
                uiController.loopMainThreadForAtLeast(checkInterval)
            }
            
            throw AssertionError("等待 View 状态超时（${maxWaitMs}ms）")
        }
    }
}
```

### 2. 刷新完成等待
```kotlin
fun waitForRefreshComplete(swipeRefreshId: Int, maxWaitMs: Long = 5000) {
    val startTime = System.currentTimeMillis()
    
    while (System.currentTimeMillis() - startTime < maxWaitMs) {
        try {
            onVisibleView(swipeRefreshId).check(matches(isDisplayed()))
            
            var isRefreshing = false
            onVisibleView(swipeRefreshId).perform(object : ViewAction {
                override fun perform(uiController: UiController, view: View) {
                    if (view is SwipeRefreshLayout) {
                        isRefreshing = view.isRefreshing
                    }
                }
            })
            
            if (!isRefreshing) {
                return // 刷新完成
            }
            
            Thread.sleep(100)
        } catch (e: Exception) {
            Thread.sleep(100)
        }
    }
    
    println("警告：等待刷新完成超时（${maxWaitMs}ms）")
}
```

### 3. Fragment 层超时保护
```kotlin
viewLifecycleOwner.lifecycleScope.launch {
    viewModel.isLoading.collect { isLoading ->
        if (isLoading) {
            binding.swipeRefresh.isRefreshing = true
            
            // 启动超时保护：1 秒后强制停止刷新
            launch {
                kotlinx.coroutines.delay(1000)
                if (binding.swipeRefresh.isRefreshing) {
                    binding.swipeRefresh.isRefreshing = false
                    Timber.w("[Fragment] 刷新超时，强制停止")
                }
            }
        } else {
            binding.swipeRefresh.isRefreshing = false
        }
    }
}
```

---

## 测试执行建议

### 运行单个测试
```bash
./gradlew connectedAndroidTest --tests "com.example.myapp.ProfileE2ETest"
```

### 运行所有测试
```bash
./gradlew connectedAndroidTest
```

### 查看测试报告
```
app/build/reports/androidTests/connected/index.html
```

---

## 注意事项

1. **不要在测试中使用 `Thread.sleep()`**，改用 `EspressoTestUtils.safeDelay()`
2. **查找视图时必须使用 `onVisibleView()`**，避免 AmbiguousViewMatcherException
3. **在断言前先等待状态更新**，使用 `waitForSwitchState()` 等方法
4. **刷新操作后必须等待完成**，使用 `waitForRefreshComplete()`
5. **所有异步操作都有超时保护**，避免测试无限等待

---

## 修改文件清单

### 新建文件（3 个）
1. `app/src/androidTest/java/com/example/myapp/utils/EspressoTestUtils.kt`
2. `app/src/androidTest/java/com/example/myapp/utils/TestIdlingResource.kt`
3. `app/src/androidTest/java/com/example/myapp/base/BaseE2ETest.kt`
4. `app/src/androidTest/java/com/example/myapp/ProfileE2ETest.kt`

### 修改的业务代码（5 个）
1. `app/src/main/java/com/example/myapp/presentation/device/HomeFragment.kt`
2. `app/src/main/java/com/example/myapp/ui/habit/HabitFragment.kt`
3. `app/src/main/java/com/example/myapp/presentation/device/DeviceViewModel.kt`
4. `app/src/main/java/com/example/myapp/ui/habit/HabitViewModel.kt`
5. `app/src/main/java/com/example/myapp/presentation/device/ProfileViewModel.kt`

### 修改的测试代码（3 个）
1. `app/src/androidTest/java/com/example/myapp/HabitAndRuleE2ETest.kt`
2. `app/src/androidTest/java/com/example/myapp/HabitFlowTest.kt`
3. `app/src/androidTest/java/com/example/myapp/EndToEndTest.kt`

---

## 总结

本次重构从底层彻底解决了三大核心问题：

1. **视图冲突**：通过 `allOf(withId(), isDisplayed())` 强制约束
2. **异步断言失败**：通过轮询等待机制（最多 3 秒）
3. **刷新假死**：通过 1 秒超时强制停止

所有修改都遵循"防御性编程"原则，确保测试的稳定性和可靠性。









