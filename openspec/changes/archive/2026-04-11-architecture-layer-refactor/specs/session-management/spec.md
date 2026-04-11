## ADDED Requirements

### Requirement: Session管理
系统SHALL提供Session概念管理用户会话。

#### Scenario: 创建新会话
- **WHEN** 用户开始新对话
- **THEN** 创建新Session并返回Session ID

#### Scenario: 获取会话列表
- **WHEN** 请求会话列表
- **THEN** 返回按时间排序的Session列表

#### Scenario: 删除会话
- **WHEN** 删除指定会话
- **THEN** 删除Session及其关联的所有Message

#### Scenario: 更新会话标题
- **WHEN** 修改会话标题
- **THEN** 更新Session的title字段

### Requirement: Message管理
系统SHALL提供Message概念管理会话消息。

#### Scenario: 添加消息
- **WHEN** 在会话中添加新消息
- **THEN** 保存Message并关联到Session

#### Scenario: 获取会话消息
- **WHEN** 请求指定会话的消息列表
- **THEN** 返回按时间排序的Message列表

#### Scenario: 删除消息
- **WHEN** 删除指定消息
- **THEN** 从数据库移除Message记录

### Requirement: SessionFacade服务
系统SHALL通过SessionFacade聚合Session和Message能力对外提供服务。

#### Scenario: 获取完整会话数据
- **WHEN** 请求会话详情
- **THEN** 返回Session及关联的所有Message

#### Scenario: 清理过期会话
- **WHEN** 执行会话清理
- **THEN** 删除超过保留期限的会话及其消息

### Requirement: 数据持久化
系统SHALL使用SQLite存储Session和Message数据。

#### Scenario: 数据库初始化
- **WHEN** 应用首次启动
- **THEN** 创建Session和Message表

#### Scenario: 数据迁移
- **WHEN** 数据库版本升级
- **THEN** 执行迁移脚本保留现有数据
