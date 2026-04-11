# PhoneClawAgent实现
> 本文档旨在指导AI如何实现PhoneClawAgent

依赖：LangChain4J

## 核心组件

- `PhoneClawAgentExecutor`：用于组合当前的`Session`、`Skill`、`Tool`、`ModelProvider`、`Model`以及`LangChain4j`，对外提供整合的agent服务

## 核心接口
> 见当前代码


## 上层用例
- chat模块：
  - 每次切换session（新建session、切换session）时重新创建AgentExecutor对象
  - chat模块需要处理两种消息
    - session中的历史消息，需要将其渲染至页面
    - 当前新增消息，由agentExecutor的回调给出
    - chat模块需要同时维护session的保存
