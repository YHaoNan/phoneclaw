# Agent Chat System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建模块化的 Agent 对话系统，分离 Session、Message、Model Selection、Agent Orchestration 职责

**Architecture:** 四层架构（UI → Presentation → Domain → Data），复用现有 ModelProviderRepository/ModelRepository，新增 SessionManager、ModelSelector、AgentOrchestrator

**Tech Stack:** Kotlin, Coroutines, Android MVP

---

## File Structure

```
app/src/main/java/top/yudoge/phoneclaw/
├── data/session/                    # 新增
│   ├── Session.kt
│   ├── SessionRepository.kt
│   └── SessionRepositoryImpl.kt
├── data/message/                    # 新增
│   ├── Message.kt
│   ├── MessageRepository.kt
│   └── MessageRepositoryImpl.kt
├── domain/                          # 新增
│   ├── ModelSelection.kt
│   ├── ModelSelector.kt
│   ├── SessionManager.kt
│   └── AgentOrchestrator.kt
├── llm/provider/                    # 已有，不修改
│   └── ...
└── ui/chat/
    ├── ChatContract.kt              # 修改
    ├── ChatPresenter.kt             # 重构
    └── ChatActivity.kt              # 修改依赖注入
```

---

### Task 1: Create Session Data Model

**Files:**
- Create: `app/src/main/java/top/yudoge/phoneclaw/data/session/Session.kt`

- [ ] **Step 1: Create Session data class**

```kotlin
package top.yudoge.phoneclaw.data.session

data class Session(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val modelId: String?
)
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/top/yudoge/phoneclaw/data/session/Session.kt
git commit -m "feat: add Session data model"
```

---

### Task 2: Create SessionRepository Interface

**Files:**
- Create: `app/src/main/java/top/yudoge/phoneclaw/data/session/SessionRepository.kt`

- [ ] **Step 1: Create SessionRepository interface**

```kotlin
package top.yudoge.phoneclaw.data.session

interface SessionRepository {
    fun createSession(title: String, modelId: String?): String
    fun getSession(id: String): Session?
    fun getAllSessions(): List<Session>
    fun updateSessionTitle(id: String, title: String)
    fun updateSessionModel(id: String, modelId: String)
    fun deleteSession(id: String)
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/top/yudoge/phoneclaw/data/session/SessionRepository.kt
git commit -m "feat: add SessionRepository interface"
```

---

### Task 3: Create SessionRepositoryImpl

**Files:**
- Create: `app/src/main/java/top/yudoge/phoneclaw/data/session/SessionRepositoryImpl.kt`

- [ ] **Step 1: Create SessionRepositoryImpl**

```kotlin
package top.yudoge.phoneclaw.data.session

import android.content.Context
import top.yudoge.phoneclaw.db.PhoneClawDbHelper

class SessionRepositoryImpl(context: Context) : SessionRepository {
    private val dbHelper = PhoneClawDbHelper(context)

    override fun createSession(title: String, modelId: String?): String {
        val id = java.util.UUID.randomUUID().toString()
        dbHelper.saveSession(id, title, System.currentTimeMillis(), modelId)
        return id
    }

    override fun getSession(id: String): Session? {
        val record = dbHelper.getSession(id) ?: return null
        return Session(
            id = record.id,
            title = record.title,
            createdAt = record.createdAt,
            updatedAt = record.updatedAt,
            modelId = record.modelId
        )
    }

    override fun getAllSessions(): List<Session> {
        return dbHelper.allSessions.map { record ->
            Session(
                id = record.id,
                title = record.title,
                createdAt = record.createdAt,
                updatedAt = record.updatedAt,
                modelId = record.modelId
            )
        }
    }

    override fun updateSessionTitle(id: String, title: String) {
        dbHelper.updateSessionTitle(id, title)
    }

    override fun updateSessionModel(id: String, modelId: String) {
        dbHelper.updateSessionModel(id, modelId)
    }

    override fun deleteSession(id: String) {
        dbHelper.deleteSession(id)
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/top/yudoge/phoneclaw/data/session/SessionRepositoryImpl.kt
git commit -m "feat: add SessionRepositoryImpl"
```

---

### Task 4: Create Message Data Model

**Files:**
- Create: `app/src/main/java/top/yudoge/phoneclaw/data/message/Message.kt`

- [ ] **Step 1: Create Message data class**

