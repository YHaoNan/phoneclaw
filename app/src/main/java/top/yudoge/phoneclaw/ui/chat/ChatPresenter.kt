package top.yudoge.phoneclaw.ui.chat

import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import top.yudoge.phoneclaw.app.AppContainer
import top.yudoge.phoneclaw.llm.domain.objects.Message
import top.yudoge.phoneclaw.llm.domain.objects.MessageRole
import top.yudoge.phoneclaw.llm.domain.objects.Session
import top.yudoge.phoneclaw.ui.chat.model.MessageItem

class ChatPresenter : ChatContract.Presenter {

    private var view: ChatContract.View? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentSession: Session? = null
    private var currentJob: Job? = null
    
    private var currentAgentMessageId: String? = null
    private var currentAgentMessageContent = StringBuilder()
    private var currentToolCallPosition: Int = -1

    private val sessionFacade by lazy { AppContainer.getInstance().sessionFacade }
    private val modelProviderFacade by lazy { AppContainer.getInstance().modelProviderFacade }

    override fun attachView(view: ChatContract.View) {
        this.view = view
    }

    override fun detachView() {
        this.view = null
        scope.cancel()
    }

    override fun loadSession(sessionId: String?) {
        currentSession = sessionId?.let { sessionFacade.getSessionById(it) }
            ?: sessionFacade.createSession()

        currentSession?.let { session ->
            view?.showSessionTitle(session.title)

            val messages = sessionFacade.getMessages(session.id)
            val items = messages.map { toMessageItem(it) }
            view?.showMessages(items)
        }

        updateModelSelector()
    }

    override fun createNewSession() {
        currentSession = sessionFacade.createSession()
        view?.showMessages(emptyList())
        view?.showSessionTitle("新对话")
        updateModelSelector()
    }

    override fun sendMessage(content: String, images: List<Uri>?) {
        val session = currentSession ?: return

        val userMessage = Message(
            id = System.currentTimeMillis().toString(),
            sessionId = session.id,
            role = MessageRole.USER,
            content = content,
            timestamp = System.currentTimeMillis()
        )

        sessionFacade.addMessage(userMessage)
        view?.appendMessage(toMessageItem(userMessage))

        runAgent(content, session)
    }

    private fun runAgent(input: String, session: Session) {
        view?.setSendButtonEnabled(false)
        view?.showStopButton()

        currentJob = scope.launch {
            try {
                updateTitleIfNeeded(input, session)
                onLLMStreamStart()
                onLLMTokenGenerated("This is a placeholder response. Agent integration is being refactored.")
                onLLMStreamEnd()
                onAgentComplete()
            } catch (e: Exception) {
                onAgentError(e.message ?: "Agent执行出错")
            }
        }
    }

    private fun updateTitleIfNeeded(input: String, session: Session) {
        if (session.title == "新对话") {
            val newTitle = input.take(20) + if (input.length > 20) "..." else ""
            sessionFacade.updateSessionTitle(session.id, newTitle)
            view?.showSessionTitle(newTitle)
        }
    }

    private fun resetUI() {
        scope.launch(Dispatchers.Main) {
            view?.setSendButtonEnabled(true)
            view?.hideStopButton()
        }
    }

    override fun stopAgent() {
        currentJob?.cancel()
        currentJob = null
        scope.launch(Dispatchers.Main) {
            view?.hideThinking()
            view?.setSendButtonEnabled(true)
            view?.hideStopButton()
        }
    }

    override fun deleteSession(sessionId: String) {
        sessionFacade.deleteSession(sessionId)
        if (sessionId == currentSession?.id) {
            createNewSession()
        }
    }

    override fun renameSession(sessionId: String, newTitle: String) {
        sessionFacade.updateSessionTitle(sessionId, newTitle)
        if (sessionId == currentSession?.id) {
            view?.showSessionTitle(newTitle)
        }
    }

    override fun selectModel(modelIndex: Int) {
        updateModelSelector()
    }

    override fun toggleInputMode() {}

    private fun updateModelSelector() {
        val models = modelProviderFacade.getAllModels()
        val names = models.map { it.displayName }
        view?.showModelSelector(names, 0)
    }

    private fun toMessageItem(message: Message): MessageItem {
        return when (message.role) {
            MessageRole.USER -> MessageItem.UserMessage(
                id = message.id,
                timestamp = message.timestamp,
                content = message.content
            )
            MessageRole.AGENT -> MessageItem.AgentMessage(
                id = message.id,
                timestamp = message.timestamp,
                content = message.content
            )
            MessageRole.TOOL -> MessageItem.ToolCallMessage(
                id = message.id,
                timestamp = message.timestamp,
                toolName = message.toolName ?: "",
                params = message.toolParams ?: "",
                result = message.toolResult,
                state = parseToolState(message.toolState, message.success)
            )
            MessageRole.SKILL -> MessageItem.SkillCallMessage(
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
    
    private fun onLLMStreamStart() {
        val session = currentSession ?: return
        
        currentAgentMessageId = System.currentTimeMillis().toString()
        currentAgentMessageContent.clear()
        
        val agentMessage = MessageItem.AgentMessage(
            id = currentAgentMessageId!!,
            timestamp = System.currentTimeMillis(),
            content = ""
        )
        
        scope.launch(Dispatchers.Main) {
            view?.appendMessage(agentMessage)
        }
    }
    
    private fun onLLMTokenGenerated(token: String) {
        currentAgentMessageContent.append(token)
        scope.launch(Dispatchers.Main) {
            view?.updateAgentMessageContent(currentAgentMessageContent.toString())
        }
    }
    
    private fun onLLMStreamEnd() {
        val session = currentSession ?: return
        
        currentAgentMessageId?.let { id ->
            val message = Message(
                id = id,
                sessionId = session.id,
                role = MessageRole.AGENT,
                content = currentAgentMessageContent.toString(),
                timestamp = System.currentTimeMillis()
            )
            sessionFacade.addMessage(message)
        }
        
        currentAgentMessageId = null
        currentAgentMessageContent.clear()
    }
    
    private fun onAgentComplete() {
        val session = currentSession ?: return
        
        if (session.title == "新对话" && currentAgentMessageContent.isNotEmpty()) {
            val title = currentAgentMessageContent.toString().take(20) + 
                       if (currentAgentMessageContent.length > 20) "..." else ""
            sessionFacade.updateSessionTitle(session.id, title)
        }
        
        resetUI()
    }
    
    private fun onAgentError(error: String) {
        scope.launch(Dispatchers.Main) {
            view?.showError(error)
            resetUI()
        }
    }
}
