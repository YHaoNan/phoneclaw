## ADDED Requirements

### Requirement: ModelProvider sealed class 定义
系统应当定义一个 sealed class `ModelProvider` 作为所有模型提供商实现的基类。

#### Scenario: ModelProvider 具有必要属性
- **WHEN** 创建 ModelProvider 实例时
- **THEN** 它应当具有 id (Long)、name (String) 和 type (ModelProviderType) 属性

#### Scenario: ModelProvider 定义抽象方法
- **WHEN** 实现 ModelProvider 子类时
- **THEN** 实现应当提供 supportAutoFetchModels() 和 toJsonConfig() 方法

### Requirement: ModelProviderType 枚举
系统应当定义一个枚举 `ModelProviderType` 列出所有支持的提供商类型。

#### Scenario: ModelProviderType 包含 OPENAI
- **WHEN** 访问 ModelProviderType 值时
- **THEN** OPENAI 应当作为有效类型可用

### Requirement: ModelProvider.OpenAI 实现
系统应当提供一个数据类 `ModelProvider.OpenAI` 实现 OpenAI 提供商。

#### Scenario: OpenAI 提供商具有必要的配置字段
- **WHEN** 创建 OpenAI 提供商时
- **THEN** 它应当包含 baseUrl、apiKey、chatCompletionUrl、modelsUrl、connectTimeoutSeconds、requestTimeoutSeconds 字段

#### Scenario: OpenAI 提供商支持自动获取
- **WHEN** 在 OpenAI 提供商上调用 supportAutoFetchModels() 时
- **THEN** 它应当返回 true

#### Scenario: OpenAI 提供商序列化为 JSON
- **WHEN** 在 OpenAI 提供商上调用 toJsonConfig() 时
- **THEN** 它应当返回包含所有配置字段的有效 JSON 字符串

### Requirement: Model 实体增强
系统应当增强 `Model` 数据类以支持提供商关联和能力标志。

#### Scenario: Model 具有提供商关联
- **WHEN** 创建 Model 实例时
- **THEN** 它应当具有 providerId (Long) 链接到其父提供商

#### Scenario: Model 具有视觉能力标志
- **WHEN** 创建 Model 实例时
- **THEN** 它应当具有 hasVisualCapability (Boolean) 标志表示是否支持视觉

### Requirement: ModelProviderFactory 用于实体转换
系统应当提供一个工厂对象用于将 ModelProviderEntity 转换为适当的 ModelProvider 子类。

#### Scenario: 工厂创建正确的提供商类型
- **WHEN** 转换 apiType 为 "OPENAI" 的 ModelProviderEntity 时
- **THEN** 工厂应当返回 ModelProvider.OpenAI 实例

#### Scenario: 工厂保留提供商 id 和 name
- **WHEN** 转换 ModelProviderEntity 时
- **THEN** 结果 ModelProvider 应当具有与实体相同的 id 和 name
