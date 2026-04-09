# Agent回调机制与实时UI更新设计文档

**日期**: 2026-04-07  
**状态**: 设计完成，待用户审查  
**范围**: PhoneClawAgent回调机制、ChatPresenter实时更新、消息持久化策略

---

## 1. 概述

### 1.1 背景

当前Agent运行过程中，用户只能看到"思考中"状态，Agent运行结束后才显示完整对话。这种体验不符合设计要求，用户需要看到Agent的实时运行状态。

### 1.2 目标

1. PhoneClawAgent支持回调机制，在运行过程中通知外部事件
2. ChatPresenter能够实时更新UI，展示Agent的思考过程
3. 多个Agent Turn的消息可以共享记忆（已通过MessageRepository实现）
4. 用户输入时立即显示用户消息
5. 持久化每条消息到数据库

### 1.3 非目标

- 不涉及Agent内部逻辑改动
- 不涉及koog框架本身的改造
- 不涉及UI布局调整

---

## 2. 架构设计

### 2.1 分层架构

采用MVP架构，数据流如下：

```
View Layer (ChatActivity)
    ↓ View接口
Presenter Layer (ChatPresenter)
    ↓ AgentCallback接口
Domain Layer (AgentOrchestrator)
    ↓ AgentCallback接口
Agent Layer (PhoneClawAgent)
    ↓ koog handleEvents
koog Framework
```

### 2.2 核心组件

#### AgentCallback接口

定义Agent运行过程中的所有关键事件：

```kotlin
interface AgentCallback {
    // LLM流式输出相关
    fun onLLMTokenGenerated(token: String)
    fun onLLMStreamStart()
    fun onLLMStreamEnd()
    
    // 工具调用相关
    fun onToolCallStart(toolName: String, params: String)
    fun onToolCallEnd(toolName: String, result: String, success: Boolean)
    
    // Skill调用相关
    fun onSkillCallStart(skillName: String)
    fun onSkillCallEnd(skillName: String, success: Boolean)
    
    // Agent生命周期
    fun onAgentComplete()
    fun onAgentError(error: String)
}
```

**设计说明**:
- **LLM流式输出**: `onLLMTokenGenerated`在每个token生成时调用，实现逐字显示效果
- **工具调用**: 支持工具调用的开始和结束事件，展示调用状态（running/success/failed）
- **Skill调用**: 与工具调用类似，但专门针对Skill
- **生命周期**: Agent整体完成或出错时通知

#### PhoneClawAgent.Builder改造

Builder添加callback参数，在handleEvents中转发事件：

```kotlin
class Builder {
    private var callback: AgentCallback? = null
    
    fun callback(cb: AgentCallback) = apply { this.callback = cb }
    
    fun build(): PhoneClawAgent {
        // ...
        val agent = AIAgent(...) {
            handleEvents {
                // 监听koog框架事件并转发到callback
                onLLMTokenGenerated = { token ->
                    callback?.onLLMTokenGenerated(token)
                }
                
                onToolCallStart = { toolName, params ->
                    callback?.onToolCallStart(toolName, params.toString())
                }
                
                onToolCallEnd = { toolName, result, success ->
                    callback?.onToolCallEnd(toolName, result.toString(), success)
                }
                
                // ... 其他事件
            }
        }
        // ...
    }
}
```

**关键点**:
- callback参数可选，不影响现有代码
- 使用安全调用`?.`避免NPE
- 在koog的handleEvents中监听框架原生事件

#### ChatPresenter改造

Presenter实现AgentCallback接口，管理消息状态：

