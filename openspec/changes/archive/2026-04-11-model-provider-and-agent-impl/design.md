## Context

PhoneClaw是一个Android自动化应用，通过AI Agent驱动手机操作。当前需要实现两个核心模块：
1. **模型提供商管理**: 支持多种AI提供商配置和管理
2. **Agent执行器**: 整合LangChain4j驱动对话和自动化

**当前状态:**
- ModelProvider是简单数据类，无法表达提供商差异
- PhoneClawAgentExecutor是空壳
- OpenAIProviderIntegration和AgentIntegration是冗余层

**约束:**
- 遵循项目分层架构（UI→Domain→Data→Integration）
- 使用MVP模式开发UI层
- Domain层需支持单元测试（通过Mock Data层）
- **上层代码不直接接触LangChain4j对象**
- API密钥等敏感字段需支持加密存储

## Goals / Non-Goals

**Goals:**
- 实现可扩展的ModelProvider抽象体系
- 支持OpenAI提供商的完整配置（含Response API）
- 实现可运行的PhoneClawAgentExecutor
- 复用并修复现有UI层实现
- 支持提供商模型的自动获取（OpenAI）

**Non-Goals:**
- 暂不支持Anthropic、DashScope等其他提供商
- 暂不实现API密钥加密存储（使用明文，后续迭代）
- 暂不实现模型能力自动检测

## Decisions

### D1: ModelProvider架构设计

**决策**: ModelProvider是抽象类，定义在domain层；具体实现类是Integration层对象

```
Domain层:
  ModelProvider (abstract class)
  ├── id: Long
  ├── name: String  
  ├── providerType: ProviderType
  ├── abstract fun supportAutoFetchModelList(): Boolean
  ├── abstract fun fetchModelList(): List<Model>
  ├── abstract fun createChatModel(modelId: String): ChatLanguageModel  // 返回类型对上层透明
  └── abstract fun parseToConfig(): String

Integration层:
  OpenAIModelProvider : ModelProvider
  ├── config: OpenAIModelConfig
  └── 实现所有抽象方法（内部使用LangChain4j）
```

**关键点:**
- `createChatModel()` 返回 `ChatLanguageModel`，但上层代码不直接调用其方法
- Agent执行器内部使用ChatLanguageModel，但通过回调将结果传递给上层

**备选方案:**
- 接口+策略模式: 不选，因为需要在抽象类中共享id/name等公共字段
- 保持数据类+单独Integration: 不选，导致职责分散，代码冗余

### D2: Provider配置存储格式

**决策**: 使用JSON字符串存储在`model_provider_config`字段

```json
{
  "baseUrl": "https://api.openai.com",
  "apiKey": "sk-xxx",
  "responseApiEnabled": false,
  "responseUrl": "/v1/responses",
  "connectTimeoutMillis": 5000,
  "requestTimeoutMillis": 60000
}
```

**理由**: 灵活扩展，无需修改数据库schema

### D3: Tool回调设计

**决策**: 使用domain层数据类封装Tool调用信息

```kotlin
// domain/objects/ToolCallInfo.kt
data class ToolCallInfo(
    val toolName: String,
    val arguments: String  // JSON字符串
)

// domain/objects/ToolCallResult.kt  
data class ToolCallResult(
    val toolName: String,
    val result: String?,
    val success: Boolean,
    val error: String? = null
)

// callback/AgentRunCallBack.kt
interface AgentRunCallBack {
    fun onAgentStart()
    fun onAgentError(e: Throwable)
    fun onAgentEnd()
    fun onReasoningStart()
    fun onReasoningEnd()
    fun onTextDelta(text: String)
    fun onTextDeltaComplete(text: String)
    fun onToolCallStart(info: ToolCallInfo)
    fun onToolCallEnd(result: ToolCallResult)
}
```

**理由**: 
- 数据类定义在domain层，上层可直接使用
- 避免LangChain4j的ToolExecutionRequest泄露到上层
- 与现有UI的MessageItem.ToolCallMessage结构对应

### D4: Agent执行器状态管理

**决策**: PhoneClawAgentExecutor持有状态，每次创建新实例

```kotlin
class PhoneClawAgentExecutor(
    private val session: Session,
    private val skillFacade: SkillFacade,
    private val modelProviderFacade: ModelProviderFacade,
    private val emuFacade: EmuFacade
) {
    private val chatMemory: ChatMemory  // 基于session初始化
    private val tools: List<Any>  // LangChain4j Tool对象，内部使用
    
    // 内部方法，不暴露LangChain对象
    private fun buildTools(): List<Any> { ... }
    private fun createChatModel(provider: ModelProvider, modelId: String): ChatLanguageModel { ... }
}
```

**理由**: 简化状态管理，避免并发问题。每次切换session创建新实例。

### D5: UI层复用

**决策**: 复用现有UI实现，修复逻辑差异

**现有UI文件:**
- `ProviderListActivity` - 提供商列表（卡片展开/收起）
- `ProviderEditActivity` - 步骤1：基础信息
- `ProviderConfigActivity` - 步骤2容器
- `OpenAIConfigFragment` - OpenAI配置表单
- `ModelEditActivity` - 步骤3：模型添加

**需修复:**
- Provider数据类改为抽象类后的兼容性
- 新增Response API配置字段
- 回调数据类更新后的适配

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| 编辑提供商时配置丢失 | 使用ViewModel保存临时状态，确认后才写入数据库 |
| API密钥明文存储风险 | 在文档中标注安全警告，后续迭代加入加密 |
| 模型自动获取可能失败 | 提供跳过选项，支持手动添加模型 |
| LangChain4j API变更 | 封装在ModelProvider实现类中，隔离变化 |
