# PhoneClaw UI 开发任务列表

> 基于 DESIGN.md 和 TECHNICAL.md 拆解的可执行任务步骤

## 任务概览

| 阶段 | 任务数 | 预计工作量 | 状态 |
|------|--------|-----------|------|
| Phase 1: 基础框架 | 12 | 2-3天 | 待开始 |
| Phase 2: Agent 集成 | 10 | 2-3天 | 待开始 |
| Phase 3: 设置页面 | 8 | 1-2天 | 待开始 |
| Phase 4: 悬浮窗 | 5 | 1天 | 待开始 |
| Phase 5: 优化完善 | 6 | 1-2天 | 待开始 |

**总预计工作量**: 7-11 天

---

## Phase 1: 基础框架 (优先级: 高)

### 1.1 项目结构初始化

#### Task 1.1.1 创建 UI 目录结构
**描述**: 创建完整的 UI 模块目录结构  
**文件**:
```
app/src/main/java/top/yudoge/phoneclaw/
├── ui/
│   ├── chat/
│   │   ├── model/
│   │   ├── viewholders/
│   │   └── drawer/
│   ├── settings/
│   │   ├── model/
│   │   └── skill/
│   ├── floating/
│   └── components/
└── core/
```
**依赖**: 无  
**验证**: 目录结构创建完成

#### Task 1.1.2 创建资源目录
**描述**: 创建布局和资源文件目录  
**文件**:
```
app/src/main/res/
├── layout/
├── layout-land/
├── layout-sw600dp/
├── values/
├── values-night/
├── drawable/
└── menu/
```
**依赖**: 无  
**验证**: 资源目录创建完成

---

### 1.2 依赖配置

#### Task 1.2.1 更新 build.gradle.kts
**描述**: 添加所需依赖和配置  
**文件**: `app/build.gradle.kts`  
**修改内容**:
- 添加 Material Design 3
- 添加 RecyclerView, ViewPager2 等 UI 组件
- 添加 Lifecycle, ViewModel
- 启用 ViewBinding
- 添加 Coil 图片加载库

**依赖**: 无  
**验证**: `./gradlew build` 成功

---

### 1.3 核心模型类

#### Task 1.3.1 创建 MessageItem 数据模型
**描述**: 定义消息类型的密封类  
**文件**: `ui/chat/model/MessageItem.kt`  
**内容**:
- UserMessage
- AgentMessage
- ToolCallMessage
- SkillCallMessage
- ThinkingMessage

**依赖**: 无  
**验证**: 编译通过

#### Task 1.3.2 创建 AgentStatusManager
**描述**: 实现全局状态管理单例  
**文件**: `core/AgentStatusManager.kt`  
**内容**:
- AgentStatus 密封类
- StateFlow 状态流
- 状态更新方法

**依赖**: 无  
**验证**: 编译通过

---

### 1.4 ChatActivity 基础实现

#### Task 1.4.1 创建 ChatContract 接口
**描述**: 定义 MVP 契约接口  
**文件**: `ui/chat/ChatContract.kt`  
**内容**:
- View 接口
- Presenter 接口

**依赖**: Task 1.3.1  
**验证**: 编译通过

#### Task 1.4.2 创建 activity_chat.xml 布局
**描述**: 创建主对话页面布局  
**文件**: `res/layout/activity_chat.xml`  
**内容**:
- DrawerLayout 根布局
- CoordinatorLayout 主内容区
- MaterialToolbar
- RecyclerView
- 输入区域（EditText, 按钮）

**依赖**: Task 1.2.1  
**验证**: 布局预览正常

#### Task 1.4.3 实现 ChatActivity 基础框架
**描述**: 创建 Activity 基础代码  
**文件**: `ui/chat/ChatActivity.kt`  
**内容**:
- ViewBinding 初始化
- Presenter 绑定
- 基础生命周期方法
- 空实现 View 接口方法

**依赖**: Task 1.4.1, Task 1.4.2  
**验证**: Activity 可启动，无崩溃

---

### 1.5 消息列表实现

#### Task 1.5.1 创建消息项布局文件
**描述**: 创建各类型消息的布局  
**文件**:
- `res/layout/item_message_user.xml`
- `res/layout/item_message_agent.xml`
- `res/layout/item_message_tool.xml`
- `res/layout/item_message_skill.xml`
- `res/layout/item_message_thinking.xml`