```kotlin
class ChatPresenter(...) : ChatContract.Presenter, AgentCallback {
    
    // 状态管理
    private var currentAgentMessageId: String? = null
    private var currentAgentMessageContent = StringBuilder()
    private var currentToolCallPosition: Int = -1
    
    // AgentCallback实现
    override fun onLLMStreamStart() {
        // 创建新的AI消息气泡
        currentAgentMessageId = generateId()
        currentAgentMessageContent.clear()
        
        view?.appendMessage(MessageItem.AgentMessage(
            id = currentAgentMessageId!!,
            timestamp = System.currentTimeMillis(),
            content = ""
        ))
    }
    
    override fun onLLMTokenGenerated(token: String) {
        // 追加内容到AI消息气泡
        currentAgentMessageContent.append(token)
        view?.updateAgentMessageContent(currentAgentMessageContent.toString())
    }
    
    override fun onLLMStreamEnd() {
        // 持久化完整的AI消息
        currentAgentMessageId?.let { id ->
            messageRepo.saveMessage(session.id, Message(
                id = id,
                sessionId = session.id,
                role = Role.AGENT,
                content = currentAgentMessageContent.toString(),
                timestamp = System.currentTimeMillis()
            ))
        }
        currentAgentMessageId = null
        currentAgentMessageContent.clear()
    }
    
    override fun onToolCallStart(toolName: String, params: String) {
        // 创建工具调用消息气泡（状态为RUNNING）
        val toolMessage = MessageItem.ToolCallMessage(...)
        view?.appendMessage(toolMessage)
        messageRepo.saveMessage(...)  // 立即持久化
        
        currentToolCallPosition = view?.getCurrentMessageCount()!! - 1
    }
    
    override fun onToolCallEnd(toolName: String, result: String, success: Boolean) {
        // 更新工具调用状态（SUCCESS/FAILED）
        val updatedMessage = ... // 更新状态
        view?.updateMessage(currentToolCallPosition, updatedMessage)
        messageRepo.updateMessage(...)  // 更新持久化
        
        currentToolCallPosition = -1
    }
    
    // ... 其他回调实现
}
```

**设计要点**:
- **状态管理**: 维护当前AI消息ID和内容StringBuilder，支持流式追加
- **UI实时更新**: 每个token生成时立即更新UI
- **持久化策略**: 见下节

#### ChatContract.View接口扩展

新增方法支持实时更新：

```kotlin
interface View {
    // 现有方法...
    
    // 新增方法
    fun updateAgentMessageContent(content: String)
    fun getCurrentMessageCount(): Int
    fun getItemAt(position: Int): MessageItem
}
```

#### AgentOrchestrator改造

添加callback参数传递：

```kotlin
suspend fun runAgent(
    input: String,
    callback: AgentCallback?,
    onResult: (String) -> Unit,
    onError: (String) -> Unit
) {
    // ...
    currentAgent = PhoneClawAgent.builder()
        .callback(callback)  // 传递回调
        .build()
    // ...
}
```

---

## 3. 消息持久化策略

### 3.1 UserMessage

**时机**: 用户发送消息时立即持久化

**原因**: 
- 用户消息是确定的，不需要等待
- 避免丢失用户输入

### 3.2 AgentMessage

**时机**: LLM流结束时持久化完整内容

**原因**:
- 流式输出过程中内容不断变化，不适合中途持久化
- 流结束时内容完整，可以一次性持久化
- 如果中途失败，不会留下不完整的消息

### 3.3 ToolCallMessage

**时机**: 分两阶段持久化

1. **开始时**: 持久化基本信息和running状态
2. **结束时**: 更新结果和状态（SUCCESS/FAILED）

**原因**:
- 需要记录工具调用的开始时间
- 工具调用可能耗时较长，需要中间状态
- 结束时更新结果，保证数据完整性

### 3.4 SkillCallMessage

与ToolCallMessage策略相同。

---

## 4. 数据流

### 4.1 用户发送消息流程

```
用户点击发送
  ↓
ChatActivity.sendMessage()
  ↓
ChatPresenter.sendMessage(content)
  ├─ 创建UserMessage
  ├─ messageRepo.saveMessage(userMessage)
  ├─ view.appendMessage(userMessage)
  └─ runAgent(content)
       ↓
  AgentOrchestrator.runAgent(input, callback=this)
```

### 4.2 Agent运行流程

```
PhoneClawAgent.run()
  ↓
koog框架触发事件
  ↓
handleEvents监听事件
  ↓
callback.onLLMTokenGenerated(token)
  ↓
ChatPresenter.onLLMTokenGenerated(token)
  ├─ currentAgentMessageContent.append(token)
  └─ view.updateAgentMessageContent(content)
       ↓
  ChatActivity.updateAgentMessageContent(content)
       └─ 更新最后一条AI消息的内容
```

### 4.3 工具调用流程

```
koog框架触发工具调用
  ↓
handleEvents.onToolCallStart()
  ↓
callback.onToolCallStart(toolName, params)
  ↓
ChatPresenter.onToolCallStart()
  ├─ 创建ToolCallMessage (state=RUNNING)
  ├─ messageRepo.saveMessage()
  └─ view.appendMessage()
       ↓
  工具执行...
       ↓
handleEvents.onToolCallEnd()
  ↓
callback.onToolCallEnd(toolName, result, success)
  ↓
ChatPresenter.onToolCallEnd()
  ├─ 更新ToolCallMessage (state=SUCCESS/FAILED)
  ├─ messageRepo.updateMessage()
  └─ view.updateMessage()
```

