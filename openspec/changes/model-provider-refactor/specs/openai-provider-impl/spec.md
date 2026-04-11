## ADDED Requirements

### Requirement: OpenAI 提供商配置参数
系统应当支持以下 OpenAI 特定的配置参数：

| 参数 | 必填 | 默认值 |
|-----------|----------|---------|
| apiKey | 否 | - |
| baseUrl | 是 | https://api.openai.com |
| chatCompletionUrl | 是 | /v1/chat/completions |
| modelsUrl | 是 | /v1/models |
| enableResponseApi | 是 | false |
| responseUrl | 是 | /v1/responses |
| connectTimeoutSeconds | 是 | 5 |
| requestTimeoutSeconds | 是 | 60 |

#### Scenario: 可选字段应用默认值
- **WHEN** 创建 OpenAI 提供商时未指定可选字段
- **THEN** 系统应当应用指定的默认值

#### Scenario: API key 可选但 API 调用时必需
- **WHEN** 创建没有 apiKey 的 OpenAI 提供商时
- **THEN** 提供商应当成功创建
- **AND** API 调用应当因认证错误而失败

### Requirement: OpenAI 提供商集成创建 ChatLanguageModel
系统应当提供 `OpenAIProviderIntegration` 用于创建 langchain4j ChatLanguageModel 实例。

#### Scenario: 集成创建有效的 ChatLanguageModel
- **WHEN** 使用有效的 OpenAI 提供商和模型调用 createChatLanguageModel 时
- **THEN** 它应当返回配置好的 ChatLanguageModel 实例

#### Scenario: 集成应用超时设置
- **WHEN** 使用自定义超时值创建 ChatLanguageModel 时
- **THEN** HTTP 客户端应当使用指定的连接和请求超时

### Requirement: OpenAI 提供商 UI 配置片段
系统应当提供 `OpenAIConfigFragment` 用于在 UI 中配置 OpenAI 特定设置。

#### Scenario: 片段显示所有配置字段
- **WHEN** 显示 OpenAIConfigFragment 时
- **THEN** 它应当显示 baseUrl、apiKey、chatCompletionUrl、modelsUrl、responseUrl、enableResponseApi 和超时设置的输入字段

#### Scenario: 片段加载现有配置
- **WHEN** 使用现有 JSON 配置调用 loadConfig 时
- **THEN** 所有输入字段应当填充保存的值

#### Scenario: 片段验证必填字段
- **WHEN** baseUrl 为空时调用 onNextStep
- **THEN** 它应当返回 false 并显示验证错误

#### Scenario: 片段支持 apiKey 的密码可见性切换
- **WHEN** 显示 apiKey 输入字段时
- **THEN** 它应当使用密码样式输入并支持可见性切换

### Requirement: OpenAI 配置序列化
系统应当将 OpenAI 配置序列化为与现有存储兼容的 JSON 格式。

#### Scenario: 配置序列化为 JSON
- **WHEN** 在 OpenAIConfigFragment 上调用 getConfigJson 时
- **THEN** 它应当返回包含所有配置字段的 JSON 字符串

#### Scenario: 配置从 JSON 反序列化
- **WHEN** 使用有效 JSON 调用 loadConfig 时
- **THEN** 所有字段应当从 JSON 正确填充
