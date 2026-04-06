# PhoneClaw UI 设计方案

> 基于 Material Design 3 设计风格，模仿千问、DeepSeek 等主流 AI 应用

## 1. 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                      PhoneClawApp                           │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────┐    │
│  │                  AgentStatusManager                  │    │
│  │        (全局状态：思考/工具调用/技能调用)             │    │
│  └─────────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────────┤
│  UI Layer                                                   │
│  ┌──────────────┬──────────────┬──────────────────────┐    │
│  │ ui/chat/     │ ui/settings/ │ ui/components/       │    │
│  │ - ChatActivity│ - ModelSettingsFragment            │    │
│  │ - MessageAdapter│ - SkillSettingsFragment          │    │
│  │ - DrawerFragment│ - PermissionSettingsFragment     │    │
│  │                - ScriptServerFragment              │    │
│  └──────────────┴──────────────┴──────────────────────┘    │
├─────────────────────────────────────────────────────────────┤
│  Presenter Layer                                            │
│  ┌──────────────┬──────────────┬──────────────────────┐    │
│  │ ChatPresenter│ SettingsPresenter                   │    │
│  └──────────────┴──────────────┴──────────────────────┘    │
├─────────────────────────────────────────────────────────────┤
│  Data Layer (现有)                                          │
│  PhoneClawDbHelper / FileBasedSkillRepository              │
│  PhoneClawAgent / EmuApi                                   │
└─────────────────────────────────────────────────────────────┘
```

## 2. 页面设计

### 2.1 ChatActivity (主界面)

```
┌────────────────────────────────────────┐
│ ≡  [对话标题]              [设置图标] │  <- TopAppBar (Material 3)
├────────────────────────────────────────┤
│                                        │
│    消息列表 (RecyclerView)              │
│    ┌──────────────────────────────┐    │
│    │ 用户消息 (右对齐气泡)         │    │
│    └──────────────────────────────┘    │
│    ┌──────────────────────────────┐    │
│    │ Agent 消息 (左对齐气泡)       │    │
│    └──────────────────────────────┘    │
│    ┌──────────────────────────────┐    │
│    │ 🔧 工具调用卡片 (可展开)      │    │
│    │    clickById                 │    │
│    │    ⏳ 调用中...              │    │
│    └──────────────────────────────┘    │
│    ┌──────────────────────────────┐    │
│    │ 📜 技能调用卡片              │    │
│    │    phone_emulation          │    │
│    │    ✓ 成功                   │    │
│    └──────────────────────────────┘    │
│    ┌──────────────────────────────┐    │
│    │ 🤔 思考中...                 │    │
│    └──────────────────────────────┘    │
│                                        │
├────────────────────────────────────────┤
│ [📷] [🎤/⌨️] [输入框...]    [模型▼] [▶]│  <- 输入栏
│                                        │
│ 图标说明:                              │
│ 📷 = 选择图片                          │
│ 🎤/⌨️ = 切换语音/键盘输入             │
│ [模型▼] = 选择模型下拉菜单             │
│ ▶ = 发送按钮 (运行时变为 ⏹ 停止)      │
└────────────────────────────────────────┘
```

### 2.2 左侧 DrawerLayout (侧滑抽屉)

```
┌──────────────────┐
│ ┌──────────────┐ │
│ │ + 新建对话   │ │  <- 主要操作按钮
│ └──────────────┘ │
├──────────────────┤
│ 最近一周         │  <- 时间分组标题
│  ├─ 对话标题1    │
│  ├─ 对话标题2    │
│  └─ 对话标题3    │
├──────────────────┤
│ 更早             │
│  ├─ 对话标题4    │
│  └─ 对话标题5    │
├──────────────────┤
│                  │
│ ⚙ 设置          │  <- 底部设置入口
│                  │
└──────────────────┘

