## ADDED Requirements

### Requirement: 提供商指示自动获取能力
系统应当允许提供商指示是否支持自动获取模型列表。

#### Scenario: OpenAI 提供商支持自动获取
- **WHEN** 在 OpenAI 提供商上检查 supportAutoFetchModels() 时
- **THEN** 它应当返回 true

### Requirement: 通过 HTTP 获取模型列表
系统应当从提供商的模型 API 端点获取可用模型。

#### Scenario: 从 OpenAI 兼容 API 获取模型
- **WHEN** 使用有效的 OpenAI 提供商配置调用 fetchModelList 时
- **THEN** 它应当从 /v1/models 端点返回模型标识符列表

#### Scenario: 处理认证失败
- **WHEN** 使用无效 API key 获取模型时
- **THEN** 它应当抛出包含错误详情的适当异常

#### Scenario: 处理网络失败
- **WHEN** 因网络问题导致模型获取失败时
- **THEN** 它应当抛出包含网络错误详情的异常

### Requirement: UI 中的自动获取对话框
系统应当在提供商支持时显示对话框询问用户是否自动获取模型。

#### Scenario: 步骤2后显示自动获取对话框
- **WHEN** 用户完成支持自动获取的提供商配置（步骤2）时
- **THEN** 应当出现对话框询问"当前提供商支持自动获取模型列表，是否自动获取"

#### Scenario: 用户选择自动获取
- **WHEN** 用户在自动获取对话框中点击"获取"时
- **THEN** 系统应当调用提供商的获取逻辑
- **AND** 带着检测到的模型进入步骤3

#### Scenario: 用户跳过自动获取
- **WHEN** 用户在自动获取对话框中点击"跳过"时
- **THEN** 系统应当不预填充模型直接进入步骤3

#### Scenario: 自动获取错误处理
- **WHEN** 自动获取失败时
- **THEN** 应当显示错误消息
- **AND** 用户仍应能够手动进入步骤3

### Requirement: 检测到的模型在步骤3中预填充
系统应当将检测到的模型标识符传递给模型创建步骤。

#### Scenario: 从自动获取预填充模型列表
- **WHEN** 自动获取成功返回模型时
- **THEN** 模型创建活动应当接收模型标识符列表
- **AND** 允许用户选择要添加的模型

### Requirement: ProviderConfigFragment 接口扩展
系统应当扩展 ProviderConfigFragment 接口以支持自动获取能力检测。

#### Scenario: 接口声明 supportAutoFetchModels 方法
- **WHEN** 实现 ProviderConfigFragment 时
- **THEN** 实现应当提供返回 Boolean 的 supportAutoFetchModels() 方法
