## ADDED Requirements

### Requirement: Emu门面提供统一API
系统SHALL通过EmuFacade提供统一的模拟器操作API，封装底层ScreenReader和ScreenOperator实现细节。

#### Scenario: 获取屏幕信息
- **WHEN** 调用EmuFacade获取屏幕信息
- **THEN** 根据配置委托给EmuAccessibilityScreenReader或EmuVLMScreenReader，返回UI树结构

#### Scenario: 执行屏幕操作
- **WHEN** 调用EmuFacade执行点击/滑动等操作
- **THEN** 委托给EmuAccessibilityScreenOperator执行并返回结果

### Requirement: 支持多种屏幕读取策略
系统SHALL支持VLM和无障碍服务两种屏幕读取方式，由EmuFacade根据配置选择。

#### Scenario: 使用无障碍服务读取
- **WHEN** 配置使用无障碍服务读取
- **THEN** EmuFacade委托EmuAccessibilityScreenReader获取UI树

#### Scenario: 使用VLM读取
- **WHEN** 配置使用VLM读取
- **THEN** EmuFacade委托EmuVLMScreenReader分析屏幕截图
