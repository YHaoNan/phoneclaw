## Why

当前模型提供商管理实现较为简单，将配置直接存储为JSON字符串，缺乏对不同提供商类型的抽象支持。随着项目发展需要支持更多提供商（OpenAI、Anthropic、DashScope、智谱、Minimax、VLLM、Ollama等），需要对现有架构进行重构，建立清晰的抽象层，使新增提供商更加标准化和可扩展。

## What Changes

### Domain Layer 重构
- 将 `ModelProvider` 从简单数据类重构为抽象基类，提供统一的提供商接口
- 新增 `ModelProviderType` 枚举定义支持的提供商类型
- 新增 `OpenAIModelProvider` 实现，包含 OpenAI 特定的配置解析和模型列表获取逻辑
- 重构 `Model` 对象，增强模型能力定义

### Integration Layer 增强
- 重构 `OpenAIProviderIntegration`，基于新的 `ModelProvider` 抽象
- 新增 `ModelProviderIntegration` 接口，定义提供商集成规范
- 实现基于 langchain4j 的模型创建逻辑

### UI Layer 优化
- 保持现有 MVP 架构，适配新的 Domain 层接口
- 优化 `OpenAIConfigFragment` 配置项，支持需求文档中定义的完整参数
- **BREAKING**: `ProviderConfigFragment` 接口签名变更，新增 `supportAutoFetchModels()` 方法

### Data Layer 兼容
- 数据库结构保持兼容，`modelProviderConfig` 字段继续存储 JSON
- `ModelProviderEntity` 新增 `toDomain()` 方法支持多态转换

## Capabilities

### New Capabilities
- `model-provider-abstraction`: 模型提供商抽象层设计，定义 ModelProvider 基类和类型枚举
- `openai-provider-impl`: OpenAI 提供商完整实现，包含配置管理和模型列表自动获取
- `provider-auto-fetch-models`: 提供商模型自动发现能力，支持从 API 获取可用模型列表

### Modified Capabilities

无（此为新建模块的初始实现）

## Impact

### 受影响的代码模块
- `llm/domain/objects/ModelProvider.kt` - 重构为抽象类
- `llm/domain/objects/Model.kt` - 增强字段定义
- `llm/domain/ModelProviderFacade.kt` - 适配新接口
- `llm/integration/OpenAIProviderIntegration.kt` - 基于新抽象重构
- `llm/data/entity/ModelProviderEntity.kt` - 增强转换逻辑
- `ui/settings/model/OpenAIConfigFragment.kt` - 扩展配置项
- `ui/settings/model/ProviderConfigFragment.kt` - 接口扩展

### 外部依赖
- langchain4j（已引入）：用于模型创建和 API 调用
- OkHttp（已引入）：用于模型列表 HTTP 请求

### 测试影响
- 需要新增单元测试覆盖新的抽象层
- 需要 Mock Repository 进行 Facade 层测试
