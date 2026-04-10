package top.yudoge.phoneclaw.llm.agent

import android.content.Context
import android.util.Log
import dev.langchain4j.model.openai.OpenAiStreamingChatModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import top.yudoge.phoneclaw.domain.AgentCallback
import top.yudoge.phoneclaw.domain.ModelSelector
import top.yudoge.phoneclaw.llm.http.AndroidOkHttpClientBuilder
import top.yudoge.phoneclaw.llm.provider.openai.OpenAIModelConfig
import java.io.File

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
        callback: AgentCallback?,
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
        val requestTimeoutMillis = configJson.optLong(
            OpenAIModelConfig.KEY_REQUEST_TIMEOUT,
            OpenAIModelConfig.DEFAULT_REQUEST_TIMEOUT
        )

        if (apiKey.isBlank()) {
            onError("API Key 未配置")
            return
        }

        try {
            val model = OpenAiStreamingChatModel.builder()
                .httpClientBuilder(AndroidOkHttpClientBuilder())
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(selection.model.id)
                .temperature(0.7)
                .maxTokens(4096)
                .timeout(java.time.Duration.ofMillis(requestTimeoutMillis))
                .build()

            val userSkillsDir = File(context.filesDir, "user_skills").apply { mkdirs() }

            currentAgent = PhoneClawAgent.builder()
                .model(model)
                .context(context)
                .userSkillsDir(userSkillsDir)
                .apply { callback?.let { callback(it) } }
                .build()

            val result = withContext(Dispatchers.IO) {
                currentAgent!!.runSuspend(input)
            }
            onResult(result)
        } catch (e: CancellationException) {
            Log.d(TAG, "Agent was cancelled")
            throw e
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
