package top.yudoge.phoneclaw.domain

import android.content.Context
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import top.yudoge.phoneclaw.llm.agent.PhoneClawAgent
import top.yudoge.phoneclaw.llm.provider.openai.OpenAIModelConfig
import java.io.File
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class AgentOrchestrator(
    private val context: Context,
    private val modelSelector: ModelSelector
) {
    private var currentAgent: PhoneClawAgent? = null

    companion object {
        private const val TAG = "AgentOrchestrator"
    }

    fun isRunning(): Boolean = currentAgent != null

    suspend fun runAgent(
        input: String,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val selection = modelSelector.getSelectedModel()
        if (selection == null) {
            onError("请先选择模型")
            return
        }

        val configJson = try {
            JSONObject(selection.provider.modelProviderConfig)
        } catch (e: Exception) {
            onError("模型配置错误")
            return
        }

        val baseUrl = configJson.optString(OpenAIModelConfig.KEY_BASE_URL, OpenAIModelConfig.DEFAULT_BASE_URL)
        val apiKey = configJson.optString(OpenAIModelConfig.KEY_API_KEY, "")

        if (apiKey.isBlank()) {
            onError("API Key 未配置")
            return
        }

        try {
            val client = OpenAILLMClient(
                apiKey = apiKey,
                settings = OpenAIClientSettings(baseUrl = baseUrl)
            )

            val model = LLModel(
                provider = LLMProvider.OpenAI,
                id = selection.model.id,
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

            val skillsDir = File(context.filesDir, "skills").apply { mkdirs() }

            currentAgent = PhoneClawAgent.builder()
                .llmClient(client)
                .llmModel(model)
                .skillsDir(skillsDir)
                .build()

            val result = withContext(Dispatchers.IO) {
                currentAgent!!.runSuspend(input)
            }
            onResult(result)
        } catch (e: CancellationException) {
            Log.d(TAG, "Agent was cancelled")
            throw e // Re-throw to let the parent coroutine handle it
        } catch (e: Exception) {
            Log.e(TAG, "Agent error", e)
            onError(e.message ?: "执行出错")
        } finally {
            currentAgent = null
        }
    }

    fun stopAgent() {
        currentAgent = null
    }
}
