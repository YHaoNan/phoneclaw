## ADDED Requirements

### Requirement: 模型提供商管理
系统SHALL提供ModelProviderFacade管理多个AI模型提供商。

#### Scenario: 获取所有提供商
- **WHEN** 请求获取所有模型提供商
- **THEN** 返回已配置的提供商列表

#### Scenario: 添加新提供商
- **WHEN** 添加新的模型提供商配置
- **THEN** 保存配置并更新提供商列表

#### Scenario: 删除提供商
- **WHEN** 删除指定提供商
- **THEN** 移除配置并更新列表

### Requirement: 模型管理
系统SHALL提供模型的增删改查能力。

#### Scenario: 获取提供商下的模型列表
- **WHEN** 查询指定提供商的模型
- **THEN** 返回该提供商支持的所有模型

#### Scenario: 更新模型配置
- **WHEN** 修改模型参数配置
- **THEN** 保存更新后的配置

### Requirement: Domain对象与Entity分离
系统SHALL将Data层Entity与Domain层对象完全分离。

#### Scenario: Entity转Domain对象
- **WHEN** 从Repository获取数据
- **THEN** Entity转换为Domain对象后返回

#### Scenario: Domain对象转Entity
- **WHEN** 保存Domain对象
- **THEN** 转换为Entity后持久化

### Requirement: Repository接口定义
系统SHALL为ModelProvider和Model定义Repository接口。

#### Scenario: ModelProviderRepository操作
- **WHEN** 调用Repository接口
- **THEN** 返回对应的Domain对象或操作结果

#### Scenario: ModelRepository操作
- **WHEN** 调用ModelRepository接口
- **THEN** 支持模型的CRUD操作
