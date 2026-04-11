## Why

当前项目架构缺乏清晰的分层和模块边界，代码耦合度高，难以维护和扩展。随着功能复杂度增加，需要建立一套标准的五层架构，明确数据流向和职责边界，提升代码的可维护性和可测试性。

## What Changes

**核心架构变更**:
- 实施五层架构: Android Framework → UI → Domain → Data → Local/Remote/Integration
- 按模块优先原则重新分包，每个功能模块聚合在顶级包中
- UI层采用MVP模式，通过Contract约束View和Presenter接口

**Emu模块**:
- `EmuApi` → `EmuFacade` (门面模式)
- 拆分DomainService为 `ScreenReader` 和 `ScreenOperator`
- 无障碍服务移至 `app` 顶级模块

**LLM模块**:
- Data层Entity与Domain层对象完全分离
- Skill Repository拆分为多源: `BuiltInSkillRepository` (assets只读) + `UserSkillRepository` (DB+文件读写)
- 新增Skill索引机制加速检索
- 新增Session/Message概念，支持会话持久化
- Integration层集成LangChain4j

## Capabilities

### New Capabilities

- `emu-facade`: Emu模块门面API，封装屏幕读取和操作能力
- `screen-reader`: 基于VLM/无障碍服务的屏幕读取能力
- `screen-operator`: 基于无障碍和Gesture的屏幕操作能力
- `model-provider`: 模型提供商管理，支持多提供商配置
- `skill-management`: Skill多源存储(内建/用户)，索引加速检索
- `session-management`: 会话和消息的持久化存储与查询
- `langchain-integration`: LangChain4j集成层，Agent与Model适配

### Modified Capabilities

(无现有规格需修改)

## Impact

**包结构重构**:
- `app/` - Android Framework层 (Application, Service)
- `ui/` - UI层 (Activity, Fragment, Presenter, Contract)
- `emu/` - 模拟器相关领域 (domain层对象, Facade)
- `llm/` - LLM相关领域 (domain层对象, data层, integration层)

**破坏性变更**:
- **BREAKING**: 现有EmuApi调用方需迁移至EmuFacade
- **BREAKING**: 现有LLM模块的Entity直接使用需改为通过Domain对象访问

**依赖变更**:
- 新增SQLite依赖 (用于Skill元数据和Session存储)
- 新增LangChain4j依赖 (用于Agent和Model集成)
