## ADDED Requirements

### Requirement: 聊天调用卡片 MUST 展示并持久化参数
聊天系统 MUST 在消息中的调用卡片里展示工具调用与技能调用的参数，并 MUST 持久化同一份参数载荷用于回放与诊断。

#### Scenario: 工具调用参数被展示并持久化
- **WHEN** 工具调用开始事件携带非空 `arguments`
- **THEN** 聊天卡片展示该参数载荷
- **AND** 持久化调用记录保存相同的 `arguments` 值

#### Scenario: 技能调用空参数仍被明确表示
- **WHEN** 技能调用开始事件的参数为空或为 null
- **THEN** 聊天卡片展示明确的空状态占位
- **AND** 持久化调用记录保存规范化后的空值

### Requirement: 聊天悬浮窗 MUST 按类型与结果使用不同状态颜色
聊天悬浮窗 MUST 同时依据调用类型（`tool` 或 `skill`）与执行结果（`started`、`succeeded`、`failed`）决定状态颜色。

#### Scenario: 工具开始态颜色可区分
- **WHEN** 工具调用进入 started 状态
- **THEN** 悬浮窗使用配置的 `tool-started` 颜色
- **AND** 该颜色与所有技能状态颜色均不同

#### Scenario: 技能失败态颜色可区分
- **WHEN** 技能调用以 failed 状态结束
- **THEN** 悬浮窗使用配置的 `skill-failed` 颜色
- **AND** 该颜色与 `tool-failed` 颜色不同

### Requirement: 流式助手消息缓冲 MUST 按消息隔离
聊天渲染器 MUST 将流式缓冲绑定到单条助手消息标识，并 MUST 在消息完成后立即清理该缓冲。

#### Scenario: 第二条消息不包含第一条内容
- **WHEN** 助手消息 A 完成且助手消息 B 开始
- **THEN** 消息 B 从空缓冲开始渲染
- **AND** 消息 B 输出不包含消息 A 的任何文本

#### Scenario: 最后一条消息仅等于自身增量
- **WHEN** 多条助手消息按顺序流式输出
- **THEN** 每条最终渲染消息仅等于其自身增量拼接结果
- **AND** 任一消息均不包含此前消息的累积文本

### Requirement: 聊天可视化改动 MUST 限定在相关模块
本次聊天可视化修复 MUST 仅修改与聊天调用卡片、悬浮窗状态展示和流式消息渲染直接相关的模块，不得改变无关模块行为。

#### Scenario: 非聊天模块不受影响
- **WHEN** 完成本次修复后执行回归验证
- **THEN** 与聊天可视化无关的模块行为保持不变
- **AND** 不存在因本次变更引入的跨模块功能回归
