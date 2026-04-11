package top.yudoge.phoneclaw.llm.domain

import android.util.Log
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.memory.ChatMemory
import dev.langchain4j.memory.chat.MessageWindowChatMemory
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.service.AiServices
import dev.langchain4j.service.TokenStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import top.yudoge.phoneclaw.emu.domain.EmuFacade
import top.yudoge.phoneclaw.llm.callback.AgentRunCallBack
import top.yudoge.phoneclaw.llm.domain.objects.Message
import top.yudoge.phoneclaw.llm.domain.objects.MessageRole
import top.yudoge.phoneclaw.llm.domain.objects.Model
import top.yudoge.phoneclaw.llm.domain.objects.Session
import top.yudoge.phoneclaw.llm.domain.objects.ToolCallInfo
import top.yudoge.phoneclaw.llm.domain.objects.ToolCallResult
import top.yudoge.phoneclaw.llm.integration.tools.PhoneEmulationTool
import top.yudoge.phoneclaw.llm.integration.tools.UseSkillTool
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class PhoneClawAgentExecutor(
    private val session: Session,
    private val skillFacade: SkillFacade,
    private val modelProviderFacade: ModelProviderFacade,
    private val sessionFacade: SessionFacade,
    private val emuFacade: EmuFacade
) {
    private val lock = ReentrantLock()
    private val chatMemory: ChatMemory
    private var isRunning = false

    private val phoneEmulationTool = PhoneEmulationTool()
    private val useSkillTool = UseSkillTool()

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "PhoneClawAgentExecutor"
    }

    init {
        chatMemory = initChatMemory()
    }

    private fun initChatMemory(): ChatMemory {
        val memory = MessageWindowChatMemory.withMaxMessages(20)
        
        val history = sessionFacade.getMessages(session.id)
        
        history.forEach { msg ->
            when (msg.role) {
                MessageRole.USER -> memory.add(UserMessage.from(msg.content))
                MessageRole.AGENT -> memory.add(AiMessage.from(msg.content))
                else -> {}
            }
        }
        
        return memory
    }

    fun flushSkills() {
        // Skills are loaded fresh on each run()
    }

    fun flushModelProviders() {
        // Model providers are fetched on demand in run()
    }

    fun run(prompt: String, model: Model, callback: AgentRunCallBack) {
        Log.d(TAG, "run: 请求执行, prompt=${prompt.take(50)}..., modelId=${model.id}")
        lock.withLock {
            if (isRunning) {
                Log.w(TAG, "run: Agent 已在运行中，拒绝新请求")
                callback.onAgentError(IllegalStateException("Agent is already running"))
                return
            }
            isRunning = true
        }

        coroutineScope.launch {
            try {
                executeAgent(prompt, model, callback)
            } catch (e: Exception) {
                Log.e(TAG, "run: 协程执行异常", e)
                callback.onAgentError(e)
                lock.withLock {
                    isRunning = false
                }
                callback.onAgentEnd()
            }
        }
    }

    private suspend fun executeAgent(prompt: String, model: Model, callback: AgentRunCallBack) {
        Log.d(TAG, "executeAgent: 开始执行, modelId=${model.id}, providerId=${model.providerId}")
        callback.onAgentStart()

        val provider = modelProviderFacade.getProviderById(model.providerId)
        if (provider == null) {
            Log.e(TAG, "executeAgent: 未找到 Provider, providerId=${model.providerId}")
            callback.onAgentError(IllegalArgumentException("Provider not found for model ${model.id}"))
            return
        }

        val streamingModel: StreamingChatModel = provider.createStreamingChatModel(model.id, model.hasVisualCapability)!!
        Log.d(TAG, "executeAgent: 创建 StreamingChatModel 成功, supportsStreaming=true")
        
        val skills = skillFacade.getAllSkills()
        Log.d(TAG, "executeAgent: 加载了 ${skills.size} 个 skills")
        
        val systemPrompt = buildSystemPrompt(skills)
        
        chatMemory.add(SystemMessage.from(systemPrompt))
        chatMemory.add(UserMessage.from(prompt))
        saveMessageToSession(prompt, MessageRole.USER)

        callback.onReasoningStart()

        try {
            executeStreaming(prompt, streamingModel, callback)
        } catch (e: Exception) {
            Log.e(TAG, "executeAgent: 执行出错", e)
            callback.onAgentError(e)
            callback.onReasoningEnd()
        }
    }

    private fun executeStreaming(prompt: String, streamingModel: StreamingChatModel, callback: AgentRunCallBack) {
        Log.d(TAG, "executeStreaming: 开始执行流式对话, prompt=${prompt.take(100)}...")
        
        val agent = AiServices.builder(StreamingAgentInterface::class.java)
            .streamingChatModel(streamingModel)
            .chatMemory(chatMemory)
            .tools(phoneEmulationTool, useSkillTool)
            .build()
        
        val fullResponse = StringBuilder()
        
        Log.d(TAG, "executeStreaming: 开始调用 agent.chat()")
        
        agent.chat(prompt)
            .onPartialResponse { chunk ->
                Log.i(TAG, "onPartialResponse: 收到文本块 [${chunk.length}字符]: ${chunk.take(50)}...")
                fullResponse.append(chunk)
                callback.onTextDelta(chunk)
            }
            .beforeToolExecution { beforeToolExecution ->
                val request = beforeToolExecution.request()
                Log.i(TAG, "beforeToolExecution: 工具即将执行, toolName=${request.name()}, arguments=${request.arguments()}")
                callback.onToolCallStart(ToolCallInfo(
                    toolName = request.name(),
                    arguments = request.arguments()
                ))
            }
            .onToolExecuted { toolExecution ->
                val request = toolExecution.request()
                val result = toolExecution.result()
                Log.i(TAG, "onToolExecuted: 工具执行完成, toolName=${request.name()}, result=${result?.take(200)}...")
                callback.onToolCallEnd(ToolCallResult(
                    toolName = request.name(),
                    result = result,
                    success = true
                ))
            }
            .onCompleteResponse {
                Log.i(TAG, "onCompleteResponse: 对话完成, 总长度=${fullResponse.length}字符")
                callback.onTextDeltaComplete(fullResponse.toString())
                saveMessageToSession(fullResponse.toString(), MessageRole.AGENT)
                callback.onReasoningEnd()
                lock.withLock {
                    isRunning = false
                }
                Log.d(TAG, "onCompleteResponse: 执行结束")
                callback.onAgentEnd()
            }
            .onError { error ->
                Log.e(TAG, "onError: 对话出错", error)
                callback.onAgentError(error)
                callback.onReasoningEnd()
                lock.withLock {
                    isRunning = false
                }
                Log.d(TAG, "onError: 执行结束")
                callback.onAgentEnd()
            }
            .start()
        
        Log.d(TAG, "executeStreaming: TokenStream.start() 已调用")
    }

    private suspend fun executeNonStreaming(prompt: String, provider: top.yudoge.phoneclaw.llm.domain.objects.ModelProvider, model: Model, callback: AgentRunCallBack) {
        Log.d(TAG, "executeNonStreaming: 开始执行非流式对话")
        
        val chatModel = provider.createChatModel(model.id, model.hasVisualCapability)
        val agent = AiServices.builder(NonStreamingAgentInterface::class.java)
            .chatModel(chatModel)
            .chatMemory(chatMemory)
            .tools(phoneEmulationTool, useSkillTool)
            .build()
        
        Log.d(TAG, "executeNonStreaming: 调用 agent.chat()")
        val response = agent.chat(prompt)
        Log.d(TAG, "executeNonStreaming: 收到响应, 长度=${response.length}字符")
        
        callback.onTextDeltaComplete(response)
        saveMessageToSession(response, MessageRole.AGENT)
        callback.onReasoningEnd()
        lock.withLock {
            isRunning = false
        }
        Log.d(TAG, "executeNonStreaming: 执行结束")
        callback.onAgentEnd()
    }

    private fun buildSystemPrompt(skills: List<top.yudoge.phoneclaw.llm.domain.objects.Skill>): String {
        val skillSection = if (skills.isNotEmpty()) {
            val skillList = skills.joinToString("\n") { skill ->
                "- ${skill.name}: ${skill.description}"
            }
            """

## Available Skills
You have access to the following skills. Use the `useSkill` tool with the skill name to get detailed instructions:

$skillList

When you need to use a skill, call `useSkill` with the exact skill name to get its full documentation and capabilities."""
        } else {
            ""
        }

        return """You are PhoneClaw, an AI assistant that helps users automate tasks on their Android phone.
You have access to tools that can interact with the device through accessibility services.
Always be helpful and explain what you're doing when using tools.
If a tool fails, explain the error to the user and suggest alternatives.$skillSection"""
    }

    private fun saveMessageToSession(content: String, role: MessageRole) {
        val message = Message(
            id = UUID.randomUUID().toString(),
            sessionId = session.id,
            role = role,
            content = content,
            timestamp = System.currentTimeMillis()
        )
        sessionFacade.addMessage(message)
    }
    
    interface StreamingAgentInterface {
        fun chat(message: String): TokenStream
    }

    interface NonStreamingAgentInterface {
        fun chat(message: String): String
    }
}
