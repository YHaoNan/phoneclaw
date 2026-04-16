## ADDED Requirements

### Requirement: AskUser工具问题结构
系统 MUST 提供 `AskUser` 工具请求结构，包含 `question` 与 `answers` 字段，用于渲染用户决策弹窗。

#### Scenario: 有效问题结构
- **WHEN** Agent 调用 `AskUser` 并传入问题与选项
- **THEN** 系统校验 `question` 为非空文本
- **AND** 系统校验 `answers` 为 1 到 5 个非空选项
- **AND** 系统将该请求转化为可渲染的用户询问事件

### Requirement: AskUser弹窗展示位置
系统 MUST 将 AskUser 问题弹窗以底部弹出（bottom sheet）方式展示，而不是居中弹窗或全屏替代。

#### Scenario: 触发AskUser展示
- **WHEN** 系统接收到 AskUser 问题请求
- **THEN** UI 从底部弹出问题面板
- **AND** 面板内展示问题文本、选项列表与确认动作

### Requirement: AskUser工具支持其他输入
系统 MUST 在标准选项之外提供“其他”输入通道，并在用户选择该通道时接收自由文本。

#### Scenario: 用户选择其他
- **WHEN** 用户在弹窗中选择“其他”并输入文本
- **THEN** 系统将文本作为最终答案内容
- **AND** 最终答案标记来源为 `other`

### Requirement: AskUser答案必须确认
系统 MUST 要求用户显式点击确认后才提交答案，选择选项本身不得直接完成提交。

#### Scenario: 选择但未确认
- **WHEN** 用户点击某个选项但未点击确认
- **THEN** 当前 AskUser 请求保持 pending
- **AND** 系统不得提交工具结果

#### Scenario: 已确认提交
- **WHEN** 用户点击确认按钮
- **THEN** 系统提交最终答案并触发恢复信号
- **AND** 该答案进入会话记录与执行上下文