```kotlin
package top.yudoge.phoneclaw.data.message

data class Message(
    val id: String,
    val sessionId: String,
    val role: Role,
    val content: String,
    val timestamp: Long,
    val toolName: String? = null,
    val toolParams: String? = null,
    val toolResult: String? = null,
    val toolState: String? = null,
    val success: Boolean = false
)

enum class Role {
    USER, AGENT, TOOL, SKILL
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/top/yudoge/phoneclaw/data/message/Message.kt
git commit -m "feat: add Message data model"
```

---

### Task 5: Create MessageRepository Interface

**Files:**
- Create: `app/src/main/java/top/yudoge/phoneclaw/data/message/MessageRepository.kt`

- [ ] **Step 1: Create MessageRepository interface**

```kotlin
package top.yudoge.phoneclaw.data.message

interface MessageRepository {
    fun saveMessage(sessionId: String, message: Message)
    fun getMessages(sessionId: String): List<Message>
    fun deleteMessages(sessionId: String)
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/top/yudoge/phoneclaw/data/message/MessageRepository.kt
git commit -m "feat: add MessageRepository interface"
```

---

### Task 6: Create MessageRepositoryImpl

**Files:**
- Create: `app/src/main/java/top/yudoge/phoneclaw/data/message/MessageRepositoryImpl.kt`

- [ ] **Step 1: Create MessageRepositoryImpl**

```kotlin
package top.yudoge.phoneclaw.data.message

import android.content.Context
import top.yudoge.phoneclaw.db.PhoneClawDbHelper

class MessageRepositoryImpl(context: Context) : MessageRepository {
    private val dbHelper = PhoneClawDbHelper(context)

    override fun saveMessage(sessionId: String, message: Message) {
        val record = PhoneClawDbHelper.MessageRecord().apply {
            role = when (message.role) {
                Role.USER -> "user"
                Role.AGENT -> "agent"
                Role.TOOL -> "tool"
                Role.SKILL -> "skill"
            }
            content = message.content
            timestamp = message.timestamp
            toolName = message.toolName
            toolParams = message.toolParams
            toolResult = message.toolResult
            toolState = message.toolState
            success = message.success
        }
        dbHelper.saveMessage(sessionId, record)
    }

    override fun getMessages(sessionId: String): List<Message> {
        return dbHelper.getMessages(sessionId).map { record ->
            Message(
                id = record.timestamp.toString(),
                sessionId = sessionId,
                role = when (record.role) {
                    "user" -> Role.USER
                    "agent" -> Role.AGENT
                    "tool" -> Role.TOOL
                    "skill" -> Role.SKILL
                    else -> Role.USER
                },
                content = record.content,
                timestamp = record.timestamp,
                toolName = record.toolName,
                toolParams = record.toolParams,
                toolResult = record.toolResult,
                toolState = record.toolState,
                success = record.success
            )
        }
    }

    override fun deleteMessages(sessionId: String) {
        // Messages are deleted via dbHelper.deleteSession
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/top/yudoge/phoneclaw/data/message/MessageRepositoryImpl.kt
git commit -m "feat: add MessageRepositoryImpl"
```

---

### Task 7: Create ModelSelection

**Files:**
- Create: `app/src/main/java/top/yudoge/phoneclaw/domain/ModelSelection.kt`

- [ ] **Step 1: Create ModelSelection data class**

```kotlin
package top.yudoge.phoneclaw.domain

import top.yudoge.phoneclaw.llm.provider.ModelEntity
import top.yudoge.phoneclaw.llm.provider.ModelProviderEntity

data class ModelSelection(
    val provider: ModelProviderEntity,
    val model: ModelEntity
)
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/top/yudoge/phoneclaw/domain/ModelSelection.kt
git commit -m "feat: add ModelSelection"
```

---

### Task 8: Create ModelSelector

**Files:**
- Create: `app/src/main/java/top/yudoge/phoneclaw/domain/ModelSelector.kt`

- [ ] **Step 1: Create ModelSelector**

