## Why

PhoneClaw需要一个完整的模型提供商管理系统来支持多种AI模型提供商（OpenAI、Anthropic、DashScope等），同时需要一个可运行的Agent执行器来驱动AI对话和自动化操作。当前代码中这两个核心模块仅有骨架实现，无法实际使用。

## What Changes

- **ModelProvider抽象化**: 将 `ModelProvider` 从简单数据类重构为抽象类，具体实现类本身就是Integration层对象
- **删除冗余Integration文件**: 删除 `OpenAIProviderIntegration.kt` 和 `AgentIntegration.kt`，职责合并到 ModelProvider
- **OpenAI提供商完整实现**: 实现 `OpenAIModelProvider`，包含完整的配置解析、模型列表获取、LangChain4j ChatModel创建
- **Response API支持**: 新增 `responseApiEnabled` 和 `responseUrl` 配置字段
- **ModelProviderFacade增强**: 新增获取提供商-模型嵌套结构的方法
- **PhoneClawAgentExecutor完整实现**: 实现Session记忆、Skill/Tool注入、Agent执行、回调通知
- **Tool回调数据类**: 新增 `ToolCallInfo` 和 `ToolCallResult` 用于回调传参
- **UI层复用修复**: 复用现有UI实现，修复逻辑差异

## Capabilities

### New Capabilities
- `model-provider-management`: 模型提供商的CRUD、配置管理、模型自动获取
- `agent-execution`: 基于LangChain4j的Agent执行器，支持Session记忆、Skill/Tool集成

### Modified Capabilities
- 无（无现有specs）

## Impact

**删除文件:**
- `llm/integration/OpenAIProviderIntegration.kt` - 职责合并到 OpenAIModelProvider
- `llm/integration/AgentIntegration.kt` - 职责合并到 PhoneClawAgentExecutor

**修改文件:**
- `llm/domain/objects/ModelProvider.kt` - 重构为抽象类
- `llm/domain/ModelProviderFacade.kt` - 增强方法
- `llm/domain/PhoneClawAgentExecutor.kt` - 完整实现
- `llm/callback/AgentRunCallBack.kt` - 更新回调方法签名
- `llm/integration/openai/OpenAIModelConfig.kt` - 新增字段
- `ui/settings/model/*` - 修复逻辑差异

**新增文件:**
- `llm/domain/objects/ToolCallInfo.kt` - Tool调用信息数据类
- `llm/domain/objects/ToolCallResult.kt` - Tool调用结果数据类
- `llm/integration/openai/OpenAIModelProvider.kt` - OpenAI提供商实现

**依赖:**
- LangChain4J (已集成)
- AndroidX Lifecycle/ViewModel (已集成)

**设计原则:**
- 上层代码（UI、Domain）不直接接触LangChain4j对象
- LangChain4j对象仅在Integration层创建和使用
