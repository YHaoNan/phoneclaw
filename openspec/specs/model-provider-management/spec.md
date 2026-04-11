# Capability: Model Provider Management

## Purpose

TBD

## Requirements

### Requirement: ModelProvider抽象设计
ModelProvider SHALL 定义为抽象类，位于domain层，具体实现类位于integration层。

#### Scenario: 抽象类结构
- **WHEN** 定义ModelProvider抽象类
- **THEN** 包含公共字段：id、name、providerType
- **AND** 包含抽象方法：supportAutoFetchModelList()、fetchModelList()、createChatModel()、parseToConfig()

#### Scenario: LangChain4j隔离
- **WHEN** 上层代码（UI、Domain门面层）使用ModelProvider
- **THEN** 不直接接触LangChain4j对象
- **AND** LangChain4j对象仅在integration层创建

### Requirement: 创建模型提供商
系统 SHALL 允许用户通过三步骤表单创建新的模型提供商。

#### Scenario: 创建OpenAI提供商成功
- **WHEN** 用户填写提供商名称为"My OpenAI"
- **AND** 选择提供商类型为"OpenAI Compatible"
- **AND** 填写baseUrl为"https://api.openai.com"
- **AND** 填写apiKey为有效值
- **THEN** 系统保存提供商到数据库
- **AND** 返回提供商ID

#### Scenario: 创建提供商时必填项为空
- **WHEN** 用户未填写提供商名称
- **THEN** 系统显示错误提示"请填写提供商名称"
- **AND** 禁用下一步按钮

### Requirement: OpenAI提供商配置
OpenAI提供商 SHALL 支持以下配置项：baseUrl、apiKey、chatCompletionUrl、modelsUrl、responseApiEnabled、responseUrl、connectTimeout、requestTimeout。

#### Scenario: 使用默认配置创建OpenAI提供商
- **WHEN** 用户填写baseUrl为"https://api.openai.com"
- **AND** apiKey为有效值
- **AND** 其他字段保持默认
- **THEN** 系统使用默认值填充chatCompletionUrl="/v1/chat/completions"
- **AND** 默认modelsUrl="/v1/models"
- **AND** 默认responseApiEnabled=false

#### Scenario: 启用Response API
- **WHEN** 用户启用responseApiEnabled开关
- **THEN** 系统显示responseUrl输入框
- **AND** 默认responseUrl="/v1/responses"

### Requirement: 自动获取模型列表
系统 SHALL 在提供商支持时提供自动获取模型列表功能。

#### Scenario: OpenAI提供商自动获取模型
- **WHEN** 用户完成OpenAI提供商配置
- **THEN** 系统弹出对话框"当前提供商支持自动获取模型列表，是否自动获取"
- **AND** 用户选择"自动获取"
- **THEN** 系统调用OpenAI Models API获取模型列表
- **AND** 自动创建Model实体

#### Scenario: 跳过自动获取
- **WHEN** 用户选择"跳过"
- **THEN** 系统跳转到手动添加模型页面

### Requirement: 编辑模型提供商
系统 SHALL 支持编辑已存在的模型提供商，且不影响已关联的模型数据。

#### Scenario: 编辑提供商配置
- **WHEN** 用户修改提供商的baseUrl
- **THEN** 系统更新提供商配置
- **AND** 已关联的模型数据保持不变

#### Scenario: 编辑涉密字段
- **WHEN** 用户进入编辑页面
- **THEN** apiKey字段使用密码输入框
- **AND** 显示"显示/隐藏"切换按钮
- **AND** 加载已保存的apiKey到表单

### Requirement: 删除模型提供商
系统 SHALL 支持删除模型提供商，并级联删除关联的模型。

#### Scenario: 删除提供商
- **WHEN** 用户删除一个提供商
- **THEN** 系统删除该提供商
- **AND** 删除该提供商下所有模型

### Requirement: 提供商列表展示
系统 SHALL 以卡片列表形式展示所有提供商。

#### Scenario: 收起状态显示
- **WHEN** 用户查看提供商列表
- **THEN** 每个提供商显示为卡片
- **AND** 默认收起状态仅显示提供商名称

#### Scenario: 展开状态显示
- **WHEN** 用户点击展开卡片
- **THEN** 显示该提供商下所有模型的显示名称
- **AND** 显示"添加模型"快捷按钮

### Requirement: 模型管理
系统 SHALL 支持为提供商添加、编辑、删除模型。

#### Scenario: 手动添加模型
- **WHEN** 用户填写模型ID为"gpt-4"
- **AND** 显示名称为"GPT-4"
- **AND** 视觉能力为true
- **THEN** 系统创建Model实体并关联到当前提供商

### Requirement: 获取提供商模型嵌套结构
ModelProviderFacade SHALL 提供获取提供商-模型嵌套结构的方法。

#### Scenario: 获取嵌套结构
- **WHEN** 调用`getAllProvidersWithModels()`
- **THEN** 返回`List<ProviderWithModels>`
- **AND** 每个ProviderWithModels包含提供商信息和模型列表

### Requirement: 删除冗余Integration文件
系统 SHALL 删除冗余的Integration层文件，职责合并到ModelProvider实现类。

#### Scenario: 文件删除
- **WHEN** 实现ModelProvider抽象体系
- **THEN** 删除`OpenAIProviderIntegration.kt`
- **AND** 删除`AgentIntegration.kt`
- **AND** 相关职责合并到`OpenAIModelProvider`
