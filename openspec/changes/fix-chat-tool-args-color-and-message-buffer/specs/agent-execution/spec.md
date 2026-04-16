## ADDED Requirements

### Requirement: Agent 回调事件 MUST 端到端保留调用参数
Agent 执行回调链路 MUST 从回调发出到 UI 事件映射全过程保留调用参数，不得丢失或覆盖。

#### Scenario: onToolCallStart 参数到达 UI 映射层
- **WHEN** `onToolCallStart` 以包含 `arguments` 的 `ToolCallInfo` 被调用
- **THEN** 映射后的 UI 事件包含相同的 `arguments` 值
- **AND** 下游渲染器收到的值保持不变

#### Scenario: 调用完成后参数仍可用
- **WHEN** 工具或技能调用成功或失败完成
- **THEN** 最终调用记录仍包含原始 `arguments`
- **AND** `result` 与 `error` 字段不会擦除或替换参数

### Requirement: Agent 消息流生命周期 MUST 发出完成边界
Agent 执行消息流 MUST 发出明确的消息完成边界，以便渲染器完成并清理按消息划分的缓冲。

#### Scenario: 最后一个增量后发出完成边界
- **WHEN** 助手流式输出某条消息的最后文本增量
- **THEN** 回调为该消息标识发出 message-complete 信号
- **AND** 该已完成消息不再追加新的增量

#### Scenario: 新消息使用新的消息标识
- **WHEN** 上一条消息完成后下一条助手回复开始
- **THEN** 回调事件引用新的消息标识
- **AND** 渲染器可以初始化独立的空缓冲

### Requirement: Agent 执行侧修复 MUST 保持最小影响范围
Agent 执行侧改动 MUST 仅覆盖回调事件映射与消息边界信号，不得改变模型选择、工具执行顺序或其它无关执行流程。

#### Scenario: 无关执行流程保持原行为
- **WHEN** 在相同输入条件下对比修复前后执行过程
- **THEN** 模型选择与工具执行顺序保持一致
- **AND** 仅新增参数保真与消息完成边界相关行为
