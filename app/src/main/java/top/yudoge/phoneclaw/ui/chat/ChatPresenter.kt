package top.yudoge.phoneclaw.ui.chat

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yudoge.phoneclaw.app.AppContainer
import top.yudoge.phoneclaw.llm.callback.AgentRunCallBack
import top.yudoge.phoneclaw.llm.domain.PhoneClawAgentExecutor
import top.yudoge.phoneclaw.llm.domain.objects.Message
import top.yudoge.phoneclaw.llm.domain.objects.MessageRole
import top.yudoge.phoneclaw.llm.domain.objects.Model
import top.yudoge.phoneclaw.llm.domain.objects.Session
import top.yudoge.phoneclaw.llm.domain.objects.ToolCallInfo
import top.yudoge.phoneclaw.llm.domain.objects.ToolCallResult
import top.yudoge.phoneclaw.ui.chat.model.AgentMessageBuffer
import top.yudoge.phoneclaw.ui.chat.model.CallEventUtils
import top.yudoge.phoneclaw.ui.chat.model.MessageItem
import top.yudoge.phoneclaw.ui.floating.FloatingWindowStatusNotifier
import java.util.UUID

class ChatPresenter : ChatContract.Presenter {

    companion object {
        private const val TAG = "ChatPresenter"
        private const val DEFAULT_SESSION_TITLE = "New Chat"
        private const val SKILL_TOOL_NAME = "useSkill"
    }

    private var view: ChatContract.View? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentSession: Session? = null
    private var currentJob: Job? = null
    private var currentModel: Model? = null
    private var executor: PhoneClawAgentExecutor? = null

    private val agentMessageBuffer = AgentMessageBuffer()
    private var currentToolCallPosition: Int = -1
    private var currentToolUiMessageId: String? = null
    private var currentToolMessageId: String? = null
    private var currentToolArguments: String = ""
    private var currentToolDisplayName: String = ""
    private var currentToolIsSkill: Boolean = false
    private var awaitingReasoningAfterTool: Boolean = false

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
        currentSession = when {
            !sessionId.isNullOrBlank() -> {
                sessionFacade.getSessionById(sessionId)
                    ?: sessionFacade.getAllSessions().firstOrNull()
                    ?: sessionFacade.createSession(DEFAULT_SESSION_TITLE)
            }
            else -> {
                sessionFacade.getAllSessions().firstOrNull()
                    ?: sessionFacade.createSession(DEFAULT_SESSION_TITLE)
            }
        }

        currentSession?.let { session ->
            view?.showSessionTitle(session.title)

            val messages = sessionFacade.getMessages(session.id)
            val items = messages.map { toMessageItem(it) }
            view?.showMessages(items)

            executor = AppContainer.getInstance().createAgentExecutor(session)
        }

