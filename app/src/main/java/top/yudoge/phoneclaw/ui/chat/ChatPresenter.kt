package top.yudoge.phoneclaw.ui.chat

import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import top.yudoge.phoneclaw.core.AgentStatusManager
import top.yudoge.phoneclaw.data.message.Message
import top.yudoge.phoneclaw.data.message.MessageRepository
import top.yudoge.phoneclaw.data.message.Role
import top.yudoge.phoneclaw.data.session.Session
import top.yudoge.phoneclaw.domain.AgentCallback
import top.yudoge.phoneclaw.llm.agent.AgentOrchestrator
import top.yudoge.phoneclaw.domain.ModelSelector
import top.yudoge.phoneclaw.domain.SessionManager
import top.yudoge.phoneclaw.ui.chat.model.MessageItem

class ChatPresenter(
    private val sessionManager: SessionManager,
    private val messageRepo: MessageRepository,
    private val modelSelector: ModelSelector,
    private val agentOrchestrator: AgentOrchestrator
) : ChatContract.Presenter, AgentCallback {

    private var view: ChatContract.View? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentSession: Session? = null
    private var currentJob: Job? = null
    
    private var currentAgentMessageId: String? = null
    private var currentAgentMessageContent = StringBuilder()
    private var currentToolCallPosition: Int = -1

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
        view?.setSendButtonEnabled(false)
        view?.showStopButton()

        currentJob = scope.launch {
            try {
                agentOrchestrator.runAgent(
                    input = input,
                    callback = this@ChatPresenter,
                    onResult = { },
                    onError = { }
                )
            } catch (e: Exception) {
                onAgentError(e.message ?: "Agent执行出错")
            }
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
        scope.launch(Dispatchers.Main) {
            view?.setSendButtonEnabled(true)
            view?.hideStopButton()
        }
    }

    override fun stopAgent() {
        currentJob?.cancel()
        currentJob = null
        agentOrchestrator.stopAgent()
        AgentStatusManager.setCompleted(false)
        scope.launch(Dispatchers.Main) {
            view?.hideThinking()
            view?.setSendButtonEnabled(true)
            view?.hideStopButton()
        }
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
    
    override fun onLLMStreamStart() {
        val session = currentSession ?: return
        println("[ChatPresenter] onLLMStreamStart called")
        
        currentAgentMessageId = System.currentTimeMillis().toString()
        currentAgentMessageContent.clear()
        
        val agentMessage = MessageItem.AgentMessage(
            id = currentAgentMessageId!!,
            timestamp = System.currentTimeMillis(),
            content = ""
        )
        
        println("[ChatPresenter] appendMessage: ${agentMessage.id}")
        scope.launch(Dispatchers.Main) {
            view?.appendMessage(agentMessage)
        }
        
        AgentStatusManager.setThinking()
    }
    
    override fun onLLMTokenGenerated(token: String) {
        currentAgentMessageContent.append(token)
        println("[ChatPresenter] onLLMTokenGenerated: '$token', total length: ${currentAgentMessageContent.length}")
        scope.launch(Dispatchers.Main) {
            view?.updateAgentMessageContent(currentAgentMessageContent.toString())
        }
    }
    
    override fun onLLMStreamEnd() {
        println("[ChatPresenter] onLLMStreamEnd, content: ${currentAgentMessageContent.toString().take(100)}")
        val session = currentSession ?: return
        
        currentAgentMessageId?.let { id ->
            val message = Message(
                id = id,
                sessionId = session.id,
                role = Role.AGENT,
                content = currentAgentMessageContent.toString(),
                timestamp = System.currentTimeMillis()
            )
            messageRepo.saveMessage(session.id, message)
        }
        
        currentAgentMessageId = null
        currentAgentMessageContent.clear()
    }
    
    override fun onToolCallStart(toolName: String, params: String) {
        val session = currentSession ?: return
        
        val toolMessage = MessageItem.ToolCallMessage(
            id = System.currentTimeMillis().toString(),
            timestamp = System.currentTimeMillis(),
            toolName = toolName,
            params = params,
            state = MessageItem.ToolCallMessage.CallState.RUNNING
        )
        
        val position = view?.getCurrentMessageCount() ?: 0
        currentToolCallPosition = position
        
        scope.launch(Dispatchers.Main) {
            view?.appendMessage(toolMessage)
        }
        
        AgentStatusManager.setToolCalling(toolName, AgentStatusManager.CallState.RUNNING)
        
        val message = Message(
            id = toolMessage.id,
            sessionId = session.id,
            role = Role.TOOL,
            content = "",
            timestamp = toolMessage.timestamp,
            toolName = toolName,
            toolParams = params,
            toolState = "running"
        )
        messageRepo.saveMessage(session.id, message)
    }
    
    override fun onToolCallEnd(toolName: String, result: String, success: Boolean) {
        if (currentToolCallPosition >= 0) {
            val session = currentSession ?: return
            val item = view?.getItemAt(currentToolCallPosition) as? MessageItem.ToolCallMessage ?: return
            
            val updatedMessage = item.copy(
                result = result,
                state = if (success) MessageItem.ToolCallMessage.CallState.SUCCESS 
                        else MessageItem.ToolCallMessage.CallState.FAILED
            )
            
            scope.launch(Dispatchers.Main) {
                view?.updateMessage(currentToolCallPosition, updatedMessage)
            }
            
            val message = Message(
                id = updatedMessage.id,
                sessionId = session.id,
                role = Role.TOOL,
                content = "",
                timestamp = updatedMessage.timestamp,
                toolName = toolName,
                toolParams = updatedMessage.params,
                toolResult = result,
                toolState = if (success) "success" else "failed",
                success = success
            )
            messageRepo.updateMessage(session.id, message)
        }
        
        AgentStatusManager.setToolCalling(toolName, if (success) AgentStatusManager.CallState.SUCCESS else AgentStatusManager.CallState.FAILED)
        
        currentToolCallPosition = -1
    }
    
    override fun onSkillCallStart(skillName: String) {
        val session = currentSession ?: return
        
        val skillMessage = MessageItem.SkillCallMessage(
            id = System.currentTimeMillis().toString(),
            timestamp = System.currentTimeMillis(),
            skillName = skillName,
            state = MessageItem.SkillCallMessage.CallState.RUNNING
        )
        
        scope.launch(Dispatchers.Main) {
            view?.appendMessage(skillMessage)
        }
        
        AgentStatusManager.setSkillCalling(skillName, AgentStatusManager.CallState.RUNNING)
        
        val message = Message(
            id = skillMessage.id,
            sessionId = session.id,
            role = Role.SKILL,
            content = "",
            timestamp = skillMessage.timestamp,
            toolName = skillName,
            toolState = "running"
        )
        messageRepo.saveMessage(session.id, message)
    }
    
    override fun onSkillCallEnd(skillName: String, success: Boolean) {
        val session = currentSession ?: return
        val count = view?.getCurrentMessageCount() ?: 0
        if (count > 0) {
            val item = view?.getItemAt(count - 1) as? MessageItem.SkillCallMessage ?: return
            
            val updatedMessage = item.copy(
                state = if (success) MessageItem.SkillCallMessage.CallState.SUCCESS 
                        else MessageItem.SkillCallMessage.CallState.FAILED
            )
            
            scope.launch(Dispatchers.Main) {
                view?.updateMessage(count - 1, updatedMessage)
            }
            
            val message = Message(
                id = updatedMessage.id,
                sessionId = session.id,
                role = Role.SKILL,
                content = "",
                timestamp = updatedMessage.timestamp,
                toolName = skillName,
                toolState = if (success) "success" else "failed",
                success = success
            )
            messageRepo.updateMessage(session.id, message)
        }
        
        AgentStatusManager.setSkillCalling(skillName, if (success) AgentStatusManager.CallState.SUCCESS else AgentStatusManager.CallState.FAILED)
    }
    
    override fun onAgentComplete() {
        val session = currentSession ?: return
        
        AgentStatusManager.setCompleted(true)
        
        if (session.title == "新对话" && currentAgentMessageContent.isNotEmpty()) {
            val title = currentAgentMessageContent.toString().take(20) + 
                       if (currentAgentMessageContent.length > 20) "..." else ""
            sessionManager.updateTitle(session.id, title)
        }
        
        resetUI()
    }
    
    override fun onAgentError(error: String) {
        AgentStatusManager.setCompleted(false)
        scope.launch(Dispatchers.Main) {
            view?.showError(error)
            resetUI()
        }
    }
}
