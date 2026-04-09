package top.yudoge.phoneclaw.llm.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeExecuteTool
import ai.koog.agents.core.dsl.extension.nodeLLMSendToolResult
import ai.koog.agents.core.dsl.extension.onAssistantMessage
import ai.koog.agents.core.dsl.extension.onToolCall
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.ResponseMetaInfo
import ai.koog.prompt.streaming.StreamFrame
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import top.yudoge.phoneclaw.domain.AgentCallback
import top.yudoge.phoneclaw.llm.skills.FileBasedSkillRepository
import top.yudoge.phoneclaw.llm.skills.Skill
import top.yudoge.phoneclaw.llm.skills.SkillRepository
import top.yudoge.phoneclaw.llm.tools.PhoneEmulationTool
import top.yudoge.phoneclaw.llm.tools.UseSkillTool
import java.io.File

class PhoneClawAgent private constructor(
    private val agent: AIAgent<String, String>,
    private val phoneEmulationTool: PhoneEmulationTool,
    private val useSkillTool: UseSkillTool
) {
    companion object {
        class Builder {
            private var llmClient: LLMClient? = null
            private var llmModel: LLModel? = null
            private var skillsDir: File? = null
            private var customSystemPrompt: String? = null
            private var maxIterations: Int = 1000
            private var temperature: Double = 0.7
            private var callback: AgentCallback? = null

            fun llmClient(client: LLMClient) = apply { this.llmClient = client }
            fun llmModel(model: LLModel) = apply { this.llmModel = model }
            fun skillsDir(dir: File) = apply { this.skillsDir = dir }
            fun systemPrompt(prompt: String) = apply { this.customSystemPrompt = prompt }
            fun maxIterations(iterations: Int) = apply { this.maxIterations = iterations }
            fun temperature(temp: Double) = apply { this.temperature = temp }
            fun callback(cb: AgentCallback) = apply { this.callback = cb }

            private fun buildSystemPrompt(skills: List<Skill>): String {
                val skillsList = if (skills.isEmpty()) {
                    "No skills are currently available."
                } else {
                    skills.joinToString("\n") { skill ->
                        "- **${skill.name}**: ${skill.description}"
                    }
                }
                
                val exampleSkill = skills.firstOrNull()?.name ?: "skill_name"

                return """
You are PhoneClawAgent, an AI assistant that can control Android phones through Lua scripts.

## Your Capabilities

1. **Phone Control**: Execute Lua scripts on a connected Android phone using the phone control tools.
2. **Skills**: Use skills to extend your capabilities.

## Available Skills

Use the `useSkill` tool to call any of these skills:

$skillsList

## How to Work

1. Understand the user's request
2. Use the `useSkill` tool to load relevant skills when needed (e.g., useSkill("$exampleSkill"))
3. Execute phone operations step by step using the executeScript tool
4. Report results clearly

## Available Tools

- **useSkill(skillName)**: Load a skill to get its documentation and capabilities
- **executeScript(script)**: Execute a Lua script on the phone. The `emu` object is automatically available in the script context.

## Best Practices

1. Always check current screen state before taking actions
2. Add appropriate delays between operations (use emu:waitMS)
3. Handle errors gracefully and report issues clearly
4. When you need specific capabilities, first call useSkill with the appropriate skill name to get the detailed API documentation
""".trimIndent()
            }

            fun build(): PhoneClawAgent {
                val client = llmClient ?: throw IllegalStateException("LLM client is required")
                val model = llmModel ?: throw IllegalStateException("LLM model is required")

                val executor = MultiLLMPromptExecutor(model.provider to client)

                val phoneTool = PhoneEmulationTool()
                
                val skillRepo: SkillRepository = if (skillsDir != null) {
                    FileBasedSkillRepository(skillsDir!!)
                } else {
                    object : SkillRepository {
                        override fun loadSkills(): List<Skill> = emptyList()
                        override fun getSkill(name: String): Skill? = null
                        override fun addSkill(skill: Skill) {}
                        override fun updateSkill(skill: Skill) {}
                        override fun deleteSkill(skill: Skill) {}
                        override fun deleteSkillByName(name: String) {}
                        override fun skillExists(name: String): Boolean = false
                    }
                }
                
                val skills = skillRepo.loadSkills()
                val systemPrompt = customSystemPrompt ?: buildSystemPrompt(skills)
                val skillTool = UseSkillTool(skillRepo)

                log("Building PhoneClawAgent with model: ${model.id}")
                log("Tools: PhoneEmulationTool, UseSkillTool")
                log("Available skills: ${skills.map { it.name }}")
                log("Using STREAMING strategy for LLM responses")

                val toolRegistry = ToolRegistry {
                    tools(phoneTool.asTools())
                    tools(skillTool.asTools())
                }

                val streamingStrategy = createStreamingStrategy()

                val agent = AIAgent(
                    promptExecutor = executor,
                    llmModel = model,
                    systemPrompt = systemPrompt,
                    toolRegistry = toolRegistry,
                    maxIterations = maxIterations,
                    temperature = temperature,
                    strategy = streamingStrategy,
                ) {
                    handleEvents {
                        println("[PhoneClawAgent] handleEvents block registered")

                        onLLMStreamingStarting {
                            println("[PhoneClawAgent] onLLMStreamingStarting callback triggered")
                            callback?.onLLMStreamStart()
                        }
                        
                        onLLMStreamingFrameReceived { eventContext ->
                            println("[PhoneClawAgent] onLLMStreamingFrameReceived: ${eventContext.streamFrame::class.simpleName}")
                            val frame = eventContext.streamFrame
                            if (frame is StreamFrame.TextDelta) {
                                val text = frame.text
                                println("[PhoneClawAgent] TextDelta: '$text'")
                                if (text.isNotEmpty()) {
                                    callback?.onLLMTokenGenerated(text)
                                }
                            }
                        }
                        
                        onLLMStreamingCompleted {
                            println("[PhoneClawAgent] onLLMStreamingCompleted")
                            callback?.onLLMStreamEnd()
                        }
                        
                        onLLMStreamingFailed { eventContext ->
                            println("[PhoneClawAgent] onLLMStreamingFailed: ${eventContext.error.message}")
                            callback?.onAgentError(eventContext.error.message ?: "LLM streaming failed")
                        }
                        
                        onToolCallStarting { eventContext ->
                            val toolName = eventContext.toolName
                            val params = eventContext.toolArgs.toString()
                            
                            if (toolName == "useSkill") {
                                val skillName = try {
                                    org.json.JSONObject(params).optString("skillName", "unknown")
                                } catch (e: Exception) {
                                    "unknown"
                                }
                                callback?.onSkillCallStart(skillName)
                            } else {
                                callback?.onToolCallStart(toolName, params)
                            }
                        }
                        
                        onToolCallCompleted { eventContext ->
                            val toolName = eventContext.toolName
                            val result = eventContext.toolResult?.toString() ?: ""
                            val success = eventContext.toolResult != null
                            
                            if (toolName == "useSkill") {
                                callback?.onSkillCallEnd("useSkill", success)
                            } else {
                                callback?.onToolCallEnd(toolName, result, success)
                            }
                        }
                        
                        onToolCallFailed { eventContext ->
                            val toolName = eventContext.toolName
                            
                            if (toolName == "useSkill") {
                                callback?.onSkillCallEnd("useSkill", false)
                            } else {
                                callback?.onToolCallEnd(toolName, eventContext.message, false)
                            }
                        }
                        
                        onAgentCompleted {
                            callback?.onAgentComplete()
                        }
                        
                        onAgentExecutionFailed { eventContext ->
                            callback?.onAgentError(eventContext.throwable.message ?: "Agent execution failed")
                        }
                    }
                }

                return PhoneClawAgent(agent, phoneTool, skillTool)
            }

            private fun createStreamingStrategy(): AIAgentGraphStrategy<String, String> {
                return strategy<String, String>("streaming_agent") {
                    val streamingRequestNode by node<String, Message.Response>("streamingRequest") { input ->
                        llm.writeSession {
                            appendPrompt { user(input) }
                            val stream = requestLLMStreaming()
                            
                            var assistantContent = StringBuilder()
                            var toolCalls = mutableListOf<Message.Tool.Call>()
                            
                            stream.collect { frame ->
                                when (frame) {
                                    is StreamFrame.TextDelta -> {
                                        assistantContent.append(frame.text)
                                    }
                                    is StreamFrame.ToolCallComplete -> {
                                        val toolCall = Message.Tool.Call(
                                            id = frame.id ?: "",
                                            tool = frame.name,
                                            content = frame.content,
                                            metaInfo = ResponseMetaInfo.Empty
                                        )
                                        toolCalls.add(toolCall)
                                    }
                                    is StreamFrame.End -> { }
                                    else -> { }
                                }
                            }
                            
                            when {
                                toolCalls.isNotEmpty() -> {
                                    val toolCall = toolCalls.first()
                                    appendPrompt { message(toolCall) }
                                    toolCall
                                }
                                assistantContent.isNotEmpty() -> {
                                    val assistant = Message.Assistant(
                                        content = assistantContent.toString(),
                                        metaInfo = ResponseMetaInfo.Empty
                                    )
                                    appendPrompt { message(assistant) }
                                    assistant
                                }
                                else -> {
                                    val assistant = Message.Assistant(
                                        content = "",
                                        metaInfo = ResponseMetaInfo.Empty
                                    )
                                    appendPrompt { message(assistant) }
                                    assistant
                                }
                            }
                        }
                    }
                    
                    val nodeExecuteTool by nodeExecuteTool("executeTool")
                    val nodeSendToolResult by nodeLLMSendToolResult("sendToolResult")

                    edge(nodeStart forwardTo streamingRequestNode)

                    edge(streamingRequestNode forwardTo nodeFinish onAssistantMessage { true })

                    edge(streamingRequestNode forwardTo nodeExecuteTool onToolCall { true })

                    edge(nodeExecuteTool forwardTo nodeSendToolResult)

                    edge(nodeSendToolResult forwardTo nodeExecuteTool onToolCall { true })

                    edge(nodeSendToolResult forwardTo nodeFinish onAssistantMessage { true })
                }
            }

            private fun log(message: String) {
                println("[PhoneClawAgent] $message")
            }
        }

        fun builder() = Builder()
    }

    fun run(input: String): String {
        println("[PhoneClawAgent] Running with input: ${input.take(100)}...")
        return runBlocking {
            try {
                val result = agent.run(input)
                println("[PhoneClawAgent] Result: ${result.take(200)}...")
                result
            } catch (e: Exception) {
                println("[PhoneClawAgent] Error: ${e.message}")
                "Error: ${e.message}"
            }
        }
    }

    suspend fun runSuspend(input: String): String {
        println("[PhoneClawAgent] Running with input: ${input.take(100)}...")
        return try {
            val result = agent.run(input)
            println("[PhoneClawAgent] Result: ${result.take(200)}...")
            result
        } catch (e: Exception) {
            println("[PhoneClawAgent] Error: ${e.message}")
            "Error: ${e.message}"
        }
    }

    fun getPhoneEmulationTool(): PhoneEmulationTool = phoneEmulationTool
    fun getUseSkillTool(): UseSkillTool = useSkillTool
}
