## ADDED Requirements

### Requirement: 基于无障碍服务的屏幕读取
系统SHALL通过EmuAccessibilityScreenReader实现基于AccessibilityService的屏幕读取。

#### Scenario: 读取屏幕UI树
- **WHEN** 无障碍服务已启用并调用读取方法
- **THEN** 返回UITree对象，包含完整的节点层级结构

#### Scenario: 获取窗口信息
- **WHEN** 调用获取窗口信息方法
- **THEN** 返回UIWindow列表，包含所有可见窗口

#### Scenario: 无障碍服务不可用
- **WHEN** 无障碍服务未启用
- **THEN** 抛出ServiceUnavailableException

### Requirement: 基于VLM的屏幕读取
系统SHALL通过EmuVLMScreenReader实现基于视觉语言模型的屏幕读取。

#### Scenario: VLM读取成功
- **WHEN** 调用VLM读取屏幕
- **THEN** 返回基于图像理解的UI树结构

#### Scenario: VLM模型未配置
- **WHEN** VLM模型未配置
- **THEN** 抛出ModelNotConfiguredException
