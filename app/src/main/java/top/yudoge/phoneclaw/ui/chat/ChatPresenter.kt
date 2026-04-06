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
}