```kotlin
package top.yudoge.phoneclaw.domain

import top.yudoge.phoneclaw.llm.provider.ModelProviderRepository
import top.yudoge.phoneclaw.llm.provider.ModelRepository

class ModelSelector(
    private val providerRepo: ModelProviderRepository,
    private val modelRepo: ModelRepository
) {
    private var models: List<ModelSelection> = emptyList()
    var selectedIndex: Int = 0

    fun loadAvailableModels(): List<ModelSelection> {
        val providers = providerRepo.listProvider()
        models = providers.flatMap { provider ->
            modelRepo.getModelsByProvider(provider.id).map { model ->
                ModelSelection(provider, model)
            }
        }
        if (selectedIndex >= models.size) {
            selectedIndex = 0
        }
        return models
    }

    fun getAvailableModels(): List<ModelSelection> = models

    fun getSelectedModel(): ModelSelection? {
        return if (models.isNotEmpty() && selectedIndex in models.indices) {
            models[selectedIndex]
        } else null
    }

    fun selectModel(index: Int) {
        selectedIndex = if (index in models.indices) index else 0
    }

    fun getDisplayNames(): List<String> {
        return models.map { "${it.provider.name}: ${it.model.displayName}" }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/top/yudoge/phoneclaw/domain/ModelSelector.kt
git commit -m "feat: add ModelSelector"
```

---

### Task 9: Create SessionManager

**Files:**
- Create: `app/src/main/java/top/yudoge/phoneclaw/domain/SessionManager.kt`

- [ ] **Step 1: Create SessionManager**

```kotlin
package top.yudoge.phoneclaw.domain

import top.yudoge.phoneclaw.data.session.Session
import top.yudoge.phoneclaw.data.session.SessionRepository

class SessionManager(
    private val sessionRepo: SessionRepository
) {
    fun createSession(): Session {
        val id = sessionRepo.createSession("新对话", null)
        return sessionRepo.getSession(id)!!
    }

    fun getSession(id: String): Session? = sessionRepo.getSession(id)

    fun getAllSessions(): List<Session> = sessionRepo.getAllSessions()

    fun updateTitle(id: String, title: String) {
        sessionRepo.updateSessionTitle(id, title)
    }

    fun deleteSession(id: String) {
        sessionRepo.deleteSession(id)
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/top/yudoge/phoneclaw/domain/SessionManager.kt
git commit -m "feat: add SessionManager"
```

---

### Task 10: Create AgentOrchestrator

**Files:**
- Create: `app/src/main/java/top/yudoge/phoneclaw/domain/AgentOrchestrator.kt`

- [ ] **Step 1: Create AgentOrchestrator**

```kotlin
package top.yudoge.phoneclaw.domain

import android.content.Context
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import org.json.JSONObject
import top.yudoge.phoneclaw.llm.agent.PhoneClawAgent
import java.io.File

class AgentOrchestrator(
    private val context: Context,
    private val modelSelector: ModelSelector
) {
    private var currentAgent: PhoneClawAgent? = null
    private var currentJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun isRunning(): Boolean = currentJob?.isActive == true

    suspend fun runAgent(
        input: String,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val selection = modelSelector.getSelectedModel()
        if (selection == null) {
            onError("请先选择模型")
            return
        }

        val configJson = try {
            JSONObject(selection.provider.modelProviderConfig)
        } catch (e: Exception) {
            onError("模型配置错误")
            return
        }

        val baseUrl = configJson.optString("baseUrl", "https://api.openai.com")
        val apiKey = configJson.optString("apiKey", "")

        if (apiKey.isBlank()) {
            onError("API Key 未配置")
            return
        }

        try {
            val client = OpenAILLMClient(
                apiKey = apiKey,
                settings = OpenAIClientSettings(baseUrl = baseUrl)
            )

            val model = LLModel(
                provider = LLMProvider.OpenAI,
                id = selection.model.id,
                capabilities = listOf(
                    LLMCapability.Temperature,
                    LLMCapability.Tools,
                    LLMCapability.ToolChoice,
                    LLMCapability.Schema.JSON.Standard,
                    LLMCapability.Completion
                ),
                contextLength = 128000,
                maxOutputTokens = 4096
            )

            val skillsDir = File(context.filesDir, "skills").apply { mkdirs() }

            currentAgent = PhoneClawAgent.builder()
                .llmClient(client)
                .llmModel(model)
                .skillsDir(skillsDir)
                .build()

            val result = withContext(Dispatchers.IO) {
                currentAgent!!.runSuspend(input)
            }
            onResult(result)
        } catch (e: CancellationException) {
            // Agent was stopped
        } catch (e: Exception) {
            onError(e.message ?: "执行出错")
        }
    }

    fun stopAgent() {
        currentJob?.cancel()
        currentJob = null
        currentAgent = null
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/top/yudoge/phoneclaw/domain/AgentOrchestrator.kt
git commit -m "feat: add AgentOrchestrator"
```

