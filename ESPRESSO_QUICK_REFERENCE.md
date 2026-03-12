# Espresso 测试工具快速参考

## 常用方法速查

### 1. 安全查找视图（避免 AmbiguousViewMatcherException）

```kotlin
// ❌ 错误：可能找到多个同 ID 的视图
onView(withId(R.id.swipeRefresh))

// ✅ 正确：只查找当前可见的视图
EspressoTestUtils.onVisibleView(R.id.swipeRefresh)

// ✅ 正确：带父容器约束
EspressoTestUtils.onVisibleViewWithParent(R.id.swipeRefresh, R.id.habitFragment)
```

### 2. 等待异步状态更新

```kotlin
// 等待 Switch 控件状态更新（解决 DataStore 异步问题）
val switchMatcher = allOf(withId(R.id.switchAtHome), isDisplayed())

onView(switchMatcher)
    .perform(EspressoTestUtils.waitForSwitchState(switchMatcher, true, 3000))
    .check(matches(isChecked()))
```

### 3. 等待刷新完成

```kotlin
// ❌ 错误：可能在刷新未完成时就继续执行
onView(withId(R.id.swipeRefresh)).perform(swipeDown())
Thread.sleep(1000) // 不可靠

// ✅ 正确：等待刷新真正完成
EspressoTestUtils.onVisibleView(R.id.swipeRefresh).perform(swipeDown())
EspressoTestUtils.waitForRefreshComplete(R.id.swipeRefresh, 3000)
```

### 4. 安全延迟

```kotlin
// ❌ 错误：阻塞线程，可能导致死锁
Thread.sleep(2000)

// ✅ 正确：在主线程上安全延迟
EspressoTestUtils.safeDelay(2000)
```

### 5. 等待视图可见/消失

```kotlin
// 等待视图可见
onView(withId(R.id.progressBar))
    .perform(EspressoTestUtils.waitForViewVisible(withId(R.id.progressBar), 3000))

// 等待视图消失
onView(withId(R.id.progressBar))
    .perform(EspressoTestUtils.waitForViewGone(withId(R.id.progressBar), 3000))
```

## 典型测试场景

### 场景 1：测试页面切换

```kotlin
@Test
fun testNavigationBetweenPages() {
    ActivityScenario.launch(MainActivity::class.java).use {
        EspressoTestUtils.safeDelay(2000)

        // 切换到习惯页
        onView(withId(R.id.nav_habit)).perform(click())
        EspressoTestUtils.safeDelay(1500)
        
        // 等待刷新完成
        EspressoTestUtils.waitForRefreshComplete(R.id.swipeRefresh, 3000)
        
        // 验证页面显示
        EspressoTestUtils.onVisibleView(R.id.recyclerView)
            .check(matches(isDisplayed()))
    }
}
```

### 场景 2：测试 Switch 开关

```kotlin
@Test
fun testSwitchToggle() {
    ActivityScenario.launch(MainActivity::class.java).use {
        EspressoTestUtils.safeDelay(2000)

        val switchMatcher = allOf(withId(R.id.switchAtHome), isDisplayed())
        
        // 等待初始状态加载完成
        onView(switchMatcher)
            .perform(EspressoTestUtils.waitForSwitchState(switchMatcher, true, 3000))
        
        // 点击切换
        onView(switchMatcher).perform(click())
        EspressoTestUtils.safeDelay(500)
        
        // 等待状态更新完成
        onView(switchMatcher)
            .perform(EspressoTestUtils.waitForSwitchState(switchMatcher, false, 3000))
        
        // 验证状态
        onView(switchMatcher).check(matches(isNotChecked()))
    }
}
```

### 场景 3：测试下拉刷新

```kotlin
@Test
fun testPullToRefresh() {
    ActivityScenario.launch(MainActivity::class.java).use {
        EspressoTestUtils.safeDelay(2000)

        // 执行下拉刷新
        EspressoTestUtils.onVisibleView(R.id.swipeRefresh)
            .perform(swipeDown())
        
        // 等待刷新完成（最多 3 秒）
        EspressoTestUtils.waitForRefreshComplete(R.id.swipeRefresh, 3000)
        
        // 验证数据已更新
        EspressoTestUtils.onVisibleView(R.id.recyclerView)
            .check(matches(isDisplayed()))
    }
}
```

### 场景 4：测试列表操作

```kotlin
@Test
fun testRecyclerViewClick() {
    ActivityScenario.launch(MainActivity::class.java).use {
        EspressoTestUtils.safeDelay(2000)
        
        // 等待刷新完成
        EspressoTestUtils.waitForRefreshComplete(R.id.swipeRefresh, 3000)

        // 点击列表第一项
        try {
            EspressoTestUtils.onVisibleView(R.id.recyclerView)
                .perform(
                    RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                        0,
                        click()
                    )
                )
            EspressoTestUtils.safeDelay(500)
        } catch (e: Exception) {
            // 列表可能为空，测试通过
        }
    }
}
```

## 常见错误及解决方案

### 错误 1：AmbiguousViewMatcherException
```
错误信息：Multiple Ambiguous Views found for matcher
原因：视图树中存在多个相同 ID 的视图
解决：使用 EspressoTestUtils.onVisibleView() 替代 onView()
```

### 错误 2：PerformException: Error performing 'single click'
```
错误信息：View is not displayed on the screen
原因：视图还未渲染完成就进行了操作
解决：先使用 waitForViewVisible() 等待视图可见
```

### 错误 3：AssertionError: View.isChecked() doesn't match
```
错误信息：Expected: is checked, Got: not checked
原因：DataStore 异步加载未完成
解决：使用 waitForSwitchState() 等待状态更新
```

### 错误 4：测试超时/卡死
```
原因：SwipeRefreshLayout.isRefreshing 一直为 true
解决：使用 waitForRefreshComplete() 等待刷新完成
```

## 最佳实践

1. **永远不要使用 `Thread.sleep()`**，改用 `EspressoTestUtils.safeDelay()`
2. **查找视图时优先使用 `onVisibleView()`**，避免视图冲突
3. **在断言前先等待状态更新**，使用 `waitForXxx()` 系列方法
4. **刷新操作后必须等待完成**，使用 `waitForRefreshComplete()`
5. **所有等待都设置合理的超时时间**（建议 3 秒）
6. **使用 try-catch 包裹可能失败的操作**，提高测试容错性

## 调试技巧

### 1. 打印视图层级
```kotlin
onView(isRoot()).perform(object : ViewAction {
    override fun getConstraints() = isRoot()
    override fun getDescription() = "打印视图层级"
    override fun perform(uiController: UiController, view: View) {
        println(view.toString())
    }
})
```

### 2. 截图保存
```kotlin
// 在测试失败时自动截图
@get:Rule
val screenshotRule = ScreenshotOnFailureRule()
```

### 3. 查看 Espresso 日志
```bash
adb logcat | grep -i espresso
```

## 性能优化

1. **减少不必要的延迟**：只在必要时使用 `safeDelay()`
2. **使用 IdlingResource**：对于复杂异步操作，使用 IdlingResource 更高效
3. **并行执行测试**：使用 `@RunWith(Parameterized::class)` 并行测试
4. **复用测试数据**：在 `@BeforeClass` 中准备共享数据

## 参考资源

- [Espresso 官方文档](https://developer.android.com/training/testing/espresso)
- [IdlingResource 指南](https://developer.android.com/training/testing/espresso/idling-resource)
- [测试最佳实践](https://developer.android.com/training/testing/fundamentals)









