package top.yudoge.phoneclaw.ui.chat

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
import top.yudoge.phoneclaw.ui.chat.model.MessageItem
import top.yudoge.phoneclaw.ui.floating.FloatingWindowStatusNotifier
import java.util.UUID

class ChatPresenter : ChatContract.Presenter {
    
    companion object {
        private const val TAG = "ChatPresenter"
        private const val DEFAULT_SESSION_TITLE = "New Chat"
    }

    private var view: ChatContract.View? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentSession: Session? = null
    private var currentJob: Job? = null
    private var currentModel: Model? = null
    private var executor: PhoneClawAgentExecutor? = null
    
    private var currentAgentMessageId: String? = null
    private var currentAgentMessageContent = StringBuilder()
    private var currentToolCallPosition: Int = -1
    private var currentToolMessageId: String? = null

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
            ?: sessionFacade.createSession(DEFAULT_SESSION_TITLE)

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

        currentAgentMessageId = System.currentTimeMillis().toString()
        currentAgentMessageContent.clear()
        currentToolMessageId = null

        val callback = object : AgentRunCallBack {
            override fun onAgentStart() {
                Log.i(TAG, "onAgentStart: 回调触发")
                scope.launch(Dispatchers.Main) {
                    Log.i(TAG, "onAgentStart: 在主线程更新UI")
                    updateTitleIfNeeded(input, session)
                }
            }

            override fun onAgentError(e: Throwable) {
                Log.e(TAG, "onAgentError: 回调触发, error=${e.message}", e)
                scope.launch(Dispatchers.Main) {
                    Log.i(TAG, "onAgentError: 在主线程更新UI")
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
                Log.i(TAG, "onAgentEnd: 回调触发")
                scope.launch(Dispatchers.Main) {
                    Log.i(TAG, "onAgentEnd: 在主线程更新UI")
                    onLLMStreamEnd()
                    FloatingWindowStatusNotifier.notify(
                        AppContainer.getInstance().appContext,
                        FloatingWindowStatusNotifier.STATE_COMPLETED
                    )
                    resetUI()
                }
            }

            override fun onReasoningStart() {
                Log.i(TAG, "onReasoningStart: 回调触发")
                scope.launch(Dispatchers.Main) {
                    Log.i(TAG, "onReasoningStart: 在主线程更新UI, view=$view")
                    view?.showThinking()
                    FloatingWindowStatusNotifier.notify(
                        AppContainer.getInstance().appContext,
                        FloatingWindowStatusNotifier.STATE_REASONING
                    )
                }
            }

            override fun onReasoningEnd() {
                Log.i(TAG, "onReasoningEnd: 回调触发")
                scope.launch(Dispatchers.Main) {
                    Log.i(TAG, "onReasoningEnd: 在主线程更新UI, view=$view")
                    view?.hideThinking()
                }
            }

            override fun onTextDelta(text: String) {
                Log.i(TAG, "onTextDelta: 回调触发, text=${text.take(50)}...")
                scope.launch(Dispatchers.Main) {
                    Log.i(TAG, "onTextDelta: 在主线程更新UI, view=$view")
                    onLLMTokenGenerated(text)
                }
            }

            override fun onTextDeltaComplete(text: String) {
                Log.i(TAG, "onTextDeltaComplete: 回调触发, text长度=${text.length}")
                scope.launch(Dispatchers.Main) {
                    Log.i(TAG, "onTextDeltaComplete: 在主线程更新UI, view=$view")
                    onLLMTokenGenerated(text)
                }
            }

            override fun onToolCallStart(info: ToolCallInfo) {
                Log.i(TAG, "onToolCallStart: 回调触发, toolName=${info.toolName}")
                scope.launch(Dispatchers.Main) {
                    Log.i(TAG, "onToolCallStart: 在主线程更新UI")
                    val isSkillCall = info.toolName == "useSkill"
                    val now = System.currentTimeMillis()
                    val callMessage = if (isSkillCall) {
                        MessageItem.SkillCallMessage(
                            id = now.toString(),
                            timestamp = now,
                            skillName = info.arguments,
                            state = MessageItem.SkillCallMessage.CallState.RUNNING
                        )
                    } else {
                        MessageItem.ToolCallMessage(
                            id = now.toString(),
                            timestamp = now,
                            toolName = info.toolName,
                            params = info.arguments,
                            state = MessageItem.ToolCallMessage.CallState.RUNNING
                        )
                    }
                    view?.appendMessage(callMessage)
                    currentToolCallPosition = (view?.getCurrentMessageCount() ?: 0) - 1
                    currentToolMessageId = sessionFacade.addMessage(
                        Message(
                            id = UUID.randomUUID().toString(),
                            sessionId = session.id,
                            role = if (isSkillCall) MessageRole.SKILL else MessageRole.TOOL,
                            content = "",
                            timestamp = now,
                            toolName = info.toolName,
                            toolParams = info.arguments,
                            toolState = "running",
                            success = false
                        )
                    )
                    FloatingWindowStatusNotifier.notify(
                        AppContainer.getInstance().appContext,
                        FloatingWindowStatusNotifier.STATE_TOOL_RUNNING,
                        info.toolName
                    )
                }
            }

            override fun onToolCallEnd(result: ToolCallResult) {
                Log.i(TAG, "onToolCallEnd: 回调触发, toolName=${result.toolName}, success=${result.success}")
                scope.launch(Dispatchers.Main) {
                    Log.i(TAG, "onToolCallEnd: 在主线程更新UI")
                    val state = if (result.success) {
                        MessageItem.ToolCallMessage.CallState.SUCCESS
                    } else {
                        MessageItem.ToolCallMessage.CallState.FAILED
                    }
                    
                    if (currentToolCallPosition >= 0) {
                        val item = view?.getItemAt(currentToolCallPosition)
                        when (item) {
                            is MessageItem.ToolCallMessage -> {
                                val updated = item.copy(
                                    result = result.result ?: result.error,
                                    state = state
                                )
                                view?.updateMessage(currentToolCallPosition, updated)
                            }
                            is MessageItem.SkillCallMessage -> {
                                val skillState = if (result.success) {
                                    MessageItem.SkillCallMessage.CallState.SUCCESS
                                } else {
                                    MessageItem.SkillCallMessage.CallState.FAILED
                                }
                                view?.updateMessage(
                                    currentToolCallPosition,
                                    item.copy(state = skillState)
                                )
                            }
                            else -> Unit
                        }
                    }
                    currentToolMessageId?.let { messageId ->
                        sessionFacade.updateMessage(
                            Message(
                                id = messageId,
                                sessionId = session.id,
                                role = if (result.toolName == "useSkill") MessageRole.SKILL else MessageRole.TOOL,
                                content = "",
                                timestamp = System.currentTimeMillis(),
                                toolName = result.toolName,
                                toolParams = "",
                                toolResult = result.result ?: result.error,
                                toolState = if (result.success) "success" else "failed",
                                success = result.success
                            )
                        )
                    }
                    currentToolMessageId = null
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
    
    private fun onLLMTokenGenerated(token: String) {
        Log.v(TAG, "onLLMTokenGenerated: token=${token.take(30)}..., currentAgentMessageId=$currentAgentMessageId")
        currentAgentMessageContent.append(token)
        
        if (currentAgentMessageId != null) {
            val lastPosition = view?.getCurrentMessageCount()?.minus(1) ?: -1
            Log.v(TAG, "onLLMTokenGenerated: lastPosition=$lastPosition")
            
            if (lastPosition >= 0) {
                val lastItem = view?.getItemAt(lastPosition)
                Log.v(TAG, "onLLMTokenGenerated: lastItem type=${lastItem?.javaClass?.simpleName}")
                
                if (lastItem is MessageItem.AgentMessage && lastItem.id == currentAgentMessageId) {
                    val updated = lastItem.copy(content = currentAgentMessageContent.toString())
                    Log.v(TAG, "onLLMTokenGenerated: 更新现有消息, content长度=${currentAgentMessageContent.length}")
                    view?.updateMessage(lastPosition, updated)
                    view?.scrollToBottom()
                    return
                }
            }
            
            val agentMessage = MessageItem.AgentMessage(
                id = currentAgentMessageId!!,
                timestamp = System.currentTimeMillis(),
                content = currentAgentMessageContent.toString()
            )
            Log.d(TAG, "onLLMTokenGenerated: 追加新消息, content长度=${currentAgentMessageContent.length}")
            view?.appendMessage(agentMessage)
            view?.scrollToBottom()
        } else {
            Log.w(TAG, "onLLMTokenGenerated: currentAgentMessageId 为 null, 跳过")
        }
    }
    
    private fun onLLMStreamEnd() {
        val session = currentSession ?: return

        currentAgentMessageId = null
        val finalContent = currentAgentMessageContent.toString()
        currentAgentMessageContent.clear()

        if (session.title == DEFAULT_SESSION_TITLE && finalContent.isNotEmpty()) {
            val title = finalContent.take(20) +
                if (finalContent.length > 20) "..." else ""
            sessionFacade.updateSessionTitle(session.id, title)
        }
    }
}
