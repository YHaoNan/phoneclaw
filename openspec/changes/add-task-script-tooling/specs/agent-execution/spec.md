## MODIFIED Requirements

### Requirement: 执行Agent对话
PhoneClawAgentExecutor SHALL 支持执行Agent对话并根据可用脚本在脚本执行与探索执行之间做出安全决策。

#### Scenario: 执行成功
- **WHEN** 调用`run(prompt, model, callback)`
- **THEN** 系统根据model获取对应的ModelProvider
- **AND** 调用provider.createChatModel()创建ChatLanguageModel
- **AND** 注入Memory和Tools
- **AND** 调用LangChain4j Agent执行
- **AND** 通过callback返回结果

#### Scenario: 存在匹配脚本时优先脚本执行
- **WHEN** Agent可通过脚本列表工具获取到与任务匹配的脚本
- **THEN** Agent优先调用脚本执行工具
- **AND** 脚本执行失败时返回明确错误或按策略回退探索执行

#### Scenario: 执行出错
- **WHEN** Agent执行过程中发生错误
- **THEN** 系统调用`callback.onAgentError(e)`
- **AND** 不进行自动重试
