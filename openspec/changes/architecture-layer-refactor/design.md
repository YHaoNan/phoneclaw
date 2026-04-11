## Context

当前PhoneClaw项目是一个Android应用，用于AI辅助的手机操作。现有代码缺乏清晰的分层架构，模块边界模糊，数据流向不明确。本次重构将基于Android官方推荐的五层架构进行彻底重组。

**当前状态**:
- Emu模块: `EmuApi`承担过多职责，DomainService职责不清
- LLM模块: Entity与Domain对象混杂，Skill存储单一，缺乏会话管理

**约束**:
- 必须保持现有功能正常运行
- 采用Kotlin语言开发
- 遵循Android官方架构指南
- 支持无障碍服务和VLM两种屏幕读取方式

## Goals / Non-Goals

**Goals:**
- 实现清晰的五层架构划分
- 每个模块职责单一、边界清晰
- 数据流向明确，便于推理和维护
- Domain层可单元测试
- UI层采用MVP模式解耦

**Non-Goals:**
- 不改变现有功能行为
- 不增加新功能特性
- 不涉及性能优化

## Decisions

### 1. 分包策略: 模块优先

**决定**: 采用模块优先、层级次优的分包原则

**理由**:
- 单个模块的所有类聚合在一起，便于查找和维护
- 层级结构作为二级分包，清晰展示引用关系
- 替代方案(层级优先)会导致模块分散，不利于模块边界理解

### 2. Skill存储: 多源Repository

**决定**: Skill拆分为 `BuiltInSkillRepository` (assets只读) + `UserSkillRepository` (DB+文件)

**理由**:
- 内建Skill只需assets存储，无需数据库
- 用户Skill需要持久化和修改能力
- 索引机制加速检索，元数据与内容分离
- 替代方案(单一Repository)会导致读写逻辑复杂耦合

### 3. 会话管理: Session/Message分离

**决定**: Session聚合Message，通过Facade统一对外

**理由**:
- 会话和消息是两个独立概念但紧密关联
- 通过Facade简化外部调用
- 数据层使用SQLite存储，支持复杂查询
- 替代方案(统一Model)会丧失灵活性

### 4. Emu模块: Facade模式

**决定**: `EmuApi` → `EmuFacade`，拆分DomainService为 `EmuAccessibilityScreenReader`、`EmuVLMScreenReader` 和 `EmuAccessibilityScreenOperator`

**理由**:
- 门面模式简化外部调用
- 读取和操作是两个独立关注点
- VLM和无障碍读取方法差异大，不需要统一接口
- 替代方案(保持单一Service)违反单一职责原则

### 5. 数据库: SQLiteOpenHelper

**决定**: 使用原生Android SQLiteOpenHelper，不引入Room。数据库帮助类放置在 `top.yudoge.phoneclaw.app.data.PhoneClawDatabaseHelper`，作为整个项目的唯一数据库入口。

**理由**:
- Repository层抽象隔离了数据访问细节
- 后续可无缝替换为Room或其他ORM
- 减少额外依赖，降低复杂度
- 替代方案(Room)增加学习成本和构建时间
- 放在 app.data 下表明这是应用级基础设施，而非某个模块私有

### 6. 数据访问层命名: Repository

**决定**: 所有数据访问层统一命名为 Repository，放置在各模块的 `data/repository` 下，不使用 Dao 命名。

**理由**:
- Repository 模式更符合领域驱动设计理念
- 统一命名减少认知负担
- Dao 通常与 Room 等 ORM 框架绑定，Repository 更抽象
- 清晰的包结构: `llm.data.repository.XxxRepository`

### 7. 依赖管理: AppContainer

**决定**: 创建 `top.yudoge.phoneclaw.app.AppContainer` 全局单例管理器，统一管理所有 Repository、DBHelper、领域层服务(Facade等)。

**理由**:
- 集中管理依赖生命周期，避免分散创建
- 为未来引入依赖注入框架(Dagger/Hilt)预留空间
- 简化各组件获取依赖的方式
- 便于测试时替换依赖实现
- 替代方案(直接在各处创建实例)导致依赖分散、难以测试

### 8. UI架构: MVP模式

**决定**: UI层统一采用MVP模式，通过Contract约束接口

**理由**:
- View与Presenter解耦，便于单元测试
- Contract明确约定双方职责
- 符合Android社区最佳实践
- 替代方案(MVVM)需要引入更多依赖

## Risks / Trade-offs

**风险**: 重构范围大，可能引入回归问题
→ **缓解**: 分阶段实施，每个阶段完成后进行功能验证

**风险**: 学习曲线，团队需要适应新架构
→ **缓解**: 提供清晰的架构文档和代码示例

**权衡**: 增加了代码量(更多接口和对象)
→ **收益**: 提升可维护性、可测试性和扩展性

## Migration Plan

**阶段1: 基础架构搭建**
1. 创建新的包结构
2. 定义Domain层对象接口
3. 定义Repository接口

**阶段2: Emu模块重构**
1. 创建EmuFacade
2. 实现ScreenReader/ScreenOperator
3. 迁移现有调用方

**阶段3: LLM模块重构**
1. 创建Domain层对象
2. 实现多源SkillRepository
3. 实现Session/Message管理
4. 集成LangChain4j

**阶段4: UI层改造**
1. 为各UI模块添加Contract
2. 实现Presenter层
3. 重构Activity/Fragment

**回滚策略**: 每个阶段完成后创建Git tag，可回滚到任意阶段

## Open Questions

1. 是否需要为Integration层引入依赖注入框架?
2. Skill索引的缓存策略如何设计?
3. Session数据的清理策略如何定义?