**依赖**: 无  
**验证**: 布局预览正常

#### Task 1.5.2 实现 BaseMessageViewHolder
**描述**: 创建 ViewHolder 基类  
**文件**: `ui/chat/viewholders/BaseMessageViewHolder.kt`  
**内容**:
- 抽象 bind 方法
- 通用工具方法

**依赖**: Task 1.5.1  
**验证**: 编译通过

#### Task 1.5.3 实现各类型 ViewHolder
**描述**: 实现具体的 ViewHolder  
**文件**:
- `ui/chat/viewholders/UserMessageViewHolder.kt`
- `ui/chat/viewholders/AgentMessageViewHolder.kt`
- `ui/chat/viewholders/ToolCallViewHolder.kt`
- `ui/chat/viewholders/SkillCallViewHolder.kt`
- `ui/chat/viewholders/ThinkingViewHolder.kt`

**依赖**: Task 1.5.2  
**验证**: 编译通过

#### Task 1.5.4 实现 MessageAdapter
**描述**: 实现消息列表适配器  
**文件**: `ui/chat/MessageAdapter.kt`  
**内容**:
- ListAdapter + DiffUtil
- 多类型支持
- 动态添加/更新/删除消息
- 思考状态管理

**依赖**: Task 1.5.3  
**验证**: 单元测试通过或手动测试列表显示

---

### 1.6 侧滑抽屉实现

#### Task 1.6.1 创建 DrawerFragment 布局
**描述**: 创建抽屉布局  
**文件**: `res/layout/fragment_drawer.xml`  
**内容**:
- 新建对话按钮
- 会话列表 RecyclerView
- 设置按钮

**依赖**: 无  
**验证**: 布局预览正常

#### Task 1.6.2 实现会话项布局
**描述**: 创建会话列表项布局  
**文件**:
- `res/layout/item_session.xml`
- `res/layout/item_session_group.xml`

**依赖**: 无  
**验证**: 布局预览正常

#### Task 1.6.3 实现 SessionAdapter
**描述**: 实现会话列表适配器  
**文件**: `ui/chat/drawer/SessionAdapter.kt`, `SessionGroupAdapter.kt`  
**内容**:
- 分组显示（最近一周/更早）
- 点击/长按事件

**依赖**: Task 1.6.2  
**验证**: 编译通过

#### Task 1.6.4 实现 DrawerFragment
**描述**: 实现抽屉 Fragment  
**文件**: `ui/chat/drawer/DrawerFragment.kt`  
**内容**:
- 加载会话列表
- 按时间分组
- 新建对话/设置导航

**依赖**: Task 1.6.1, Task 1.6.3  
**验证**: 抽屉可打开，列表显示

---

## Phase 2: Agent 集成 (优先级: 高)

### 2.1 Presenter 实现

#### Task 2.1.1 实现 ChatPresenter 基础功能
**描述**: 实现 Presenter 核心逻辑  
**文件**: `ui/chat/ChatPresenter.kt`  
**内容**:
- attachView/detachView
- loadSession
- createNewSession
- 协程作用域管理

**依赖**: Task 1.4.1  
**验证**: 编译通过

#### Task 2.1.2 实现消息发送流程
**描述**: 连接 Agent 发送消息  
**文件**: `ui/chat/ChatPresenter.kt` (更新)  
**内容**:
- sendMessage 方法
- 创建 PhoneClawAgent
- 调用 runSuspend
- 结果处理

**依赖**: Task 2.1.1  
**验证**: 可发送消息并收到响应

---

### 2.2 数据持久化

#### Task 2.2.1 扩展 PhoneClawDbHelper
**描述**: 添加工具调用/技能调用消息支持  
**文件**: `db/PhoneClawDbHelper.java` (更新)  
**修改内容**:
- 检查现有字段是否足够
- 如需添加字段，处理数据库升级

**依赖**: 无  
**验证**: 数据库操作正常

#### Task 2.2.2 创建 MessageRepository (可选)
**描述**: 封装消息数据操作  
**文件**: `db/MessageRepository.kt`  
**内容**:
- 封装消息 CRUD
- 类型转换

**依赖**: Task 2.2.1  
**验证**: 单元测试通过

---

### 2.3 消息状态管理