---

## 5. 边界情况处理

### 5.1 Agent中途取消

当用户点击停止按钮时：
- `currentJob?.cancel()` 取消协程
- `onAgentError()` 被调用
- 清理当前状态，重置UI

### 5.2 网络错误

当LLM调用失败时：
- `onAgentError(error)` 被调用
- 显示错误提示
- 如果有正在生成的AI消息，标记为失败

### 5.3 工具调用失败

工具调用失败时：
- `onToolCallEnd(toolName, result, success=false)` 被调用
- 更新工具调用状态为FAILED
- Agent可以根据错误信息决定是否重试

### 5.4 多个Agent Turn共享记忆

已通过MessageRepository实现：
- 每次加载session时，从数据库读取所有历史消息
- Agent运行时，新消息持久化到同一个session
- 多个Agent Turn的消息都属于同一个session，自然共享

---

## 6. 性能考虑

### 6.1 UI更新频率

`onLLMTokenGenerated`会被频繁调用，需要优化：
- 使用`StringBuilder`累积内容，避免频繁字符串拼接
- View层的`updateAgentMessageContent`应该只更新UI，不做复杂计算
- 考虑节流（throttle）机制，如果token生成过快

### 6.2 持久化时机

- AI消息在流结束时一次性持久化，减少数据库写入次数
- 工具调用消息分两次持久化，第一次记录开始，第二次更新结果

### 6.3 内存管理

- `currentAgentMessageContent`使用StringBuilder，内存效率高
- Agent运行结束后，清理所有临时状态

---

## 7. 测试策略

### 7.1 单元测试

- **AgentCallback测试**: 验证各个回调方法的调用时机
- **ChatPresenter测试**: 验证状态管理和UI更新逻辑
- **持久化测试**: 验证消息的保存和更新

### 7.2 集成测试

- 端到端测试：用户发送消息 → Agent运行 → UI实时更新 → 消息持久化
- 边界情况测试：取消Agent、网络错误、工具调用失败

### 7.3 UI测试

- 验证消息气泡的实时更新效果
- 验证工具调用状态的变化

---

## 8. 文件清单

需要修改的文件：

1. **新增文件**:
   - `AgentCallback.kt` - 回调接口定义

2. **修改文件**:
   - `PhoneClawAgent.kt` - Builder添加callback参数，handleEvents实现
   - `AgentOrchestrator.kt` - runAgent方法添加callback参数
   - `ChatPresenter.kt` - 实现AgentCallback接口，管理状态
   - `ChatContract.kt` - View接口新增方法
   - `ChatActivity.kt` - 实现新增的View方法
   - `MessageAdapter.kt` - 添加辅助方法

3. **可能需要修改**:
   - `MessageRepository.kt` - 添加updateMessage方法
   - `MessageRepositoryImpl.kt` - 实现updateMessage

---

## 9. 风险与缓解

### 9.1 koog框架API不确定性

**风险**: koog的handleEvents API可能与我们假设的不一致

**缓解**: 
- 先调研koog文档和源码
- 如果API不同，调整实现方案

### 9.2 流式输出支持

**风险**: koog可能不支持流式输出的token级别事件

**缓解**:
- 如果不支持，可以简化为整个响应完成后一次性显示
- 或者使用PromptExecutor的流式API

### 9.3 性能问题

**风险**: 频繁的UI更新可能导致卡顿

**缓解**:
- 实现节流机制
- 使用DiffUtil优化RecyclerView更新

---

## 10. 实现优先级

1. **高优先级**（核心功能）:
   - AgentCallback接口定义
   - PhoneClawAgent.Builder改造
   - ChatPresenter实现回调
   - 用户消息立即显示

2. **中优先级**（体验优化）:
   - 流式输出支持
   - 工具调用状态实时更新
   - 消息持久化

3. **低优先级**（可后续优化）:
   - 性能优化（节流）
   - 错误恢复机制

---

## 11. 下一步

设计文档已编写完成，接下来需要：

1. ✅ Spec自我审查
2. ⏳ 用户审查设计文档
3. ⏳ 调用writing-plans技能编写实现计划
4. ⏳ 开始实现

---

**设计审查清单**:
- [ ] 无TBD或TODO占位符
- [ ] 各部分逻辑一致，无矛盾
- [ ] 范围明确，聚焦单一功能
- [ ] 需求明确，无歧义
