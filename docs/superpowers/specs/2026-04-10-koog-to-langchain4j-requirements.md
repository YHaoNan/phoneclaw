# 需求文档：用 langchain4j 替换 koog 框架

## 1. 背景与目标

**背景**：项目当前使用 koog 框架作为 LLM Agent 能力，现需要完全替换为 langchain4j。

**目标**：
- 用 langchain4j 完全替换 koog 框架
- 保持现有功能不变（Agent 运行、工具调用、流式输出）
- 调整架构：LLM 相关代码集中在 llm 包，UI 层不直接引用框架

---

## 2. 功能需求

### 2.1 LLM 客户端
- 支持 OpenAI 兼容 API（通过 baseUrl 配置）
- 支持流式输出（实时返回 token）
- 配置：baseUrl、apiKey、modelName、temperature

### 2.2 Agent 能力
- 使用 langchain4j StreamingChatModel + Tools 构建 ReAct Agent 循环
- 支持 ReAct 模式的工具调用循环
- 支持自定义系统提示词
- 支持最大迭代次数限制

### 2.3 工具（Tools）
- 使用 langchain4j `@Tool` 注解定义工具
- **PhoneEmulationTool**：执行 Lua 脚本控制手机
- **UseSkillTool**：加载并返回技能文档

### 2.4 事件回调
- 流式输出开始/进行/结束
- 工具调用开始/结束（成功/失败）
- Agent 完成/出错

### 2.5 架构调整
- `AgentOrchestrator` 从 `domain` 包移到 `llm/agent` 包
- `llm` 包对外暴露接口，UI 层通过接口调用
- `OpenAIConfigFragment` 不再直接引用 LLM 框架类

---

## 3. 需要修改的文件

| 文件 | 操作 |
|------|------|
| `llm/agent/PhoneClawAgent.kt` | 重写，使用 langchain4j |
| `llm/agent/AgentOrchestrator.kt` | 从 domain 迁移 |
| `llm/tools/PhoneEmulationTool.kt` | 改用 @Tool 注解 |
| `llm/tools/UseSkillTool.kt` | 改用 @Tool 注解 |
| `llm/provider/ModelInitializer.kt` | 删除或改用 langchain4j |
| `llm/provider/openai/OpenAIModelInitializer.kt` | 删除或改用 langchain4j |
| `ui/settings/model/OpenAIConfigFragment.kt` | 移除 koog 引用，改为纯配置 |
| `domain/AgentCallback.kt` | 保留，llm 包引用 |
| 删除 `llm/PhoneClawAgentExample.kt` | 示例代码可能不需要 |

---

## 4. 不包含的范围

- 不修改 Skills 相关功能
- 不修改 ScriptEngine/Lua 执行相关功能
- 不修改 UI 业务逻辑（只调整 import）

---

## 5. 成功标准

- 项目编译通过
- 原有 Agent 功能测试通过（工具调用、流式输出）
- llm 包外部不依赖任何 koog 类
