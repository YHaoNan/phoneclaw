# Phase 2: Agent 对话系统架构设计

## 目标

构建模块化的 Agent 对话系统，遵循单一职责原则，最小化修改现有代码。

## 架构概览

```
┌─────────────────────────────────────┐
│          UI Layer (Activity)        │
├─────────────────────────────────────┤
│      Presentation Layer (Presenter) │
├─────────────────────────────────────┤
│      Domain Layer (Managers)        │
│  SessionManager | AgentOrchestrator │
│  ModelSelector                      │
├─────────────────────────────────────┤
│      Data Layer (Repositories)      │
│  SessionRepo | MessageRepo | 已有   │
└─────────────────────────────────────┘
```

## 模块设计

### 1. Data Layer（新增）

#### SessionRepository
```kotlin
interface SessionRepository {
    fun createSession(title: String, modelId: String?): String
    fun getSession(id: String): Session?
    fun getAllSessions(): List<Session>
    fun updateSessionTitle(id: String, title: String)
    fun updateSessionModel(id: String, modelId: String)
    fun deleteSession(id: String)
}

data class Session(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val modelId: String?
)
```

#### MessageRepository
```kotlin
interface MessageRepository {
    fun saveMessage(sessionId: String, message: Message)
    fun getMessages(sessionId: String): List<Message>
    fun deleteMessages(sessionId: String)
}

data class Message(
    val id: String,
    val sessionId: String,
    val role: Role,
    val content: String,
    val timestamp: Long,
    val toolName: String?,
    val toolParams: String?,
    val toolResult: String?,
    val toolState: String?,
    val success: Boolean
)

enum class Role { USER, AGENT, TOOL, SKILL }
```

### 2. Domain Layer（新增）

#### SessionManager
职责：会话生命周期管理

```kotlin
class SessionManager(
    private val sessionRepo: SessionRepository,
    private val messageRepo: MessageRepository
) {
    fun createSession(): Session
    fun loadSession(id: String): Session?
    fun deleteSession(id: String)
    fun renameSession(id: String, title: String)
    fun getAllSessions(): List<Session>
}
```

#### ModelSelector
职责：模型选择管理（复用现有 Repository）

```kotlin
data class ModelSelection(
    val provider: ModelProviderEntity,
    val model: ModelEntity
)

class ModelSelector(
    private val providerRepo: ModelProviderRepository,
    private val modelRepo: ModelRepository
) {
    fun getAvailableModels(): List<ModelSelection>
    fun getSelectedModel(): ModelSelection?
    fun selectModel(index: Int)
    var selectedIndex: Int
}
```

#### AgentOrchestrator
职责：Agent 创建与执行

```kotlin
class AgentOrchestrator(
    private val context: Context,
    private val modelSelector: ModelSelector
) {
    suspend fun runAgent(input: String, onResult: (String) -> Unit, onError: (String) -> Unit)
    fun stopAgent()
    fun isRunning(): Boolean
}
```

### 3. Presentation Layer（重构）

#### ChatPresenter（简化）
```kotlin
class ChatPresenter(
    private val sessionManager: SessionManager,
    private val messageRepo: MessageRepository,
    private val agentOrchestrator: AgentOrchestrator,
    private val modelSelector: ModelSelector
) : ChatContract.Presenter {
    // 只负责协调，业务逻辑委托给 Domain 层
}
```

## 数据流

1. **发送消息**: UI → Presenter → AgentOrchestrator → Agent → MessageRepository
2. **选择模型**: UI → Presenter → ModelSelector → (复用现有 Repository)
3. **管理会话**: UI → Presenter → SessionManager → SessionRepository

## 文件结构

```
app/src/main/java/top/yudoge/phoneclaw/
├── domain/                          # 新增
│   ├── SessionManager.kt
│   ├── AgentOrchestrator.kt
│   ├── ModelSelector.kt
│   └── ModelSelection.kt
├── data/                            # 新增
│   ├── SessionRepository.kt
│   ├── SessionRepositoryImpl.kt
│   ├── MessageRepository.kt
│   ├── MessageRepositoryImpl.kt
│   ├── Session.kt
│   └── Message.kt
├── llm/provider/                    # 已有，不修改
│   ├── ModelProviderRepository.kt
│   ├── ModelProviderRepositoryImpl.kt
│   ├── ModelRepository.kt
│   ├── ModelRepositoryImpl.kt
│   ├── ModelProviderEntity.kt
│   └── ModelEntity.kt
└── ui/chat/
    ├── ChatPresenter.kt             # 重构
    └── ChatActivity.kt              # 修改 Presenter 注入
```

## 实现计划

1. 创建 data 层接口和实现（Session, Message）
2. 创建 domain 层（SessionManager, ModelSelector, AgentOrchestrator）
3. 重构 ChatPresenter，注入新模块
4. 修改 ChatActivity 的依赖注入
5. 测试验证