交互:
- 点击对话项：切换到该对话
- 长按对话项：显示删除/重命名菜单
- 点击新建对话：创建新会话
- 点击设置：跳转 SettingsActivity
```

### 2.3 SettingsActivity (设置页面)

```
┌────────────────────────────────────────┐
│ ← 设置                                │
├────────────────────────────────────────┤
│                                        │
│ ┌────────────────────────────────────┐ │
│ │ 🤖 模型设置                    >   │ │
│ │    管理模型提供商                  │ │
│ │    当前: OpenAI GPT-4             │ │
│ └────────────────────────────────────┘ │
│                                        │
│ ┌────────────────────────────────────┐ │
│ │ 📜 技能设置                    >   │ │
│ │    管理可用技能                    │ │
│ │    已启用: 2 个技能               │ │
│ └────────────────────────────────────┘ │
│                                        │
│ ┌────────────────────────────────────┐ │
│ │ 🔐 权限设置                        │ │
│ │                                    │ │
│ │ 无障碍服务        [────●] 开启    │ │
│ │ 悬浮窗权限        [●────] 未授权  │ │
│ │ 保活功能          [●────] 关闭    │ │
│ └────────────────────────────────────┘ │
│                                        │
│ ┌────────────────────────────────────┐ │
│ │ 📡 Lua 脚本服务器              >   │ │
│ │    运行状态: 已停止                │ │
│ │    点击进入详细配置                │ │
│ └────────────────────────────────────┘ │
│                                        │
└────────────────────────────────────────┘
```

### 2.4 模型设置二级页面

```
┌────────────────────────────────────────┐
│ ← 模型设置              [+ 添加]      │
├────────────────────────────────────────┤
│                                        │
│ ┌────────────────────────────────────┐ │
│ │ ⭐ OpenAI GPT-4            [默认]  │ │
│ │    api.openai.com                  │ │
│ │    [编辑] [删除]                   │ │
│ └────────────────────────────────────┘ │
│                                        │
│ ┌────────────────────────────────────┐ │
│ │    DeepSeek V3                     │ │
│ │    api.deepseek.com                │ │
│ │    [设为默认] [编辑] [删除]        │ │
│ └────────────────────────────────────┘ │
│                                        │
│ 点击 + 添加 → 新建模型提供商表单       │
│                                        │
└────────────────────────────────────────┘

新建/编辑模型表单:
┌────────────────────────────────────────┐
│ 名称: [_______________]               │
│ API 类型: [OpenAI 兼容 ▼]             │
│ Base URL: [_______________]           │
│ API Key: [_______________]            │
│ 默认模型: [_______________]           │
│ 视觉能力: [○] 是  [●] 否             │
│                                        │
│         [取消]  [保存]                │
└────────────────────────────────────────┘
```

### 2.5 技能设置二级页面

```
┌────────────────────────────────────────┐
│ ← 技能设置              [+ 添加]      │
├────────────────────────────────────────┤
│                                        │
│ ┌────────────────────────────────────┐ │
│ │ [●] phone_emulation               │ │
│ │     手机自动化操作                 │ │
│ │     用户可调用: 是                 │ │
│ └────────────────────────────────────┘ │
│                                        │
│ ┌────────────────────────────────────┐ │
│ │ [●] custom_skill_1                │ │
│ │     自定义技能描述                 │ │
│ │     用户可调用: 否                 │ │
│ └────────────────────────────────────┘ │
│                                        │
│ 技能项操作:                            │
│ - 开关控制是否启用                     │
│ - 点击查看详情                         │
│ - 长按编辑/删除                        │
│                                        │
└────────────────────────────────────────┘
```

### 2.6 悬浮窗设计

```
状态 1: 思考中
┌─────────────────┐
│ 🤔 思考中...    │
└─────────────────┘

状态 2: 工具调用中
┌─────────────────┐
│ 🔧 clickById    │
│    ⏳ 调用中... │
└─────────────────┘

状态 3: 工具调用成功
┌─────────────────┐
│ 🔧 clickById    │
│    ✓ 成功       │
└─────────────────┘

状态 4: 工具调用失败
┌─────────────────┐
│ 🔧 clickById    │
│    ✗ 失败       │
└─────────────────┘

状态 5: 技能调用
┌─────────────────┐
│ 📜 phone_emu    │
│    ⏳ 执行中... │
└─────────────────┘

设计要求:
- 尺寸小巧: 约 120dp x 48dp
- 位置: 屏幕顶部或底部边缘
- 点击穿透: 不拦截触摸事件
- 不对无障碍服务展示
- 动画过渡: 类似灵动岛效果
- 透明背景 + 圆角卡片
```

## 3. 消息类型设计

### 3.1 消息数据结构

```kotlin
sealed class MessageItem {
    abstract val id: String
    abstract val timestamp: Long
    
