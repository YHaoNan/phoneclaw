## 1. Domain 层 - ModelProvider 抽象

- [ ] 1.1 在 `llm/domain/objects/ModelProviderType.kt` 中创建 `ModelProviderType` 枚举，包含 OPENAI 值
- [ ] 1.2 将 `llm/domain/objects/ModelProvider.kt` 中的 `ModelProvider` 从数据类重构为 sealed class
- [ ] 1.3 实现 `ModelProvider.OpenAI` 数据类，包含所有必要的配置字段
- [ ] 1.4 在 ModelProvider sealed class 中添加 `supportAutoFetchModels()` 抽象方法
- [ ] 1.5 在 ModelProvider sealed class 中添加 `toJsonConfig()` 抽象方法
- [ ] 1.6 在 `llm/domain/ModelProviderFactory.kt` 中创建 `ModelProviderFactory` 对象用于实体到领域对象的转换
- [ ] 1.7 增强 `Model` 数据类，确认 providerId 和 hasVisualCapability 字段存在

## 2. Data 层 - 实体转换

- [ ] 2.1 更新 `ModelProviderEntity.toDomain()` 扩展函数使用 ModelProviderFactory
- [ ] 2.2 在 ModelProviderFactory 中添加 OpenAI 配置的 JSON 解析逻辑
- [ ] 2.3 确保与现有 JSON 配置格式的向后兼容

## 3. Integration 层 - OpenAI 提供商

- [ ] 3.1 在 `llm/integration/ModelProviderIntegration.kt` 中创建 `ModelProviderIntegration` 接口
- [ ] 3.2 重构 `OpenAIProviderIntegration` 实现 ModelProviderIntegration 接口
- [ ] 3.3 更新 `createChatLanguageModel` 方法接受 ModelProvider.OpenAI 参数
- [ ] 3.4 使用 OkHttp 实现 `fetchModelList()` 方法
- [ ] 3.5 添加认证和网络失败的错误处理
- [ ] 3.6 配置 HTTP 客户端使用自定义超时设置

## 4. Domain 层 - Facade 更新

- [ ] 4.1 更新 `ModelProviderFacade` 以支持 sealed class ModelProvider
- [ ] 4.2 添加按类型获取提供商的方法
- [ ] 4.3 确保现有 CRUD 操作正常工作

## 5. UI 层 - ProviderConfigFragment 接口

- [ ] 5.1 在 `ProviderConfigFragment` 接口中添加 `supportAutoFetchModels()` 方法
- [ ] 5.2 在 ProviderConfigFragment 接口中添加 `detectModels()` 方法（如需要则重构现有签名）

## 6. UI 层 - OpenAIConfigFragment 增强

- [ ] 6.1 添加 enableResponseApi 的 UI 输入字段（复选框/开关）
- [ ] 6.2 添加 responseUrl 的 UI 输入字段
- [ ] 6.3 为 apiKey 输入字段添加密码可见性切换
- [ ] 6.4 更新 `loadConfig()` 处理新配置字段
- [ ] 6.5 更新 `getConfigJson()` 序列化新配置字段
- [ ] 6.6 实现 `supportAutoFetchModels()` 返回 true

## 7. UI 层 - 自动获取对话框流程

- [ ] 7.1 更新 `ProviderConfigActivity` 在显示对话框前检查 supportAutoFetchModels()
- [ ] 7.2 确保步骤2完成后显示自动获取对话框
- [ ] 7.3 处理自动获取成功并将模型传递给 ModelEditActivity
- [ ] 7.4 处理自动获取失败并显示错误消息
- [ ] 7.5 允许用户在自动获取失败时仍可进入步骤3

## 8. UI 层 - ModelEditActivity 增强

- [ ] 8.1 支持接收自动获取预填充的模型列表
- [ ] 8.2 显示检测到的模型供用户选择

## 9. 单元测试

- [ ] 9.1 创建 ModelProviderFactory 转换 OpenAI 实体的测试
- [ ] 9.2 创建 ModelProvider.OpenAI toJsonConfig() 序列化的测试
- [ ] 9.3 创建 ModelProvider.OpenAI supportAutoFetchModels() 的测试
- [ ] 9.4 创建 OpenAIProviderIntegration fetchModelList() 的测试
- [ ] 9.5 创建使用模拟仓库的 ModelProviderFacade 测试

## 10. 验证

- [ ] 10.1 验证现有提供商列表正确显示
- [ ] 10.2 验证现有提供商编辑不会丢失配置
- [ ] 10.3 验证新提供商创建的步骤流程
- [ ] 10.4 验证自动获取模型功能正常工作
- [ ] 10.5 验证从检测到的模型创建模型正常工作
