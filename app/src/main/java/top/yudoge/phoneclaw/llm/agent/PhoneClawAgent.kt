package top.yudoge.phoneclaw.llm.agent

import android.content.Context
import dev.langchain4j.agent.tool.ToolExecutionRequest
import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.agent.tool.ToolSpecifications
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.ToolExecutionResultMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.request.ChatRequest
import dev.langchain4j.model.chat.response.ChatResponse
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler
import dev.langchain4j.model.openai.OpenAiChatRequestParameters
import dev.langchain4j.model.openai.OpenAiStreamingChatModel
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import top.yudoge.phoneclaw.domain.AgentCallback
import top.yudoge.phoneclaw.llm.skills.AssetSkillRepository
import top.yudoge.phoneclaw.llm.skills.CompositeSkillRepository
import top.yudoge.phoneclaw.llm.skills.FileBasedSkillRepository
import top.yudoge.phoneclaw.llm.skills.Skill
import top.yudoge.phoneclaw.llm.skills.SkillRepository
import top.yudoge.phoneclaw.llm.tools.PhoneEmulationTool
import top.yudoge.phoneclaw.llm.tools.UseSkillTool
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class PhoneClawAgent private constructor(
    private val model: OpenAiStreamingChatModel,
    private val systemPrompt: String,
    private val maxIterations: Int,
    private val callback: AgentCallback?,
    private val phoneEmulationTool: PhoneEmulationTool,
    private val useSkillTool: UseSkillTool,
    private val toolSpecifications: List<ToolSpecification>
) {
    companion object {
        class Builder {
            private var model: OpenAiStreamingChatModel? = null
            private var context: Context? = null
            private var userSkillsDir: File? = null
            private var customSystemPrompt: String? = null
            private var maxIterations: Int = 1000
            private var callback: AgentCallback? = null

            fun model(model: OpenAiStreamingChatModel) = apply { this.model = model }
            fun context(ctx: Context) = apply { this.context = ctx }
            fun userSkillsDir(dir: File) = apply { this.userSkillsDir = dir }
            fun systemPrompt(prompt: String) = apply { this.customSystemPrompt = prompt }
            fun maxIterations(iterations: Int) = apply { this.maxIterations = iterations }
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
                val streamingModel = model ?: throw IllegalStateException("LLM model is required")

                val phoneTool = PhoneEmulationTool()

                val skillRepo: SkillRepository = if (context != null) {
                    val builtInRepo = AssetSkillRepository(context!!)
                    val userDir = userSkillsDir ?: File(context!!.filesDir, "user_skills").apply { mkdirs() }
                    val userRepo = FileBasedSkillRepository(userDir)
                    CompositeSkillRepository(builtInRepo, userRepo)
                } else if (userSkillsDir != null) {
                    FileBasedSkillRepository(userSkillsDir!!)
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
                val resolvedSystemPrompt = customSystemPrompt ?: buildSystemPrompt(skills)
                val skillTool = UseSkillTool(skillRepo)
                val specs = buildList {
                    addAll(ToolSpecifications.toolSpecificationsFrom(phoneTool))
                    addAll(ToolSpecifications.toolSpecificationsFrom(skillTool))
                }

                return PhoneClawAgent(
                    model = streamingModel,
                    systemPrompt = resolvedSystemPrompt,
                    maxIterations = maxIterations,
                    callback = callback,
                    phoneEmulationTool = phoneTool,
                    useSkillTool = skillTool,
                    toolSpecifications = specs
                )
            }
        }

        fun builder() = Builder()
    }

    fun run(input: String): String {
        return runBlocking { runSuspend(input) }
    }

    suspend fun runSuspend(input: String): String {
        val messages = mutableListOf<ChatMessage>(
            SystemMessage.from(systemPrompt),
            UserMessage.from(input)
        )

        repeat(maxIterations) {
            val response = streamOneTurn(messages)
            val aiMessage = response.aiMessage()
            messages += aiMessage

            if (!aiMessage.hasToolExecutionRequests()) {
                callback?.onAgentComplete()
                return aiMessage.text().orEmpty()
            }

            aiMessage.toolExecutionRequests().forEach { request ->
                val result = executeToolRequest(request)
                messages += ToolExecutionResultMessage.from(request, result)
            }
        }

        val error = "Agent exceeded max iterations: $maxIterations"
        callback?.onAgentError(error)
        throw IllegalStateException(error)
    }

    private fun streamOneTurn(messages: List<ChatMessage>): ChatResponse {
        val latch = CountDownLatch(1)
        var response: ChatResponse? = null
        var error: Throwable? = null

        callback?.onLLMStreamStart()
        val request = ChatRequest.builder()
            .messages(messages)
            .parameters(
                OpenAiChatRequestParameters.builder()
                    .toolSpecifications(toolSpecifications)
                    .build()
            )
            .build()

        model.chat(request, object : StreamingChatResponseHandler {
            override fun onPartialResponse(partialResponse: String) {
                if (partialResponse.isNotEmpty()) {
                    callback?.onLLMTokenGenerated(partialResponse)
                }
            }

            override fun onCompleteResponse(completeResponse: ChatResponse) {
                response = completeResponse
                callback?.onLLMStreamEnd()
                latch.countDown()
            }

            override fun onError(throwable: Throwable) {
                error = throwable
                callback?.onAgentError(throwable.message ?: "LLM streaming failed")
                latch.countDown()
            }
        })

        if (!latch.await(120, TimeUnit.SECONDS)) {
            val timeout = IllegalStateException("LLM streaming timed out")
            callback?.onAgentError(timeout.message ?: "LLM streaming timed out")
            throw timeout
        }

        error?.let { throw it }
        return response ?: throw IllegalStateException("LLM returned empty response")
    }

    private fun executeToolRequest(request: ToolExecutionRequest): String {
        val toolName = request.name()
        val argsJson = request.arguments().orEmpty()

        return try {
            if (toolName == "useSkill") {
                val skillName = readStringArg(argsJson, "skillName") ?: "unknown"
                callback?.onSkillCallStart(skillName)
                val result = useSkillTool.useSkill(skillName)
                callback?.onSkillCallEnd(skillName, true)
                result
            } else {
                callback?.onToolCallStart(toolName, argsJson)
                val result = when (toolName) {
                    "executeScript" -> {
                        val script = readStringArg(argsJson, "script")
                            ?: throw IllegalArgumentException("Missing script argument")
                        phoneEmulationTool.executeScript(script)
                    }

                    else -> "Error: tool '$toolName' not found"
                }
                callback?.onToolCallEnd(toolName, result, !result.startsWith("Error:"))
                result
            }
        } catch (e: Exception) {
            val message = "Error: ${e.message ?: "Tool execution failed"}"
            if (toolName == "useSkill") {
                val skillName = readStringArg(argsJson, "skillName") ?: "unknown"
                callback?.onSkillCallEnd(skillName, false)
            } else {
                callback?.onToolCallEnd(toolName, message, false)
            }
            message
        }
    }

    private fun readStringArg(argumentsJson: String, preferredKey: String): String? {
        if (argumentsJson.isBlank()) {
            return null
        }

        return try {
            val json = JSONObject(argumentsJson)
            val direct = json.optString(preferredKey, "").takeIf { it.isNotBlank() }
            if (direct != null) {
                return direct
            }

            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = json.optString(key, "")
                if (value.isNotBlank()) {
                    return value
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    fun getPhoneEmulationTool(): PhoneEmulationTool = phoneEmulationTool
    fun getUseSkillTool(): UseSkillTool = useSkillTool
}