---

### Task 11: Refactor ChatPresenter

**Files:**
- Modify: `app/src/main/java/top/yudoge/phoneclaw/ui/chat/ChatPresenter.kt`

- [ ] **Step 1: Rewrite ChatPresenter**

```kotlin
package top.yudoge.phoneclaw.ui.chat

import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import top.yudoge.phoneclaw.core.AgentStatusManager
import top.yudoge.phoneclaw.data.message.Message
import top.yudoge.phoneclaw.data.message.MessageRepository
import top.yudoge.phoneclaw.data.message.Role
import top.yudoge.phoneclaw.data.session.Session
import top.yudoge.phoneclaw.domain.AgentOrchestrator
import top.yudoge.phoneclaw.domain.ModelSelector
import top.yudoge.phoneclaw.domain.SessionManager
import top.yudoge.phoneclaw.ui.chat.model.MessageItem

class ChatPresenter(
    private val sessionManager: SessionManager,
    private val messageRepo: MessageRepository,
    private val modelSelector: ModelSelector,
    private val agentOrchestrator: AgentOrchestrator
) : ChatContract.Presenter {

    private var view: ChatContract.View? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentSession: Session? = null
    private var currentJob: Job? = null

    override fun attachView(view: ChatContract.View) {
        this.view = view
    }

    override fun detachView() {
        this.view = null
        scope.cancel()
    }

    override fun loadSession(sessionId: String?) {
        currentSession = sessionId?.let { sessionManager.getSession(it) }
            ?: sessionManager.createSession()

        currentSession?.let { session ->
            view?.showSessionTitle(session.title)

            val messages = messageRepo.getMessages(session.id)
            val items = messages.map { toMessageItem(it) }
            view?.showMessages(items)
        }

        updateModelSelector()
    }

    override fun createNewSession() {
        currentSession = sessionManager.createSession()
        view?.showMessages(emptyList())
        view?.showSessionTitle("新对话")
        updateModelSelector()
    }

    override fun sendMessage(content: String, images: List<Uri>?) {
        val session = currentSession ?: return

        val userMessage = Message(
            id = System.currentTimeMillis().toString(),
            sessionId = session.id,
            role = Role.USER,
            content = content,
            timestamp = System.currentTimeMillis()
        )

        messageRepo.saveMessage(session.id, userMessage)
        view?.appendMessage(toMessageItem(userMessage))

        runAgent(content, session)
    }

    private fun runAgent(input: String, session: Session) {
        view?.showThinking()
        view?.setSendButtonEnabled(false)
        view?.showStopButton()
        AgentStatusManager.setThinking("分析任务...")

        currentJob = scope.launch {
            agentOrchestrator.runAgent(
                input = input,
                onResult = { result ->
                    view?.hideThinking()

                    val agentMessage = Message(
                        id = System.currentTimeMillis().toString(),
                        sessionId = session.id,
                        role = Role.AGENT,
                        content = result,
                        timestamp = System.currentTimeMillis()
                    )
                    messageRepo.saveMessage(session.id, agentMessage)
                    view?.appendMessage(toMessageItem(agentMessage))

                    updateTitleIfNeeded(input, session)
                    resetUI()
                },
                onError = { error ->
                    view?.hideThinking()
                    view?.showError(error)
                    resetUI()
                }
            )
        }
    }

    private fun updateTitleIfNeeded(input: String, session: Session) {
        if (session.title == "新对话") {
            val newTitle = input.take(20) + if (input.length > 20) "..." else ""
            sessionManager.updateTitle(session.id, newTitle)
            view?.showSessionTitle(newTitle)
        }
    }

    private fun resetUI() {
        AgentStatusManager.reset()
        view?.setSendButtonEnabled(true)
        view?.hideStopButton()
    }

    override fun stopAgent() {
        currentJob?.cancel()
        currentJob = null
        agentOrchestrator.stopAgent()
        AgentStatusManager.reset()
        view?.hideThinking()
        view?.setSendButtonEnabled(true)
        view?.hideStopButton()
    }

    override fun deleteSession(sessionId: String) {
        sessionManager.deleteSession(sessionId)
        if (sessionId == currentSession?.id) {
            createNewSession()
        }
    }

    override fun renameSession(sessionId: String, newTitle: String) {
        sessionManager.updateTitle(sessionId, newTitle)
        if (sessionId == currentSession?.id) {
            view?.showSessionTitle(newTitle)
        }
    }

    override fun selectModel(modelIndex: Int) {
        modelSelector.selectModel(modelIndex)
        updateModelSelector()
    }

    override fun toggleInputMode() {}

    private fun updateModelSelector() {
        modelSelector.loadAvailableModels()
        val names = modelSelector.getDisplayNames()
        view?.showModelSelector(names, modelSelector.selectedIndex)
    }

    private fun toMessageItem(message: Message): MessageItem {
        return when (message.role) {
            Role.USER -> MessageItem.UserMessage(
                id = message.id,
                timestamp = message.timestamp,
                content = message.content
            )
            Role.AGENT -> MessageItem.AgentMessage(
                id = message.id,
                timestamp = message.timestamp,
                content = message.content
            )
            Role.TOOL -> MessageItem.ToolCallMessage(
                id = message.id,
                timestamp = message.timestamp,
                toolName = message.toolName ?: "",
                params = message.toolParams ?: "",
                result = message.toolResult,
                state = parseToolState(message.toolState, message.success)
            )
            Role.SKILL -> MessageItem.SkillCallMessage(
                id = message.id,
                timestamp = message.timestamp,
                skillName = message.toolName ?: "",
                state = parseSkillState(message.toolState, message.success)
            )
        }
    }

    private fun parseToolState(state: String?, success: Boolean): MessageItem.ToolCallMessage.CallState {
        return when {
            state == "running" -> MessageItem.ToolCallMessage.CallState.RUNNING
            success -> MessageItem.ToolCallMessage.CallState.SUCCESS
            else -> MessageItem.ToolCallMessage.CallState.FAILED
        }
    }

    private fun parseSkillState(state: String?, success: Boolean): MessageItem.SkillCallMessage.CallState {
        return when {
            state == "running" -> MessageItem.SkillCallMessage.CallState.RUNNING
            success -> MessageItem.SkillCallMessage.CallState.SUCCESS
            else -> MessageItem.SkillCallMessage.CallState.FAILED
        }
    }

    private fun kotlinx.coroutines.CoroutineScope.cancel() {
        kotlin.coroutines.coroutineContext[kotlinx.coroutines.Job]?.cancel()
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/top/yudoge/phoneclaw/ui/chat/ChatPresenter.kt
git commit -m "refactor: rewrite ChatPresenter with domain modules"
```