        updateModelSelector()
    }

    override fun createNewSession() {
        currentSession = sessionFacade.createSession(DEFAULT_SESSION_TITLE)
        view?.showMessages(emptyList())
        view?.showSessionTitle(DEFAULT_SESSION_TITLE)
        executor = currentSession?.let { AppContainer.getInstance().createAgentExecutor(it) }
        updateModelSelector()
    }

    override fun sendMessage(content: String, images: List<Uri>?) {
        val session = currentSession ?: return
        val model = currentModel ?: run {
            val models = modelProviderFacade.getAllModels()
            if (models.isEmpty()) {
                view?.showError("请先添加模型")
                return
            }
            models.first()
        }

        view?.appendMessage(
            MessageItem.UserMessage(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                content = content
            )
        )

        runAgent(content, session, model)
    }

    private fun runAgent(input: String, session: Session, model: Model) {
        view?.setSendButtonEnabled(false)
        view?.showStopButton()
        FloatingWindowStatusNotifier.notify(
            AppContainer.getInstance().appContext,
            FloatingWindowStatusNotifier.STATE_REASONING
        )

        agentMessageBuffer.start(System.currentTimeMillis().toString())
        currentToolMessageId = null
        currentToolUiMessageId = null
        currentToolArguments = ""
        currentToolDisplayName = ""
        currentToolIsSkill = false
        awaitingReasoningAfterTool = false

        val callback = object : AgentRunCallBack {
            override fun onAgentStart() {
                scope.launch(Dispatchers.Main) {
                    updateTitleIfNeeded(input, session)
                }
            }

            override fun onAgentError(e: Throwable) {
                Log.e(TAG, "onAgentError", e)
                scope.launch(Dispatchers.Main) {
                    view?.showError("错误: ${e.message}")
                    FloatingWindowStatusNotifier.notify(
                        AppContainer.getInstance().appContext,
                        FloatingWindowStatusNotifier.STATE_ERROR,
                        e.message
                    )
                    resetUI()
                }
            }

            override fun onAgentEnd() {
                scope.launch(Dispatchers.Main) {
                    onLLMStreamEnd()
                    FloatingWindowStatusNotifier.notify(
                        AppContainer.getInstance().appContext,
                        FloatingWindowStatusNotifier.STATE_COMPLETED
                    )
                    resetUI()
                }
            }

            override fun onReasoningStart() {
                scope.launch(Dispatchers.Main) {
                    view?.showThinking()
                    FloatingWindowStatusNotifier.notify(
                        AppContainer.getInstance().appContext,
                        FloatingWindowStatusNotifier.STATE_REASONING
                    )
                }
            }

            override fun onReasoningEnd() {
                scope.launch(Dispatchers.Main) {
                    view?.hideThinking()
                }
            }

            override fun onTextDelta(text: String) {
                scope.launch(Dispatchers.Main) {
                    onLLMTokenGenerated(text)
                }
            }

            override fun onTextDeltaComplete(text: String) {
                scope.launch(Dispatchers.Main) {
                    onLLMStreamComplete(text)
                }
            }

            override fun onToolCallStart(info: ToolCallInfo) {
                scope.launch(Dispatchers.Main) {
                    val isSkillCall = info.toolName == SKILL_TOOL_NAME
                    val now = System.currentTimeMillis()
                    val normalizedArguments = CallEventUtils.normalizeArguments(info.arguments)
                    val displayName = if (isSkillCall) {
                        CallEventUtils.extractSkillName(normalizedArguments)
                    } else {
                        info.toolName
                    }
                    val callMessage = if (isSkillCall) {
                        MessageItem.SkillCallMessage(
                            id = now.toString(),
                            timestamp = now,
                            skillName = displayName,
                            arguments = normalizedArguments,
                            state = MessageItem.SkillCallMessage.CallState.RUNNING
                        )
                    } else {
                        MessageItem.ToolCallMessage(
                            id = now.toString(),
                            timestamp = now,
                            toolName = displayName,
                            params = normalizedArguments,
                            state = MessageItem.ToolCallMessage.CallState.RUNNING
                        )
                    }
                    view?.appendMessage(callMessage)
                    currentToolCallPosition = (view?.getCurrentMessageCount() ?: 0) - 1
                    currentToolUiMessageId = callMessage.id
                    currentToolIsSkill = isSkillCall
                    currentToolArguments = normalizedArguments
                    currentToolDisplayName = displayName
                    currentToolMessageId = sessionFacade.addMessage(
                        Message(
                            id = UUID.randomUUID().toString(),
                            sessionId = session.id,
                            role = if (isSkillCall) MessageRole.SKILL else MessageRole.TOOL,
                            content = "",
                            timestamp = now,
                            toolName = displayName,
                            toolParams = normalizedArguments,
                            toolState = "running",
                            success = false
                        )
                    )
                    FloatingWindowStatusNotifier.notify(
                        AppContainer.getInstance().appContext,
                        if (isSkillCall) {
                            FloatingWindowStatusNotifier.STATE_SKILL_RUNNING
                        } else {
                            FloatingWindowStatusNotifier.STATE_TOOL_RUNNING
                        },
                        displayName
                    )
                }
            }

            override fun onToolCallEnd(result: ToolCallResult) {
                scope.launch(Dispatchers.Main) {
                    val toolState = if (result.success) {
                        MessageItem.ToolCallMessage.CallState.SUCCESS
                    } else {
                        MessageItem.ToolCallMessage.CallState.FAILED
                    }

                    updateToolCallUi(result, toolState)

                    val persistedArguments = CallEventUtils.normalizeArguments(
                        result.arguments.takeUnless { it.isNullOrBlank() } ?: currentToolArguments
                    )

                    currentToolMessageId?.let { messageId ->
                        sessionFacade.updateMessage(
                            Message(
                                id = messageId,
                                sessionId = session.id,
                                role = if (currentToolIsSkill) MessageRole.SKILL else MessageRole.TOOL,
                                content = "",
                                timestamp = System.currentTimeMillis(),
                                toolName = currentToolDisplayName.ifBlank { result.toolName },
                                toolParams = persistedArguments,
                                toolResult = result.result ?: result.error,
                                toolState = if (result.success) "success" else "failed",
                                success = result.success
                            )
                        )
                    }

                    FloatingWindowStatusNotifier.notify(
                        AppContainer.getInstance().appContext,
                        when {
                            currentToolIsSkill && result.success -> FloatingWindowStatusNotifier.STATE_SKILL_SUCCESS
                            currentToolIsSkill && !result.success -> FloatingWindowStatusNotifier.STATE_SKILL_FAILED
                            !currentToolIsSkill && result.success -> FloatingWindowStatusNotifier.STATE_TOOL_SUCCESS
                            else -> FloatingWindowStatusNotifier.STATE_TOOL_FAILED
                        },
                        currentToolDisplayName.ifBlank { result.toolName }
                    )

                    currentToolMessageId = null
                    currentToolUiMessageId = null
                    currentToolArguments = ""
                    currentToolDisplayName = ""
                    currentToolIsSkill = false
                    awaitingReasoningAfterTool = true
                }
            }
        }

        executor?.run(input, model, callback)
    }

    private fun updateTitleIfNeeded(input: String, session: Session) {
        if (session.title == DEFAULT_SESSION_TITLE) {
            val newTitle = input.take(20) + if (input.length > 20) "..." else ""
            sessionFacade.updateSessionTitle(session.id, newTitle)
            view?.showSessionTitle(newTitle)
        }
    }

    private fun resetUI() {
        scope.launch(Dispatchers.Main) {
            view?.setSendButtonEnabled(true)
            view?.hideStopButton()
            FloatingWindowStatusNotifier.notify(
                AppContainer.getInstance().appContext,
                FloatingWindowStatusNotifier.STATE_IDLE
            )
        }
    }

    override fun stopAgent() {
        currentJob?.cancel()
        currentJob = null
        scope.launch(Dispatchers.Main) {
            view?.hideThinking()
            view?.setSendButtonEnabled(true)
            view?.hideStopButton()
            FloatingWindowStatusNotifier.notify(
                AppContainer.getInstance().appContext,
                FloatingWindowStatusNotifier.STATE_IDLE
            )
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
        val models = modelProviderFacade.getAllModels()
        if (modelIndex in models.indices) {
            currentModel = models[modelIndex]
        }
        updateModelSelector()
    }

    override fun toggleInputMode() {}

    private fun updateModelSelector() {
        val models = modelProviderFacade.getAllModels()
        val names = models.map { it.displayName }
        val selectedIndex = if (currentModel != null) {
            models.indexOfFirst { it.id == currentModel?.id }.takeIf { it >= 0 } ?: 0
        } else {
            0
        }

        if (models.isNotEmpty() && currentModel == null) {
            currentModel = models[selectedIndex]
        }

        view?.showModelSelector(names, selectedIndex)
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
                skillName = if (message.toolName.isNullOrBlank() || message.toolName == SKILL_TOOL_NAME) {
                    CallEventUtils.extractSkillName(message.toolParams)
                } else {
                    message.toolName
                } ?: SKILL_TOOL_NAME,
                arguments = message.toolParams.orEmpty(),
                result = message.toolResult,
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

    private fun onLLMTokenGenerated(token: String) {
        if (awaitingReasoningAfterTool) {
            FloatingWindowStatusNotifier.notify(
                AppContainer.getInstance().appContext,
                FloatingWindowStatusNotifier.STATE_REASONING
            )
            awaitingReasoningAfterTool = false
        }
        val messageId = agentMessageBuffer.currentMessageId() ?: return
        val content = agentMessageBuffer.appendDelta(token)
        upsertAgentMessage(messageId, content)
    }

    private fun onLLMStreamComplete(fullText: String) {
        val messageId = agentMessageBuffer.currentMessageId() ?: return
        val content = agentMessageBuffer.complete(fullText)
        if (content.isNotEmpty()) {
            upsertAgentMessage(messageId, content)
        }
    }

    private fun upsertAgentMessage(messageId: String, content: String) {
        val lastPosition = view?.getCurrentMessageCount()?.minus(1) ?: -1
        if (lastPosition >= 0) {
            val lastItem = view?.getItemAt(lastPosition)
            if (lastItem is MessageItem.AgentMessage && lastItem.id == messageId) {
                view?.updateMessage(lastPosition, lastItem.copy(content = content))
                view?.scrollToBottom()
                return
            }
        }

        view?.appendMessage(
            MessageItem.AgentMessage(
                id = messageId,
                timestamp = System.currentTimeMillis(),
                content = content
            )
        )
        view?.scrollToBottom()
    }

    private fun onLLMStreamEnd() {
        val session = currentSession ?: return
        val finalContent = agentMessageBuffer.clearAndGetFinalContent()
        awaitingReasoningAfterTool = false

        if (session.title == DEFAULT_SESSION_TITLE && finalContent.isNotEmpty()) {
            val title = finalContent.take(20) + if (finalContent.length > 20) "..." else ""
            sessionFacade.updateSessionTitle(session.id, title)
        }
    }

    private fun findMessagePositionById(messageId: String): Int {
        val count = view?.getCurrentMessageCount() ?: return -1
        for (idx in 0 until count) {
            if (view?.getItemAt(idx)?.id == messageId) {
                return idx
            }
        }
        return -1
    }

    private suspend fun updateToolCallUi(
        result: ToolCallResult,
        toolState: MessageItem.ToolCallMessage.CallState
    ) {
        var toolPosition = currentToolUiMessageId?.let { findMessagePositionById(it) } ?: -1
        var retries = 0
        while (toolPosition < 0 && retries < 5) {
            delay(24)
            toolPosition = currentToolUiMessageId?.let { findMessagePositionById(it) } ?: -1
            retries++
        }

        if (toolPosition < 0) return

        val item = view?.getItemAt(toolPosition)
        when (item) {
            is MessageItem.ToolCallMessage -> {
                view?.updateMessage(
                    toolPosition,
                    item.copy(
                        result = result.result ?: result.error,
                        state = toolState
                    )
                )
            }

            is MessageItem.SkillCallMessage -> {
                val skillState = if (result.success) {
                    MessageItem.SkillCallMessage.CallState.SUCCESS
                } else {
                    MessageItem.SkillCallMessage.CallState.FAILED
                }
                view?.updateMessage(
                    toolPosition,
                    item.copy(state = skillState)
                )
            }

            else -> Unit
        }
    }
}