#### Task 2.3.1 实现思考状态显示
**描述**: 在消息列表中显示思考状态  
**文件**: 
- `ui/chat/ChatPresenter.kt` (更新)
- `ui/chat/MessageAdapter.kt` (更新)

**内容**:
- 发送消息前显示 ThinkingMessage
- Agent 响应后移除
- 更新 AgentStatusManager

**依赖**: Task 1.5.4, Task 2.1.2  
**验证**: 思考状态正确显示

#### Task 2.3.2 实现工具调用消息
**描述**: 显示工具调用过程和结果  
**文件**:
- `ui/chat/ChatPresenter.kt` (更新)
- `ui/chat/viewholders/ToolCallViewHolder.kt` (更新)

**内容**:
- 监听 Agent 工具调用事件
- 实时更新消息状态
- 可展开查看详情

**依赖**: Task 2.1.2  
**验证**: 工具调用正确显示

#### Task 2.3.3 实现技能调用消息
**描述**: 显示技能调用过程和结果  
**文件**:
- `ui/chat/ChatPresenter.kt` (更新)
- `ui/chat/viewholders/SkillCallViewHolder.kt` (更新)

**依赖**: Task 2.3.2  
**验证**: 技能调用正确显示

---

### 2.4 会话管理

#### Task 2.4.1 实现会话切换
**描述**: 在抽屉中切换不同会话  
**文件**: 
- `ui/chat/ChatActivity.kt` (更新)
- `ui/chat/ChatPresenter.kt` (更新)

**内容**:
- loadSession 加载历史消息
- 更新标题
- 更新模型选择

**依赖**: Task 1.6.4  
**验证**: 会话切换正常

#### Task 2.4.2 实现会话删除和重命名
**描述**: 长按会话显示操作菜单  
**文件**:
- `ui/chat/drawer/DrawerFragment.kt` (更新)
- `ui/chat/ChatPresenter.kt` (更新)

**内容**:
- 显示确认对话框
- 执行删除/重命名
- 刷新列表

**依赖**: Task 2.4.1  
**验证**: 删除/重命名正常

---

### 2.5 模型选择

#### Task 2.5.1 实现模型选择器
**描述**: 在输入栏添加模型下拉选择  
**文件**:
- `ui/components/ModelSpinner.kt`
- `ui/chat/ChatActivity.kt` (更新)
- `ui/chat/ChatPresenter.kt` (更新)

**内容**:
- 显示可用模型列表
- 选择后更新当前会话
- 持久化选择

**依赖**: Task 1.4.3  
**验证**: 模型选择正常

---

### 2.6 Agent 控制

#### Task 2.6.1 实现停止 Agent 功能
**描述**: 发送后显示停止按钮  
**文件**:
- `ui/chat/ChatActivity.kt` (更新)
- `ui/chat/ChatPresenter.kt` (更新)

**内容**:
- 发送时隐藏发送按钮，显示停止按钮
- 点击停止取消协程
- 重置状态

**依赖**: Task 2.1.2  
**验证**: 可正常停止 Agent

---

## Phase 3: 设置页面 (优先级: 中)

### 3.1 设置页面框架

#### Task 3.1.1 创建 SettingsActivity
**描述**: 创建设置主页面  
**文件**:
- `ui/settings/SettingsActivity.kt`
- `res/layout/activity_settings.xml`

**内容**:
- 设置项列表
- 点击跳转二级页面

**依赖**: 无  
**验证**: 设置页面可打开

---

### 3.2 模型设置

#### Task 3.2.1 实现 ModelSettingsFragment
**描述**: 实现模型提供商列表  
**文件**:
- `ui/settings/model/ModelSettingsFragment.kt`
- `res/layout/fragment_model_settings.xml`
- `ui/settings/model/ModelAdapter.kt`
- `res/layout/item_model.xml`

**内容**:
- 列表显示所有模型
- 设置默认模型
- 添加/编辑/删除入口

**依赖**: Task 3.1.1  
**验证**: 模型列表正常显示

#### Task 3.2.2 实现 ModelEditActivity
**描述**: 实现模型编辑页面  
**文件**:
- `ui/settings/model/ModelEditActivity.kt`
- `res/layout/activity_model_edit.xml`

**内容**:
- 表单输入
- 验证保存
- 动态配置项（根据 API 类型）

**依赖**: Task 3.2.1  
**验证**: 可添加/编辑模型