---

### Task 12: Update ChatActivity Dependencies

**Files:**
- Modify: `app/src/main/java/top/yudoge/phoneclaw/ui/chat/ChatActivity.kt`

- [ ] **Step 1: Update imports and presenter initialization**

Find the presenter initialization (around line 70) and replace:

```kotlin
import top.yudoge.phoneclaw.data.message.MessageRepositoryImpl
import top.yudoge.phoneclaw.data.session.SessionRepositoryImpl
import top.yudoge.phoneclaw.domain.AgentOrchestrator
import top.yudoge.phoneclaw.domain.ModelSelector
import top.yudoge.phoneclaw.domain.SessionManager
import top.yudoge.phoneclaw.llm.provider.ModelProviderRepositoryImpl
import top.yudoge.phoneclaw.llm.provider.ModelRepositoryImpl

// In onCreate, replace presenter initialization with:
val providerRepo = ModelProviderRepositoryImpl(this)
val modelRepo = ModelRepositoryImpl(this)
val sessionRepo = SessionRepositoryImpl(this)
val messageRepo = MessageRepositoryImpl(this)

val sessionManager = SessionManager(sessionRepo)
val modelSelector = ModelSelector(providerRepo, modelRepo)
val agentOrchestrator = AgentOrchestrator(this, modelSelector)

presenter = ChatPresenter(sessionManager, messageRepo, modelSelector, agentOrchestrator)
presenter.attachView(this)
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/top/yudoge/phoneclaw/ui/chat/ChatActivity.kt
git commit -m "refactor: update ChatActivity with new dependencies"
```

---

### Task 13: Build and Verify

- [ ] **Step 1: Clean build**

```bash
./gradlew clean compileDebugKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Final commit**

```bash
git add -A
git commit -m "feat: complete Phase 2 agent chat system refactor"
```
