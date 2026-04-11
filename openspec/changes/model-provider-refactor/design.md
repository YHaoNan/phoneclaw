## Context

PhoneClaw 是一个 Android 应用，使用分层架构（UI Layer → Domain Layer → Data Layer → Integration Layer）。当前 `llm` 模块的模型提供商管理使用简单的数据类存储配置，缺乏对多种提供商类型的抽象支持。

**当前状态：**
- `ModelProvider` 是简单 data class，config 字段存储 JSON 字符串
- `OpenAIProviderIntegration` 直接解析 JSON 创建配置对象
- 缺乏统一接口定义提供商能力（如自动获取模型列表）

**约束：**
- 使用 langchain4j 作为 LLM 框架
- 数据库使用 SQLite，需保持向后兼容
- UI 层使用 MVP 模式
- Kotlin 开发语言

## Goals / Non-Goals

**Goals:**
1. 建立清晰的 `ModelProvider` 抽象层，支持多种提供商类型扩展
2. 实现 OpenAI 提供商的完整功能，包括模型列表自动获取
3. 保持数据库和 API 层的向后兼容
4. 支持单元测试，可 Mock Repository

**Non-Goals:**
1. 本次不实现 Anthropic、DashScope 等其他提供商（仅 OpenAI）
2. 不修改现有数据库 Schema
3. 不涉及聊天功能的修改

## Decisions

### D1: ModelProvider 抽象设计

**选择方案：** 使用 sealed class 而非 abstract class

**理由：**
- Sealed class 提供有限的类型层次，编译器可穷举检查
- 配合 `when` 表达式确保新增提供商类型时必须处理所有分支
- 符合 Kotlin 最佳实践，便于模式匹配

**替代方案：**
- Interface + 实现类：缺乏类型安全，新增类型时可能遗漏处理
- Abstract class：可行，但 sealed class 更符合 Kotlin 惯用写法

### D2: 配置存储策略

**选择方案：** 保持 JSON 字符串存储，在 Domain 层做序列化/反序列化

**理由：**
- 避免数据库迁移复杂度
- 不同提供商配置结构不同，JSON 灵活性高
- 已有代码使用此方式，风险低

**替代方案：**
- 拆分为多张表：增加 JOIN 复杂度，新增提供商需修改 Schema
- 使用 Room Entity：当前使用原生 SQLite，引入 Room 成本高

### D3: 模型列表获取实现

**选择方案：** 在 Integration 层使用 OkHttp 直接调用 API

**理由：**
- langchain4j 的 OpenAI 客户端主要用于聊天，模型列表需额外处理
- OkHttp 已引入，轻量级
- 可复用现有 HTTP 配置（超时、拦截器等）

**替代方案：**
- 使用 langchain4j 的 OpenAiClient：需额外依赖，功能不完整
- Retrofit：增加依赖，当前无需完整的 REST 客户端

### D4: UI 配置页面设计

**选择方案：** 保持现有 Fragment + Contract 模式，扩展接口方法

**理由：**
- MVP 架构已建立，扩展成本低
- Fragment 可复用于不同提供商类型
- Contract 接口便于单元测试

**替代方案：**
- Compose 重写：学习成本高，现有代码迁移量大
- ViewModel + LiveData：与现有 Presenter 模式不一致

## Risks / Trade-offs

### R1: Sealed Class 扩展性限制
- **风险：** Sealed class 子类必须同一文件/包，跨模块扩展受限
- **缓解：** 所有 Provider 实现放在 `llm/domain/objects/` 包内，当前规模可接受

### R2: JSON 配置类型安全
- **风险：** JSON 字段变更可能导致反序列化失败
- **缓解：** 使用默认值和 optString/optLong 等安全方法，版本迁移时做兼容处理

### R3: 编辑时数据丢失
- **风险：** 需求文档强调"修改基础信息不能影响已有配置"
- **缓解：** ProviderEditActivity 仅修改 name/apiType，配置信息从数据库加载后原样保留

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                      UI Layer (MVP)                          │
│  ProviderListActivity → ProviderListPresenter → Contract     │
│  ProviderEditActivity → Step 1: Basic Info                   │
│  ProviderConfigActivity → OpenAIConfigFragment (Step 2)     │
│  ModelEditActivity → Step 3: Add Models                      │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     Domain Layer                             │
│  ModelProviderFacade ───────────────────────────────────────│
│       │                                                      │
│       ├── objects/                                           │
│       │    ├── ModelProvider (sealed class)                  │
│       │    │    ├── OpenAI                                   │
│       │    │    └── ... (future providers)                   │
│       │    ├── Model                                         │
│       │    └── ModelProviderType (enum)                      │
│       │                                                      │
│       └── ModelProviderFactory (creates from entity)         │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      Data Layer                              │
│  ModelProviderRepository ← ModelProviderRepositoryImpl       │
│  ModelRepository ← ModelRepositoryImpl                       │
│       │                                                      │
│       └── entity/                                            │
│            ├── ModelProviderEntity (DB mapping)              │
│            └── ModelEntity                                   │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   Integration Layer                          │
│  ModelProviderIntegration (interface)                        │
│       └── OpenAIProviderIntegration                          │
│            ├── createChatLanguageModel() → LangChain4j       │
│            └── fetchModelList() → OkHttp                     │
└─────────────────────────────────────────────────────────────┘
```

## Class Design

### ModelProvider (sealed class)

```kotlin
sealed class ModelProvider {
    abstract val id: Long
    abstract val name: String
    abstract val type: ModelProviderType
    
    abstract fun supportAutoFetchModels(): Boolean
    abstract fun toJsonConfig(): String
    
    data class OpenAI(
        override val id: Long,
        override val name: String,
        val baseUrl: String,
        val apiKey: String,
        val chatCompletionUrl: String,
        val modelsUrl: String,
        val connectTimeoutSeconds: Int,
        val requestTimeoutSeconds: Int,
        val enableResponseApi: Boolean,
        val responseUrl: String?
    ) : ModelProvider() {
        override val type = ModelProviderType.OPENAI
        override fun supportAutoFetchModels() = true
    }
}
```

### ModelProviderType (enum)

```kotlin
enum class ModelProviderType(val displayName: String) {
    OPENAI("OpenAI")
}
```

### ModelProviderIntegration (interface)

```kotlin
interface ModelProviderIntegration<T : ModelProvider> {
    fun createChatLanguageModel(provider: T, model: Model): ChatLanguageModel
    fun fetchModelList(provider: T): List<ModelInfo>
    fun supportAutoFetch(): Boolean
}
```