---

### 3.3 技能设置

#### Task 3.3.1 实现 SkillSettingsFragment
**描述**: 实现技能管理列表  
**文件**:
- `ui/settings/skill/SkillSettingsFragment.kt`
- `res/layout/fragment_skill_settings.xml`
- `ui/settings/skill/SkillAdapter.kt`
- `res/layout/item_skill.xml`

**内容**:
- 列表显示所有技能
- 开关控制启用/禁用
- 点击查看详情

**依赖**: Task 3.1.1  
**验证**: 技能列表正常显示

---

### 3.4 权限设置

#### Task 3.4.1 实现 PermissionSettingsFragment
**描述**: 整合权限控制  
**文件**:
- `ui/settings/PermissionSettingsFragment.kt`
- `res/layout/fragment_permission_settings.xml`

**内容**:
- 无障碍服务开关
- 悬浮窗权限
- 保活功能
- 高级权限引导

**依赖**: Task 3.1.1, 从 MainActivity 迁移代码  
**验证**: 权限控制正常

---

### 3.5 脚本服务器

#### Task 3.5.1 实现 ScriptServerFragment
**描述**: 整合脚本服务器控制  
**文件**:
- `ui/settings/ScriptServerFragment.kt`
- `res/layout/fragment_script_server.xml`

**内容**:
- 启动/停止服务器
- 显示地址
- 从 MainActivity 迁移逻辑

**依赖**: Task 3.1.1  
**验证**: 服务器控制正常

---

## Phase 4: 悬浮窗 (优先级: 中)

### 4.1 悬浮窗服务

#### Task 4.1.1 创建悬浮窗布局
**描述**: 创建悬浮窗 UI  
**文件**: `res/layout/layout_floating_window.xml`  
**内容**:
- 小型卡片布局
- 图标 + 标题 + 状态
- 圆角背景

**依赖**: 无  
**验证**: 布局预览正常

#### Task 4.1.2 实现 FloatingWindowService
**描述**: 实现悬浮窗服务  
**文件**: `ui/floating/FloatingWindowService.kt`  
**内容**:
- LifecycleService 基类
- WindowManager 添加视图
- 点击穿透配置
- 生命周期管理

**依赖**: Task 4.1.1  
**验证**: 悬浮窗可显示

---

### 4.2 状态同步

#### Task 4.2.1 实现 Agent 状态监听
**描述**: 悬浮窗同步 Agent 状态  
**文件**: `ui/floating/FloatingWindowService.kt` (更新)  
**内容**:
- 监听 AgentStatusManager.flow
- 更新 UI 显示
- 状态动画

**依赖**: Task 1.3.2, Task 4.1.2  
**验证**: 状态同步显示

---

### 4.3 动画效果

#### Task 4.3.1 实现悬浮窗动画
**描述**: 实现灵动岛风格动画  
**文件**: `ui/floating/FloatingWindowService.kt` (更新)  
**内容**:
- 显示/隐藏动画
- 状态切换动画
- 缩放效果

**依赖**: Task 4.2.1  
**验证**: 动画流畅

---

### 4.4 权限处理

#### Task 4.4.1 整合悬浮窗权限
**描述**: 更新 AndroidManifest 和权限引导  
**文件**: 
- `AndroidManifest.xml` (更新)
- `ui/settings/PermissionSettingsFragment.kt` (更新)

**依赖**: Task 3.4.1  
**验证**: 权限请求正常

---

## Phase 5: 优化完善 (优先级: 低)

### 5.1 横屏/平板适配

#### Task 5.1.1 创建横屏布局
**描述**: 适配横屏显示  
**文件**: `res/layout-land/activity_chat.xml`  
**内容**:
- 调整布局比例
- 输入区域优化

**依赖**: Phase 1 完成  
**验证**: 横屏显示正常

#### Task 5.1.2 创建平板布局
**描述**: 适配平板横屏双栏  
**文件**: `res/layout-sw600dp/activity_chat.xml`  
**内容**:
- 固定左侧栏
- 双栏布局

**依赖**: Phase 1 完成  
**验证**: 平板显示正常

---

### 5.2 主题优化

#### Task 5.2.1 完善颜色主题
**描述**: 完善 Material Design 3 颜色  
**文件**:
- `res/values/colors.xml`
- `res/values/themes.xml`
- `res/values/styles.xml`

