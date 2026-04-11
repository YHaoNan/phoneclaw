# PhoneClaw项目分层架构设计

> 本文档旨在对PhoneClaw项目的架构进行拆解、定义、梳理，让人类和AI基于该架构进行可维护的代码生成。

# 1. High Level 层次结构

```text
Android Framework / App Entry
    ↓
UI Layer
    ↓
Domain Layer（按需存在，但本项目建议作为主干层）
    ↓
Data Layer
    ↓
Local / Remote / Integration
```



按照Android官方规范与最佳实践，我们将项目总体分成五层：

- Android Framework Layer：Android的Application、Service等组件，处于最顶层。

- UI Layer：展示层，Activity、Fragment、Dialog、Adapter等等。

- Domain Layer：领域层，提供复杂领域对象和复杂领域业务逻辑。主要负责业务编排。

- Data Layer：数据层，负责处理存储。在这里各类数据源都被抽象成Repository。如数据库、文件、网络。

- Local/Remote/Integration层：负责处理DataLayer具体集成的实现层。如SQLite、Remote Http调用、LangChain4j等



核心原则：

1. 上层可以引用下层
   
   1. 如UILayer可以引用Domain Layer。在无复杂业务编排的场景下，也可以直接引用Data Layer
   
   2. Domain Layer使用Data Layer完成业务编排。
   
   3. ...

2. 下层不能引用上层
   
   1. Data Layer不能触碰也没有理由触碰UI Layer
   
   2. 唯一的例外是底层Layer在部分情况下可以依赖Android Framework对象，如启动Service或依赖无障碍服务提供的能力时。

3. 同层级可以相互引用

4. DomainLayer可单元测试。通过MockDataLayer





# 2. 具体分包规则

分包规则采用模块优先、层级次优的基本原则，保证单个模块的所有类聚合在一起，并具有清晰的层次结构辅助了解它们之间的引用关系。

1. 每个功能模块从Domain层开始被提出到顶级包中，如`llm`中包含所有大模型相关的类，`emu`中包含所有模拟相关的类
2. Android Framework层使用`app`顶级包，包含所有Android生命周期组件
3. ui层使用`ui`顶级包，并在内部按照ui模块进行二级分包，如`chat`、`settings`、`provider`等
4. ui层必须使用MVP进行开发，提供`Contract`来约束`View`层接口和`Presenter`层接口

```
src
   app
      PhoneClawApp
      AgentBubbleFloatingWindowService
      EmuAccessiblilityService
   ui
      chat
         ChatContract    # 提供presenter和view层的接口，用于将业务逻辑和View解耦
         ChatActivity    # 继承自ChatContract.View  专注于处理View
         ChatPresenter   # 继承自ChatContract.Presenter   专注于处理业务逻辑
         ...
      settings
         ModelListActivity
         ....
         
   emu
      domain
         objects
            UITree
            UIWindow
            AppInfo
         EmuVLMScreenReader          # 基于VLM对屏幕进行读取
         EmuAccessbilityScreenReader # 基于无障碍对屏幕进行读取
         EmuAccessbilityScreenOperator  # 基于无障碍和Gesture对屏幕进行操作
         EmuFacade                   # 封装所有API的门面
   llm
      domain
         objects
            ModelProvider
            Model
            Skill
            SkillWithContent
            Message
            Session
         ModelProviderFacade
         SkillFacade   # 聚合所有SkillRepository向外提供服务
         SessionFacade # 聚合Message和Session能力，向外界提供消息的存储、增删改查
         PhoneClawAgent
      data
         entity
            ModelProviderEntity
            ModelEntity
            SkillEntity
            MessageEntity
            SessionEntity
         repository
            ModelProviderRepository
            ModelProviderRepositoryImpl
            ModelRepository
            ModelRepositoryImpl
            SkillRepository
            BuiltInSkillRepository # Store In assets(index file + skill file, read-only)
            UserSkillRepository    # Store In DB(index) + File(skill it self)
            MessageRepository
            SessionRepository
            /* ignore impl */
      integration
         OpenAIProviderIntegration # 负责基于具体的ModelProvider配置创建LangChain的ChatModel
         AgentIntegration          # 负责集成LangChain的Agent
         
```

