## ADDED Requirements

### Requirement: Agent执行器初始化
PhoneClawAgentExecutor SHALL 在创建时基于Session初始化聊天记忆。

#### Scenario: 基于Session初始化
- **WHEN** 创建PhoneClawAgentExecutor并传入Session
- **THEN** 系统从SessionFacade加载该Session的历史消息
- **AND** 将历史消息转换为ChatMemory

#### Scenario: 工具初始化
- **WHEN** 创建PhoneClawAgentExecutor
- **THEN** 系统自动创建PhoneEmulationTool实例
- **AND** 自动创建UseSkillTool实例
- **AND** 工具列表存储在内部，不暴露给上层

### Requirement: 执行Agent对话
PhoneClawAgentExecutor SHALL 支持执行Agent对话并返回结果。

#### Scenario: 执行成功
- **WHEN** 调用`run(prompt, model, callback)`
- **THEN** 系统根据model获取对应的ModelProvider
- **AND** 调用provider.createChatModel()创建ChatLanguageModel
- **AND** 注入Memory和Tools
- **AND** 调用LangChain4j Agent执行
- **AND** 通过callback返回结果

#### Scenario: 执行出错
- **WHEN** Agent执行过程中发生错误
- **THEN** 系统调用`callback.onAgentError(e)`
- **AND** 不进行自动重试

### Requirement: Tool回调数据类
系统 SHALL 在domain层定义Tool调用的数据类，用于回调传参。

#### Scenario: ToolCallInfo数据类
- **WHEN** 定义ToolCallInfo
- **THEN** 包含toolName: String
- **AND** 包含arguments: String（JSON格式）

#### Scenario: ToolCallResult数据类
- **WHEN** 定义ToolCallResult
- **THEN** 包含toolName: String
- **AND** 包含result: String?（执行结果）
- **AND** 包含success: Boolean
- **AND** 包含error: String?（错误信息）

### Requirement: Agent执行回调
AgentRunCallBack SHALL 提供完整的执行生命周期回调，使用domain层数据类。

#### Scenario: 完整执行流程
- **WHEN** Agent开始执行
- **THEN** 依次调用:
  1. `onAgentStart()`
  2. `onReasoningStart()`
  3. `onTextDelta()` (多次)
  4. `onTextDeltaComplete()`
  5. `onReasoningEnd()`
  6. `onAgentEnd()`

#### Scenario: 工具调用开始回调
- **WHEN** Agent开始调用Tool
- **THEN** 调用`onToolCallStart(info: ToolCallInfo)`
- **AND** info包含toolName和arguments

#### Scenario: 工具调用结束回调
- **WHEN** Tool执行完成
- **THEN** 调用`onToolCallEnd(result: ToolCallResult)`
- **AND** result包含toolName、result、success、error

#### Scenario: 工具执行成功
- **WHEN** Tool执行成功
- **THEN** ToolCallResult.success为true
- **AND** ToolCallResult.result包含返回值

#### Scenario: 工具执行失败
- **WHEN** Tool执行失败
- **THEN** ToolCallResult.success为false
- **AND** ToolCallResult.error包含错误信息

### Requirement: 技能刷新
PhoneClawAgentExecutor SHALL 支持动态刷新技能列表。

#### Scenario: 刷新技能
- **WHEN** 调用`flushSkills()`
- **THEN** 系统从SkillFacade重新加载所有技能
- **AND** 更新内部Tool列表

### Requirement: 模型提供商刷新
PhoneClawAgentExecutor SHALL 支持动态刷新模型提供商列表。

#### Scenario: 刷新提供商
- **WHEN** 调用`flushModelProviders()`
- **THEN** 系统从ModelProviderFacade重新加载提供商和模型列表

### Requirement: 会话消息持久化
Agent执行过程中的消息 SHALL 自动持久化到Session。

#### Scenario: 用户消息持久化
- **WHEN** 用户发送prompt
- **THEN** 系统创建USER角色Message并保存

#### Scenario: Agent回复持久化
- **WHEN** Agent完成回复
- **THEN** 系统创建AGENT角色Message并保存

### Requirement: LangChain4j隔离
PhoneClawAgentExecutor SHALL 隔离LangChain4j对象，不暴露给上层。

#### Scenario: 内部使用ChatLanguageModel
- **WHEN** Agent执行对话
- **THEN** ChatLanguageModel对象仅在Executor内部创建和使用
- **AND** 上层代码不直接访问ChatLanguageModel

#### Scenario: Tool对象内部管理
- **WHEN** Agent注入Tool
- **THEN** Tool对象仅在Executor内部创建和管理
- **AND** 上层通过domain层数据类获取Tool调用信息

### Requirement: 线程安全
PhoneClawAgentExecutor SHALL 是线程安全的。

#### Scenario: 并发访问
- **WHEN** 多个线程同时访问PhoneClawAgentExecutor
- **THEN** 内部状态保持一致性
- **AND** 不产生竞态条件
