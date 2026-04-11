## ADDED Requirements

### Requirement: 基于无障碍服务的屏幕操作
系统SHALL通过EmuAccessibilityScreenOperator实现基于AccessibilityService的屏幕操作。

#### Scenario: 执行点击操作
- **WHEN** 调用点击指定坐标或节点
- **THEN** 执行点击并返回操作结果

#### Scenario: 执行滑动操作
- **WHEN** 调用执行滑动
- **THEN** 按指定路径执行滑动并返回结果

#### Scenario: 执行文本输入
- **WHEN** 调用输入文本
- **THEN** 在当前焦点位置输入文本

#### Scenario: 通过节点执行操作
- **WHEN** 提供AccessibilityNodeInfo执行操作
- **THEN** 通过无障碍框架执行对应操作

#### Scenario: 节点不可操作
- **WHEN** 目标节点不可见或不可点击
- **THEN** 返回操作失败状态
