package top.yudoge.phoneclaw.llm.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import kotlinx.coroutines.runBlocking
import top.yudoge.phoneclaw.llm.provider.ModelInitializer
import top.yudoge.phoneclaw.llm.provider.ModelProviderEntity
import top.yudoge.phoneclaw.llm.skills.FileBasedSkillRepository
import top.yudoge.phoneclaw.llm.tools.PhoneEmulationTool
import top.yudoge.phoneclaw.llm.tools.UseSkillTool
import java.io.File

class PhoneClawAgent private constructor(
    private val agent: AIAgent<String, String>,
    private val phoneEmulationTool: PhoneEmulationTool,
    private val useSkillTool: UseSkillTool
) {
    companion object {
        private const val DEFAULT_SYSTEM_PROMPT = """
You are PhoneClawAgent, an AI assistant that can control Android phones through Lua scripts.

## Your Capabilities

1. **Phone Control**: You can execute Lua scripts on a connected Android phone using the phone control tools.
2. **Skill Loading**: You can dynamically load skills to learn new capabilities.

## How to Work

1. First, understand the user's request
2. Load relevant skills if needed using the skill tools
3. Execute phone operations step by step
4. Report results clearly

## Available Phone Control Tools

- `executeScript`: Run any Lua script on the phone
- `getCurrentScreen`: Get current UI structure
- `openApp`: Open an app by package name
- `tap`: Tap at coordinates
- `swipe`: Perform swipe gesture
- `inputText`: Input text into a field
- `findElements`: Find UI elements by pattern
- `pressBack`: Press back button
- `pressHome`: Press home button

## Skill Tools

- `listSkills`: See all available skills
- `loadSkill`: Load a skill to get its documentation
- `getSkillContent`: Get content of a loaded skill
- `unloadSkill`: Remove a loaded skill

## Best Practices

1. Always check current screen state before taking actions
2. Use findElements to locate UI elements before interacting
3. Add appropriate delays between operations (use emu:waitMS)
4. Handle errors gracefully and report issues clearly
5. Load the 'phone_emulation' skill for detailed API documentation
"""

        class Builder {
            private var llmClient: LLMClient? = null
            private var llmModel: LLModel? = null
            private var skillsDir: File? = null
            private var systemPrompt: String = DEFAULT_SYSTEM_PROMPT
            private var maxIterations: Int = 50
            private var temperature: Double = 0.7

            fun llmClient(client: LLMClient) = apply { this.llmClient = client }
            fun llmModel(model: LLModel) = apply { this.llmModel = model }
            fun skillsDir(dir: File) = apply { this.skillsDir = dir }
            fun systemPrompt(prompt: String) = apply { this.systemPrompt = prompt }
            fun maxIterations(iterations: Int) = apply { this.maxIterations = iterations }
            fun temperature(temp: Double) = apply { this.temperature = temp }

            fun build(): PhoneClawAgent {
                val client = llmClient ?: throw IllegalStateException("LLM client is required")
                val model = llmModel ?: throw IllegalStateException("LLM model is required")

                val executor = MultiLLMPromptExecutor(model.provider to client)

                val phoneTool = PhoneEmulationTool()
                
                val skillRepo = skillsDir?.let { FileBasedSkillRepository(it) }
                    ?: object : top.yudoge.phoneclaw.llm.skills.SkillRepository {
                        override fun loadSkills(): List<top.yudoge.phoneclaw.llm.skills.Skill> = emptyList()
                        override fun getSkill(name: String): top.yudoge.phoneclaw.llm.skills.Skill? = null
                        override fun addSkill(skill: top.yudoge.phoneclaw.llm.skills.Skill) {}
                        override fun updateSkill(skill: top.yudoge.phoneclaw.llm.skills.Skill) {}
                        override fun deleteSkill(skill: top.yudoge.phoneclaw.llm.skills.Skill) {}
                        override fun deleteSkillByName(name: String) {}
                        override fun skillExists(name: String): Boolean = false
                    }
                val skillTool = UseSkillTool(skillRepo)

                val toolRegistry = ToolRegistry {
                    tools(phoneTool.asTools())
                    tools(skillTool.asTools())
                }

                log("Building PhoneClawAgent with model: ${model.id}")
                log("Tools: PhoneEmulationTool, UseSkillTool")

                val agent = AIAgent(
                    promptExecutor = executor,
                    llmModel = model,
                    systemPrompt = systemPrompt,
                    toolRegistry = toolRegistry,
                    maxIterations = maxIterations,
                    temperature = temperature
                )

                return PhoneClawAgent(agent, phoneTool, skillTool)
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