**依赖**: 无  
**验证**: 主题一致

#### Task 5.2.2 实现暗色主题
**描述**: 支持暗色模式  
**文件**: `res/values-night/colors.xml`  
**依赖**: Task 5.2.1  
**验证**: 暗色主题正常

---

### 5.3 性能优化

#### Task 5.3.1 RecyclerView 优化
**描述**: 优化消息列表性能  
**内容**:
- 设置 setHasFixedSize
- 使用 DiffUtil
- 避免过度绘制

**依赖**: Phase 1 完成  
**验证**: 滚动流畅

#### Task 5.3.2 内存优化
**描述**: 优化内存使用  
**内容**:
- 图片加载优化
- 及时释放资源
- 避免内存泄漏

**依赖**: Phase 1-4 完成  
**验证**: 无内存泄漏

---

### 5.4 边缘情况处理

#### Task 5.4.1 错误处理完善
**描述**: 完善错误提示和恢复  
**内容**:
- 网络错误
- Agent 执行失败
- 数据库操作失败

**依赖**: Phase 1-4 完成  
**验证**: 错误提示友好

#### Task 5.4.2 空状态处理
**描述**: 添加空状态视图  
**文件**: 空状态布局  
**内容**:
- 无会话提示
- 无消息提示

**依赖**: Phase 1 完成  
**验证**: 空状态显示正常

---

### 5.5 迁移 MainActivity

#### Task 5.5.1 迁移到新 UI
**描述**: 将 MainActivity 功能迁移到新结构  
**内容**:
- 删除旧的 MainActivity
- 更新 AndroidManifest
- 迁移初始化逻辑

**依赖**: Phase 1-4 完成  
**验证**: 应用启动正常

#### Task 5.5.2 清理旧代码
**描述**: 删除不再使用的旧 UI 代码  
**依赖**: Task 5.5.1  
**验证**: 编译通过，无警告

---

## 任务执行建议

### 开发顺序

1. **严格按 Phase 顺序执行**，每个 Phase 内的任务可并行
2. **Phase 1 完成后进行集成测试**，确保基础功能正常
3. **Phase 2 是核心**，需要与 Agent 充分集成测试
4. **Phase 3-4 可并行开发**
5. **Phase 5 在所有功能完成后进行**

### 每日工作建议

**Day 1-2**: Phase 1 基础框架
- 完成目录结构、依赖配置
- 实现 ChatActivity 骨架
- 实现消息列表显示

**Day 3-4**: Phase 2 Agent 集成
- 连接 PhoneClawAgent
- 实现消息发送/接收
- 实现工具调用显示

**Day 5-6**: Phase 2 继续 + Phase 3 设置页面
- 完成会话管理
- 开始设置页面开发

**Day 7-8**: Phase 3-4 设置页面 + 悬浮窗
- 完成设置页面所有功能
- 实现悬浮窗

**Day 9-10**: Phase 5 优化完善
- 适配、主题、性能优化
- 清理旧代码
- 全面测试

### 验证检查点

- [ ] Phase 1 完成: 可打开应用，看到聊天界面，显示模拟消息
- [ ] Phase 2 完成: 可发送消息，收到 Agent 响应，看到工具调用过程
- [ ] Phase 3 完成: 所有设置页面功能正常
- [ ] Phase 4 完成: 悬浮窗同步显示 Agent 状态
- [ ] Phase 5 完成: 各设备适配正常，无崩溃，性能良好

---

## 风险和注意事项

### 技术风险

1. **Agent 事件流**: 当前 PhoneClawAgent 可能没有暴露细粒度的工具调用事件，需要评估是否需要修改 Agent 代码
2. **状态同步**: AgentStatusManager 需要正确处理多线程访问
3. **数据库升级**: 如需修改表结构，注意版本迁移

### 兼容性注意

1. **API 33+**: 项目最低 API 33，无需考虑低版本兼容
2. **权限变化**: Android 14+ 权限模型可能有变化
3. **无障碍服务**: 不同厂商可能有不同限制

### 代码规范

1. **遵循现有代码风格**: 保持与现有 Java/Kotlin 代码一致
2. **模块化**: 每个类职责单一，避免过度耦合
3. **注释**: 关键逻辑添加注释
4. **无注释要求**: 根据用户要求，不需要添加注释
