# 🚀 Mock 系统快速使用指南

## 📋 5 分钟快速上手

### 第 1 步：编译安装 App

```bash
# 在项目根目录执行
./gradlew clean installDebug
```

或在 Android Studio 中点击 Run 按钮。

---

### 第 2 步：注册账号

1. 打开 App
2. 点击"注册"
3. 输入用户名（如：`testuser`）
4. 输入密码（至少 6 位）
5. 点击"注册"

**✅ 自动完成**：
- 注册成功后，`MockInitializer` 会自动注入 3 个虚拟设备
- 自动跳转到主页

---

### 第 3 步：查看设备列表

在 `HomeFragment` 中，你会看到：

**设备列表**（3 个虚拟设备）：
- 📱 客厅智能灯
- ❄️ 卧室空调
- 🌡️ 温湿度传感器

**环境数据卡片**（每 3 秒自动更新）：
- 🌡️ 温度：20-25°C
- 💧 湿度：45-60%
- ☀️ 光照：300-500 lux

---

### 第 4 步：测试设备控制

1. 点击"客厅智能灯"
2. 进入设备控制页面
3. 尝试以下操作：
   - 点击电源开关 → 200ms 后状态更新
   - 滑动亮度滑块 → 200ms 后固定位置
   - 选择颜色 → 200ms 后应用颜色

**✅ 预期效果**：
- 所有操作都有 200ms 延迟（模拟网络）
- UI 控件状态正确回弹/固定
- 无需真实硬件和网络

---

### 第 5 步：测试离线模式

1. 回到主页
2. 点击底部导航栏的"我的"
3. **连续点击版本号 5 次**
4. 弹出调试菜单
5. 选择"切换全局离线模式"

**✅ 预期效果**：
- 所有设备显示为离线（灰色指示）
- 设备控制页面显示"离线提示卡片"
- 所有控制按钮禁用

---

### 第 6 步：生成测试数据

1. 在调试菜单中选择"生成 30 天习惯数据"
2. 等待 2 秒
3. 提示"已生成 XXX 条习惯日志"
4. 打开"习惯"页面
5. 查看习惯列表和图表

**✅ 预期效果**：
- 生成 4 种典型习惯模式
- 图表显示时间分布和环境关联
- 数据真实可信

---

## 🎯 核心功能测试清单

### ✅ 虚拟设备注入
- [ ] 注册后自动创建 3 个设备
- [ ] 登录后自动创建设备（如果不存在）
- [ ] 设备显示在 HomeFragment

### ✅ 环境数据生成
- [ ] 环境卡片每 3 秒自动更新
- [ ] 温度在 20-25°C 之间平滑变化
- [ ] 湿度在 45-60% 之间平滑变化
- [ ] 光照在 300-500 lux 之间平滑变化

### ✅ 设备控制模拟
- [ ] 点击设备跳转到控制页面
- [ ] 电源开关延迟 200ms 响应
- [ ] 亮度滑块延迟 200ms 固定
- [ ] 颜色选择延迟 200ms 应用
- [ ] 空调模式切换延迟 200ms 响应
- [ ] 温度调节延迟 200ms 响应

### ✅ 离线模拟
- [ ] 可以切换全局离线模式
- [ ] 离线时设备显示灰色指示
- [ ] 离线时控制页面显示提示卡片
- [ ] 离线时所有控制按钮禁用
- [ ] 恢复在线后设备正常工作

### ✅ 测试数据生成
- [ ] 可以生成 30 天习惯数据
- [ ] 可以清除所有习惯数据
- [ ] 可以生成图表假数据
- [ ] 可以查看 Mock 状态

---

## 🐛 常见问题

### Q1: 注册后看不到设备？

**A**: 检查以下几点：
1. 确认 `BuildConfig.IS_MOCK_MODE = true`
2. 查看 Logcat 日志，搜索 `[MOCK]`
3. 手动刷新设备列表（下拉刷新）
4. 重新登录

### Q2: 环境数据不更新？

**A**: 
1. 确认 `MockEngineLifecycleManager` 已初始化
2. 查看 Logcat 日志：`[MOCK] Environment data updated`
3. 确认 App 在前台运行
4. 重启 App

### Q3: 设备控制没有反应？

