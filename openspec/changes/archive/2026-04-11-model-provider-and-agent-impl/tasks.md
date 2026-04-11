## 1. ModelProvider抽象层重构

- [x] 1.1 重构 `ModelProvider` 为抽象类，包含id、name、providerType公共字段和抽象方法
- [x] 1.2 更新 `OpenAIModelConfig` 数据类，新增responseApiEnabled、responseUrl字段
- [x] 1.3 创建 `OpenAIModelProvider` 实现类，实现所有抽象方法
- [x] 1.4 删除冗余文件 `OpenAIProviderIntegration.kt` 和 `AgentIntegration.kt`
- [x] 1.5 更新 `ModelProviderFacade`，新增getAllProvidersWithModels方法

## 2. Tool回调数据类

- [x] 2.1 创建 `ToolCallInfo` 数据类，包含toolName、arguments字段
- [x] 2.2 创建 `ToolCallResult` 数据类，包含toolName、result、success、error字段
- [x] 2.3 更新 `AgentRunCallBack` 接口，onToolCallStart接收ToolCallInfo，onToolCallEnd接收ToolCallResult

## 3. PhoneClawAgentExecutor实现

- [x] 3.1 实现构造函数，接收Session、SkillFacade、ModelProviderFacade、EmuFacade依赖
- [x] 3.2 实现工具初始化，创建PhoneEmulationTool和UseSkillTool实例
- [x] 3.3 实现基于Session初始化ChatMemory
- [x] 3.4 实现 `run()` 方法，整合ChatModel、Memory、Tools执行Agent
- [x] 3.5 实现回调机制，在关键节点调用AgentRunCallBack
- [x] 3.6 实现 `flushSkills()` 方法，重新加载技能列表
- [x] 3.7 实现 `flushModelProviders()` 方法，重新加载提供商列表
- [x] 3.8 实现消息自动持久化到Session

## 4. UI层修复

- [x] 4.1 修复 `ProviderListActivity` 和 `ProviderAdapter` 对抽象类ModelProvider的兼容性
- [x] 4.2 修复 `ProviderEditActivity` 对抽象类ModelProvider的兼容性
- [x] 4.3 更新 `OpenAIConfigFragment`，新增responseApiEnabled开关和responseUrl输入框
- [x] 4.4 更新 `ModelEditActivity` 的模型自动获取逻辑，适配新的OpenAIModelProvider

## 5. AppContainer集成

- [x] 5.1 更新AppContainer，提供创建ModelProvider的工厂方法
- [x] 5.2 更新AppContainer，提供PhoneClawAgentExecutor工厂方法

## 6. 单元测试

- [x] 6.1 创建 `FakeModelProviderRepository` 用于测试
- [x] 6.2 编写 `ModelProviderFacade` 单元测试
- [x] 6.3 编写 `OpenAIModelProvider` 单元测试
- [x] 6.4 编写 `PhoneClawAgentExecutor` 单元测试
