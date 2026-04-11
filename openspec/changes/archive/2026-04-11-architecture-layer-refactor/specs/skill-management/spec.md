## ADDED Requirements

### Requirement: Skill多源存储
系统SHALL支持内建Skill和用户Skill两种存储源。

#### Scenario: 获取内建Skill
- **WHEN** 请求内建Skill列表
- **THEN** 从assets加载只读的内建Skill

#### Scenario: 获取用户Skill
- **WHEN** 请求用户Skill列表
- **THEN** 从数据库和文件系统加载可读写的用户Skill

#### Scenario: 添加用户Skill
- **WHEN** 创建新的用户Skill
- **THEN** 保存到用户存储源并更新索引

### Requirement: Skill索引机制
系统SHALL为Skill提供索引加速检索。

#### Scenario: 内建Skill索引
- **WHEN** 应用启动
- **THEN** 从assets/skills/index.json加载内建Skill索引

#### Scenario: 用户Skill索引
- **WHEN** 应用启动或Skill变更
- **THEN** 更新SQLite中的用户Skill元数据索引

#### Scenario: 通过索引快速查询
- **WHEN** 按条件查询Skill
- **THEN** 先查询索引获取元数据，再按需加载完整内容

### Requirement: Skill Facade服务
系统SHALL通过SkillFacade聚合所有SkillRepository对外提供服务。

#### Scenario: 获取所有Skill
- **WHEN** 请求所有Skill
- **THEN** 返回内建和用户Skill的合并列表，标记来源

#### Scenario: 按条件搜索Skill
- **WHEN** 提供搜索条件
- **THEN** 返回匹配的Skill列表

### Requirement: Skill Domain对象
系统SHALL提供Skill和SkillWithContent两种Domain对象。

#### Scenario: 获取Skill元数据
- **WHEN** 请求Skill列表
- **THEN** 返回Skill对象，包含元数据但不包含内容

#### Scenario: 获取完整Skill内容
- **WHEN** 请求指定Skill的完整内容
- **THEN** 返回SkillWithContent对象，包含元数据和内容

#### Scenario: 区分Skill来源
- **WHEN** 获取Skill对象
- **THEN** 通过source字段标识是BUILT_IN还是USER来源
