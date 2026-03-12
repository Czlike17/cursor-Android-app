# MyAndroidApp

一个基于 Clean Architecture + MVVM 架构的 Android 应用项目模板。

## 📱 项目信息

- **最低 SDK**: 29 (Android 10)
- **目标 SDK**: 34 (Android 14)
- **语言**: Kotlin 1.9.22
- **构建工具**: Gradle 8.2 + AGP 8.2.2
- **架构**: Clean Architecture + MVVM

## 🚀 快速开始

### 环境要求

- JDK 17 或更高版本
- Android Studio Hedgehog (2023.1.1) 或更高版本
- Android SDK 34

### 初始化项目

1. **克隆或下载项目**
2. **用 Android Studio 打开项目**
3. **同步 Gradle**: 点击 `Sync Project with Gradle Files`
4. **运行项目**: 点击 Run 按钮

详细步骤请查看 [QUICKSTART.md](QUICKSTART.md)

## 📚 文档

- [快速开始指南](QUICKSTART.md) - 项目配置和使用说明
- [架构说明](ARCHITECTURE.md) - 详细的架构设计文档
- [配置检查清单](CHECKLIST.md) - 完整的配置验证清单

## 🏗️ 技术栈

### 核心框架
- **Kotlin** - 主要开发语言
- **Coroutines & Flow** - 异步编程
- **Hilt** - 依赖注入
- **ViewBinding** - 视图绑定

### 架构组件
- **ViewModel** - 视图模型
- **LiveData** - 可观察数据
- **Lifecycle** - 生命周期管理
- **Room** - 本地数据库
- **DataStore** - 数据存储

### 网络通信
- **Retrofit** - HTTP 客户端
- **OkHttp** - 网络请求
- **Gson** - JSON 解析
- **Eclipse Paho** - MQTT 客户端

### UI 组件
- **Material Design 3** - UI 设计规范
- **ConstraintLayout** - 约束布局
- **SwipeRefreshLayout** - 下拉刷新
- **CardView** - 卡片视图

### 第三方库
- **Timber** - 日志框架
- **Glide** - 图片加载
- **MPAndroidChart** - 图表库
- **HoloColorPicker** - 颜色选择器
- **StepIndicator** - 步骤指示器
- **PermissionX** - 权限管理

## 📁 项目结构

```
app/
├── data/                    # 数据层
│   ├── local/              # 本地数据源 (Room, DataStore)
│   ├── remote/             # 远程数据源 (Retrofit)
│   ├── mqtt/               # MQTT 客户端
│   ├── model/              # 数据模型
│   └── repository/         # 数据仓库
├── domain/                  # 领域层
│   └── base/               # 基础类
├── presentation/            # 表现层
│   ├── base/               # 基类 (Activity, Fragment, ViewModel)
│   ├── main/               # 主界面
│   └── widget/             # 自定义控件
├── di/                      # 依赖注入模块
├── util/                    # 工具类
├── constants/               # 常量定义
└── MyApplication.kt        # Application 类
```

## 🎯 核心特性

### 1. 完整的基类体系

- **BaseActivity** - 提供 ViewBinding、ViewModel、加载状态管理
- **BaseFragment** - Fragment 基类，功能同 BaseActivity
- **BaseViewModel** - 统一的状态管理和错误处理
- **BaseRepository** - 安全的数据访问封装

### 2. 依赖注入

使用 Hilt 进行依赖注入，模块化管理依赖：
- AppModule - 应用级依赖
- DatabaseModule - 数据库依赖
- NetworkModule - 网络依赖

### 3. 数据存储

- **Room** - 结构化数据存储
- **DataStore** - 键值对存储（替代 SharedPreferences）
- **EncryptedSharedPreferences** - 加密存储敏感数据

### 4. 网络请求

- Retrofit + OkHttp 实现 RESTful API 调用
- 统一的响应模型和错误处理
- 支持请求日志拦截

### 5. MQTT 支持

完整的 MQTT 客户端实现：
- 连接管理
- 主题订阅/取消订阅
- 消息发布/接收
- 自动重连

### 6. 丰富的工具类

- **网络检测** - NetworkUtils
- **日期处理** - DateUtils
- **数据验证** - ValidateUtils
- **加密解密** - EncryptUtils
- **尺寸转换** - DensityUtils
- **键盘管理** - KeyboardUtils
- **View 扩展** - ViewExtensions
- **协程扩展** - CoroutineExtensions

## 💡 使用示例

### 创建新的 Activity

```kotlin
@AndroidEntryPoint
class YourActivity : BaseActivity<ActivityYourBinding, YourViewModel>() {
    
    override fun createViewBinding(inflater: LayoutInflater) = 
        ActivityYourBinding.inflate(inflater)
    
    override fun getViewModelClass() = YourViewModel::class.java
    
    override fun initView() {
        binding.button.setOnSingleClickListener {
            viewModel.loadData()
        }
    }
    
    override fun initData() {
        viewModel.loadData()
    }
    
    override fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    if (isLoading) showLoading() else hideLoading()
                }
            }
        }
    }
}
```

### 创建 ViewModel

```kotlin
@HiltViewModel
class YourViewModel @Inject constructor(
    private val repository: YourRepository
) : BaseViewModel() {
    
    fun loadData() {
        executeWithLoading(
            block = { repository.getData() },
            onSuccess = { data -> /* 处理成功 */ },
            onError = { error -> /* 处理错误 */ }
        )
    }
}
```

## 🔧 配置说明

### 修改包名

1. 在 Android Studio 中右键点击包名
2. 选择 `Refactor` → `Rename`
3. 输入新的包名并应用

### 配置 API 地址

在 `NetworkModule.kt` 中修改：

```kotlin
private const val BASE_URL = "https://your-api.com/"
```

### 配置 MQTT

```kotlin
val mqttManager = MqttManager(
    context = this,
    serverUri = "tcp://your-broker.com:1883",
    clientId = "your_client_id"
)
```

## 🧪 测试

```bash
# 运行单元测试
./gradlew test

# 运行 UI 测试
./gradlew connectedAndroidTest
```

## 📦 构建

```bash
# Debug 构建
./gradlew assembleDebug

# Release 构建
./gradlew assembleRelease

# 清理项目
./gradlew clean
```

## 📝 开发规范

- 遵循 Kotlin 官方代码风格
- 使用 Clean Architecture 分层
- 所有公共方法添加注释
- 使用 Timber 进行日志输出
- 异常必须妥善处理

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 License

MIT License

---

**开始你的 Android 开发之旅吧！** 🎉