    data class UserMessage(
        override val id: String,
        override val timestamp: Long,
        val content: String,
        val images: List<ImageInfo>? = null
    ) : MessageItem()
    
    data class AgentMessage(
        override val id: String,
        override val timestamp: Long,
        val content: String
    ) : MessageItem()
    
    data class ToolCallMessage(
        override val id: String,
        override val timestamp: Long,
        val toolName: String,
        val params: String,
        var result: String? = null,
        var state: CallState = CallState.RUNNING
    ) : MessageItem() {
        enum class CallState { RUNNING, SUCCESS, FAILED }
    }
    
    data class SkillCallMessage(
        override val id: String,
        override val timestamp: Long,
        val skillName: String,
        var state: CallState = CallState.RUNNING
    ) : MessageItem() {
        enum class CallState { RUNNING, SUCCESS, FAILED }
    }
    
    data class ThinkingMessage(
        override val id: String,
        override val timestamp: Long,
        var status: String = "思考中..."
    ) : MessageItem()
}
```

### 3.2 消息 UI 样式

```
用户消息:
┌─────────────────────────────┐
│ 这里是用户输入的消息内容     │  <- 右对齐，蓝色背景
└─────────────────────────────┘
                              时间戳

Agent 消息:
┌─────────────────────────────┐
│ 这里是 Agent 的回复内容      │  <- 左对齐，灰色背景
└─────────────────────────────┘
时间戳

工具调用卡片:
┌─────────────────────────────────────────┐
│ 🔧 工具调用                        [▼] │
├─────────────────────────────────────────┤
│ clickById                               │
│ 状态: ⏳ 调用中...                      │
├─────────────────────────────────────────┤
│ 点击展开查看详情 ▼                      │
└─────────────────────────────────────────┘

展开后:
┌─────────────────────────────────────────┐
│ 🔧 工具调用                        [▲] │
├─────────────────────────────────────────┤
│ clickById                               │
│ 状态: ✓ 成功                            │
├─────────────────────────────────────────┤
│ 参数:                                   │
│ {                                       │
│   "id": "com.example:id/button"        │
│ }                                       │
├─────────────────────────────────────────┤
│ 返回:                                   │
│ true                                    │
└─────────────────────────────────────────┘

技能调用卡片:
┌─────────────────────────────────────────┐
│ 📜 技能调用                      [查看] │
├─────────────────────────────────────────┤
│ phone_emulation                         │
│ 状态: ⏳ 执行中...                      │
└─────────────────────────────────────────┘

思考状态:
┌─────────────────────────────────────────┐
│ 🤔 思考中...                            │
│    分析任务要求...                      │  <- 动态更新
└─────────────────────────────────────────┘
```

## 4. 全局状态管理

### 4.1 AgentStatusManager

```kotlin
object AgentStatusManager {
    private val _status = MutableStateFlow<AgentStatus>(AgentStatus.Idle)
    val status: StateFlow<AgentStatus> = _status.asStateFlow()
    
    sealed class AgentStatus {
        object Idle : AgentStatus()
        data class Thinking(val message: String = "思考中...") : AgentStatus()
        data class ToolCalling(
            val name: String,
            val state: CallState
        ) : AgentStatus()
        data class SkillCalling(
            val name: String,
            val state: CallState
        ) : AgentStatus()
    }
    
    enum class CallState { RUNNING, SUCCESS, FAILED }
    
    fun setStatus(status: AgentStatus) {
        _status.value = status
    }
    
    fun reset() {
        _status.value = AgentStatus.Idle
    }
}
```

### 4.2 状态流转图

```
                    ┌─────────┐
                    │  Idle   │
                    └────┬────┘
                         │ 用户发送消息
                         ▼
                    ┌─────────┐
              ┌─────│Thinking │─────┐
              │     └────┬────┘     │
              │          │          │
              ▼          ▼          ▼
        ┌──────────┐ ┌──────────┐ ┌──────────┐
        │ToolCalling│ │SkillCalling│ │  完成    │
        └─────┬────┘ └─────┬────┘ └────┬─────┘
              │            │            │
              │   成功/失败 │   成功/失败 │
              └─────┬──────┴─────┬──────┘
                    │            │
                    ▼            ▼
                    ┌─────────────┐
                    │   Thinking  │ (继续处理)
                    └─────────────┘
                          │
                          │ 完成
                          ▼
                    ┌─────────┐
                    │  Idle   │
                    └─────────┘
