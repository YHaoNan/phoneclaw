## MODIFIED Requirements

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

#### Scenario: AskUser作为普通工具调用
- **WHEN** Agent 在执行过程中调用 `AskUser` 工具
- **THEN** 执行器按普通工具调用流程处理该调用
- **AND** AskUser 的确认结果作为工具执行结果返回
- **AND** 不引入新的执行器状态机状态

#### Scenario: 用户确认后继续执行
- **WHEN** 用户完成选择或填写“其他”并点击确认
- **THEN** 执行器接收结构化答案并继续当前执行流程
- **AND** 当前上下文包含本次用户决策结果
- **AND** 流程继续直到正常完成或报错
