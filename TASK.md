实现ui包中的ui界面以及整合所有功能，提供服务

# UI模块设计

模仿千问、deepseek等设计

## 左侧侧滑栏
- 新建对话按钮
- 对话列表，最近一周，更早两个栏，用于展示和agent的所有对话历史
- 设置

## 主对话
进入应用后直接看到的页面，占据所有空间

- 顶栏是当前对话的标题总结（来自于ai自动生成，一会会提到）
- 当前agent的对话
- 可以发送消息
- 可以选择图片
- 可以选择模型
- 可以切换语音输入和键盘输入（语音输入功能暂不实现）
- 可以停止agent当前的任务
- 消息条目设计清晰明确
  - Agent思考中
  - 工具调用中（展开可查看调用参数、返回）
  - 技能调用中
  - 工具调用、技能调用的过程中有明确的加载，成功，失败样式

## 设置页面
### 模型设置

领域对象：
- ModelProvider：模型提供商，提供一批Model
- Model：模型，id、displayName、hasVisualCapability

整合提供商的增删改查功能，目前只实现[openai](app/src/main/java/top/yudoge/phoneclaw/llm/provider/openai)提供商

模型提供商的基础字段：
val id: Long,
val name: String,
val apiType: APIType,
val models: List<Model>,
val modelProviderConfig: String,

选择不同提供商后的选项应该是动态的，每个提供商都不同， 其存储在modelProviderConfig中，如OpenAI的配置是OpenAIModelConfig，并用OpenAIModelInitializer解析

#### 交互
1. 填写基本信息（id, name, apiType）
2. 填写提供商私有信息
3. 添加模型

#### 自动识别模型
一些提供商具有返回所有模型和其能力的api接口，如OpenAI，这类模型，可以做一个优化，必要字段填完会弹出一个按钮自动识别模型及其能力


### skill设置
整合skill的增删改查功能，详见[skills](app/src/main/java/top/yudoge/phoneclaw/llm/skills)

### 权限设置
- 无障碍权限是否开启
- 无障碍保活
- 悬浮窗权限

### Lua脚本服务器
整合[ScriptServer.java](app/src/main/java/top/yudoge/phoneclaw/scripts/ScriptServer.java)功能

### 说明
> 设置页面的部分功能是完整的增删改查，此时需要具有完整的二级页面，对于简单的设置项，仅需switch开关等控件即可

## 悬浮窗
因为phoneclaw agent经常会操作手机，导致界面进入到其它应用，phoneclaw转入后台，所以在转入后台时希望提供悬浮窗来监控当前agent的执行情况

1. 思考中：展示agent正在思考
2. 工具调用：展示工具调用的名称，状态（调用中，成功，失败）
3. 技能调用：展示技能调用的名称，状态

悬浮窗点击后无任何效果

悬浮窗的设计应尽量小，不占用屏幕空间，且不对accessibility展示，不拦截点击事件，所有点击事件可以穿透到下层的其它应用（如早期护眼宝）

悬浮窗状态迁移时的动画应该丝滑美观，如灵动岛

# 限制&要求
1. 必须使用Material Design 3设计风格
2. 必须清晰阅读已有代码，深刻理解后再开始工作
3. 现在已有早期的ui实现，你需要参考它，并完全重写，而不是在其上修改
4. 设计新的Activity，不要用MainActivity这种意义不明的名称
5. 代码设计应该遵循模块化规则，不同的内容交给不同的类处理，如左侧Drawer和聊天页、设置页等等
6. 持久层状态变化，应考虑应用层的刷新
7. 如果开发过程中发现现有的api设计得不好，可以优化，请提出优化想法，并询问我的意见
8. 应该考虑在平板上的横屏兼容性