**A**:
1. 确认设备在线（不是全局离线模式）
2. 查看 Logcat 日志：`[MOCK] Publishing to topic`
3. 等待 200ms 延迟
4. 检查数据库中的设备状态

### Q4: 调试菜单打不开？

**A**:
1. 确认 `BuildConfig.IS_MOCK_MODE = true`
2. 在"我的"页面连续点击版本号 5 次
3. 确认 `MockDebugHelper` 已注入
4. 查看 Logcat 日志

---

## 📊 Logcat 日志示例

### 正常运行的日志

```
[MOCK] UnifiedMockSystem initialized
[MOCK] Injecting virtual devices for user: testuser
[MOCK] Device created: 客厅智能灯
[MOCK] Device created: 卧室空调
[MOCK] Device created: 温湿度传感器
[MOCK] Successfully injected 3 virtual devices
[MOCK] Environment data updated: temp=22.5°C, humidity=55%, light=400lux
[MOCK] Connecting to MQTT: mock.broker.local, clientId: light_001
[MOCK] MQTT connected successfully
[MOCK] Publishing to topic: /device/light_001/command, payload: {"power":"on"}
[MOCK] Status response sent: {"deviceId":"light_001","power":"on","timestamp":1234567890}
```

### 离线模式的日志

```
[MOCK] Global offline mode: true
[MOCK] Updated 3 devices online status to: false
[MOCK] Device control failed: Device is offline (global offline mode)
```

---

## 🎓 进阶使用

### 自定义虚拟设备

编辑 `UnifiedMockSystem.kt`，修改 `injectVirtualDevices()` 方法：

```kotlin
// 添加更多虚拟设备
val devices = listOf(
    createVirtualLight(username),
    createVirtualAirConditioner(username),
    createVirtualTempHumiditySensor(username),
    createVirtualCurtain(username), // 新增窗帘
    createVirtualSocket(username)   // 新增插座
)
```

### 自定义环境数据范围

编辑 `UnifiedMockSystem.kt`，修改 `startEnvironmentDataGeneration()` 方法：

```kotlin
// 修改温度范围为 18-28°C
currentTemp = smoothTransition(currentTemp, 18f, 28f, 0.5f)

// 修改湿度范围为 40-70%
currentHumidity = smoothTransition(currentHumidity, 40f, 70f, 2f)

// 修改光照范围为 200-800 lux
currentLight = smoothTransition(currentLight, 200f, 800f, 50f)
```

### 自定义响应延迟

编辑 `MockMqttClientManager.kt`，修改 `publish()` 方法：

```kotlin
// 修改延迟为 500ms
delay(500)
```

---

## 🚀 性能优化建议

### 1. 环境数据更新频率

当前：每 3 秒更新一次

如果觉得太频繁，可以修改为 5 秒：

```kotlin
// UnifiedMockSystem.kt
delay(5000) // 改为 5 秒
```

### 2. 随机掉线频率

当前：3-5 分钟触发一次

如果觉得太频繁，可以修改为 10-15 分钟：

```kotlin
// MockMqttClientManager.kt
val delayMinutes = Random.nextInt(10, 16) // 改为 10-15 分钟
```

### 3. 习惯数据生成数量

当前：30 天约 120 条日志

如果需要更多数据，可以修改为 90 天：

```kotlin
// MockHabitDataGenerator.kt
for (dayOffset in 0 until 90) { // 改为 90 天
    // ...
}
```

---

## 📞 技术支持

如果遇到问题，请提供以下信息：

1. **Logcat 日志**（搜索 `[MOCK]`）
2. **操作步骤**（如何复现问题）
3. **预期结果** vs **实际结果**
4. **设备信息**（Android 版本、机型）

---

## 🎉 总结

使用本 Mock 系统，你可以：

✅ **无需硬件**：完全本地运行，不需要真实设备
✅ **无需网络**：所有数据都是模拟生成的
✅ **完整体验**：所有 UI 交互和业务流转都能正常工作
✅ **快速测试**：5 分钟即可完成所有功能测试
✅ **易于调试**：丰富的日志和调试菜单
✅ **无痛移除**：硬件就绪后 3 步即可移除

**开始测试吧！** 🚀














