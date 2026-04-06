package top.yudoge.phoneclaw.ui.chat

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.*
import org.json.JSONObject
import top.yudoge.phoneclaw.core.AgentStatusManager
import top.yudoge.phoneclaw.db.PhoneClawDbHelper
import top.yudoge.phoneclaw.llm.agent.PhoneClawAgent
import top.yudoge.phoneclaw.llm.provider.ModelProviderRepositoryImpl
import top.yudoge.phoneclaw.ui.chat.model.MessageItem
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import java.io.File
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class ChatPresenter(
    private val context: Context,
    private val dbHelper: PhoneClawDbHelper
) : ChatContract.Presenter {

    private var view: ChatContract.View? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var currentSessionId: String? = null
    private var currentJob: Job? = null
    private var currentAgent: PhoneClawAgent? = null

    override fun attachView(view: ChatContract.View) {
        this.view = view
    }

    override fun detachView() {
        this.view = null
        scope.cancel()
    }

    override fun loadSession(sessionId: String?) {
        currentSessionId = sessionId ?: createNewSessionInternal()

        val session = dbHelper.getSession(currentSessionId!!)
        if (session != null) {
            view?.showSessionTitle(session.title ?: "新对话")

            val messages = dbHelper.getMessages(currentSessionId!!)
            val messageItems = messages.map { record ->
                when (record.role) {
                    "user" -> MessageItem.UserMessage(
                        id = record.timestamp.toString(),
                        timestamp = record.timestamp,
                        content = record.content
                    )
                    "agent" -> MessageItem.AgentMessage(
                        id = record.timestamp.toString(),
                        timestamp = record.timestamp,
                        content = record.content
                    )
                    "tool" -> MessageItem.ToolCallMessage(
                        id = record.timestamp.toString(),
                        timestamp = record.timestamp,
                        toolName = record.toolName ?: "",
                        params = record.toolParams ?: "",
                        result = record.toolResult,
                        state = parseToolState(record.toolState, record.success)
                    )
                    "skill" -> MessageItem.SkillCallMessage(
                        id = record.timestamp.toString(),
                        timestamp = record.timestamp,
                        skillName = record.toolName ?: "",
                        state = parseSkillState(record.toolState, record.success)
                    )
                    else -> null
                }
            }.filterNotNull()

            view?.showMessages(messageItems)
        }
        
        updateModelSelector()
    }
    
    private fun updateModelSelector() {
        val providers = dbHelper.allModelProviders
        if (providers.isEmpty()) {
            view?.showModelSelector(emptyList(), 0)
            return
        }
        
        val allModels = mutableListOf<Pair<String, String>>()
        for (provider in providers) {
            val models = dbHelper.getModelsByProvider(provider.id)
            for (model in models) {
                allModels.add(Pair(provider.name, model.displayName))
            }
        }
        
        if (allModels.isNotEmpty()) {
            val modelNames = allModels.map { "${it.first}: ${it.second}" }
            view?.showModelSelector(modelNames, 0)
        } else {
            view?.showModelSelector(emptyList(), 0)
        }
    }

    override fun createNewSession() {
        currentSessionId = createNewSessionInternal()
        view?.showMessages(emptyList())
        view?.showSessionTitle("新对话")
        updateModelSelector()
    }

    private fun createNewSessionInternal(): String {
        val sessionId = java.util.UUID.randomUUID().toString()
        dbHelper.saveSession(
            sessionId,
            "新对话",
            System.currentTimeMillis(),
            null
        )
        return sessionId
    }

    override fun sendMessage(content: String, images: List<Uri>?) {
        val sessionId = currentSessionId ?: return

        val userMessage = MessageItem.UserMessage(
            id = System.currentTimeMillis().toString(),
            timestamp = System.currentTimeMillis(),
            content = content,
            images = images?.map { MessageItem.ImageInfo(it.toString()) }
        )
        view?.appendMessage(userMessage)

        dbHelper.saveMessage(sessionId, PhoneClawDbHelper.MessageRecord().apply {
            role = "user"
            this.content = content
            timestamp = userMessage.timestamp
        })

        startAgent(content, images)
    }

    private fun startAgent(input: String, images: List<Uri>?) {
        view?.showThinking()
        view?.setSendButtonEnabled(false)
        view?.showStopButton()
        AgentStatusManager.setThinking("分析任务...")

        currentJob = scope.launch {
            try {
                val agent = createAgent()
                if (agent == null) {
                    view?.hideThinking()
                    view?.showError("请先配置模型")
                    return@launch
                }
                
                currentAgent = agent

                withContext(Dispatchers.IO) {
                    agent.runSuspend(input)
                }.let { result ->
                    view?.hideThinking()
                    
                    val agentMessage = MessageItem.AgentMessage(
                        id = System.currentTimeMillis().toString(),
                        timestamp = System.currentTimeMillis(),
                        content = result
                    )
                    view?.appendMessage(agentMessage)

                    dbHelper.saveMessage(currentSessionId!!, PhoneClawDbHelper.MessageRecord().apply {
                        role = "agent"
                        this.content = result
                        timestamp = agentMessage.timestamp
                    })

                    val session = dbHelper.getSession(currentSessionId!!)
                    if (session != null && (session.title == "新对话" || session.title.isNullOrBlank())) {
                        val newTitle = input.take(20) + if (input.length > 20) "..." else ""
                        dbHelper.updateSessionTitle(currentSessionId!!, newTitle)
                        view?.showSessionTitle(newTitle)
                    }
                }

            } catch (e: CancellationException) {
                view?.hideThinking()
            } catch (e: Exception) {
                view?.hideThinking()
                view?.showError(e.message ?: "发生错误")
                e.printStackTrace()
            } finally {
                AgentStatusManager.reset()
                currentAgent = null
                view?.setSendButtonEnabled(true)
                view?.hideStopButton()
            }
        }
    }
    
    private fun createAgent(selectedModelIndex: Int = 0): PhoneClawAgent? {
        return try {
            val providers = dbHelper.allModelProviders
            
            if (providers.isEmpty()) {
                return null
            }
            
            val selectedProvider = if (selectedModelIndex >= 0 && selectedModelIndex < providers.size) {
                providers[selectedModelIndex]
            } else {
                providers.first()
            }
            
            val configJson = JSONObject(selectedProvider.modelProviderConfig)
            val baseUrl = configJson.optString("baseUrl", "https://api.openai.com")
            val apiKey = configJson.optString("apiKey", "")
            
            if (apiKey.isBlank()) {
                return null
            }
            
            val modelConfig = dbHelper.getDefaultModel()
            val modelId = modelConfig?.name ?: "gpt-4o"
            
            val client = OpenAILLMClient(
                apiKey = apiKey,
                settings = OpenAIClientSettings(baseUrl = baseUrl)
            )
            
            val model = LLModel(
                provider = LLMProvider.OpenAI,
                id = modelId,
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
            
            val skillsDir = File(context.filesDir, "skills")
            if (!skillsDir.exists()) {
                skillsDir.mkdirs()
            }
            
            PhoneClawAgent.builder()
                .llmClient(client)
                .llmModel(model)
                .skillsDir(skillsDir)
                .build()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun stopAgent() {
        currentJob?.cancel()
        currentJob = null
        currentAgent = null
        AgentStatusManager.reset()
        view?.hideThinking()
        view?.setSendButtonEnabled(true)
        view?.hideStopButton()
    }

    override fun deleteSession(sessionId: String) {
        dbHelper.deleteSession(sessionId)
        if (sessionId == currentSessionId) {
            createNewSession()
        }
    }

    override fun renameSession(sessionId: String, newTitle: String) {
        dbHelper.updateSessionTitle(sessionId, newTitle)
        if (sessionId == currentSessionId) {
            view?.showSessionTitle(newTitle)
        }
    }

    private var selectedProviderIndex: Int = 0
    
    override fun selectModel(modelIndex: Int) {
        selectedProviderIndex = modelIndex
        updateModelSelector()
    }

    override fun toggleInputMode() {
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
}