```

## 5. 横屏/平板适配

### 5.1 布局策略

**手机竖屏:**
- DrawerLayout 侧滑抽屉
- 全屏消息列表

**手机横屏:**
- DrawerLayout 侧滑抽屉（保持一致）
- 消息区域自动调整宽度

**平板横屏 (sw600dp):**
```
┌──────────────┬──────────────────────────────────────┐
│              │                                      │
│  固定左侧栏   │           消息列表区域               │
│  (Drawer)    │                                      │
│              │                                      │
│  对话列表     │                                      │
│              │                                      │
│  设置入口     │                                      │
│              │                                      │
└──────────────┴──────────────────────────────────────┘
```

### 5.2 资源目录

```
res/
├── layout/                    # 默认布局 (手机竖屏)
│   ├── activity_chat.xml
│   └── fragment_drawer.xml
├── layout-land/               # 横屏布局
│   └── activity_chat.xml
├── layout-sw600dp/            # 平板布局
│   ├── activity_chat.xml
│   └── fragment_drawer.xml
└── layout-sw600dp-land/       # 平板横屏
    └── activity_chat.xml      # 固定双栏布局
```

## 6. 颜色主题 (Material Design 3)

### 6.1 亮色主题

```xml
<!-- colors.xml -->
<color name="primary">#1976D2</color>           <!-- 主色调 -->
<color name="primary_container">#BBDEFB</color> <!-- 主色容器 -->
<color name="on_primary">#FFFFFF</color>        <!-- 主色上文字 -->
<color name="secondary">#7B1FA2</color>         <!-- 次要色 -->
<color name="surface">#FFFFFF</color>           <!-- 表面色 -->
<color name="on_surface">#212121</color>        <!-- 表面上文字 -->
<color name="background">#FAFAFA</color>        <!-- 背景色 -->
<color name="error">#D32F2F</color>             <!-- 错误色 -->
<color name="success">#388E3C</color>           <!-- 成功色 -->
<color name="warning">#F57C00</color>           <!-- 警告色 -->
```

### 6.2 暗色主题 (可选)

```xml
<!-- colors.xml (night) -->
<color name="primary">#90CAF9</color>
<color name="primary_container">#1565C0</color>
<color name="surface">#121212</color>
<color name="background">#121212</color>
```

## 7. 动画设计

### 7.1 悬浮窗状态切换

```kotlin
// 类似灵动岛效果
ObjectAnimator.ofPropertyValuesHolder(
    floatingView,
    PropertyValuesHolder.ofFloat("scaleX", 1f, 1.2f, 1f),
    PropertyValuesHolder.ofFloat("scaleY", 1f, 1.2f, 1f)
).apply {
    duration = 300
    interpolator = AnticipateOvershootInterpolator()
}.start()
```

### 7.2 消息发送动画

```kotlin
// 新消息淡入
val animator = ObjectAnimator.ofFloat(messageView, "alpha", 0f, 1f)
animator.duration = 200
animator.start()

// RecyclerView 滚动到底部
recyclerView.smoothScrollToPosition(adapter.itemCount - 1)
```

### 7.3 Drawer 切换动画

```xml
<!-- 使用 Material Motion -->
<com.google.android.material.transition.MaterialContainerTransform
    android:duration="300"
    android:interpolator="@android:interpolator/fast_out_slow_in" />
```

## 8. 交互规范

### 8.1 手势操作

- **左滑**: 打开 Drawer（或从左边缘滑入）
- **右滑**: 关闭 Drawer
- **长按消息**: 复制/删除菜单
- **长按对话项**: 删除/重命名菜单
- **下拉刷新**: 重新加载会话列表（可选）

### 8.2 反馈机制

- **发送成功**: 消息立即显示，带加载指示器
- **Agent 响应**: 流式显示，实时更新
- **工具调用**: 状态卡片动态更新
- **错误提示**: Snackbar 显示错误信息
- **操作确认**: 重要操作（删除会话）需确认对话框
