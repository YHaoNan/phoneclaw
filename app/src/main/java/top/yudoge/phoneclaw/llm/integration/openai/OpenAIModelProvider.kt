package top.yudoge.phoneclaw.llm.integration.openai

import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.openai.OpenAiStreamingChatModel
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import top.yudoge.phoneclaw.llm.domain.objects.Model
import top.yudoge.phoneclaw.llm.domain.objects.ModelProvider
import top.yudoge.phoneclaw.llm.domain.objects.ProviderType
import top.yudoge.phoneclaw.llm.integration.http.AndroidOkHttpClientBuilder
import java.util.concurrent.TimeUnit

class OpenAIModelProvider(
    id: Long,
    name: String,
    private val config: OpenAIModelConfig
) : ModelProvider(id, name, ProviderType.OpenAICompatible) {

    override fun supportAutoFetchModelList(): Boolean = true

    override fun fetchModelList(): List<Model> {
        val client = OkHttpClient.Builder()
            .connectTimeout(config.connectTimeoutMillis, TimeUnit.MILLISECONDS)
            .readTimeout(config.requestTimeoutMillis, TimeUnit.MILLISECONDS)
            .build()

        val url = config.baseUrl.removeSuffix("/") + config.modelsUrl
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return emptyList()
                }
                val body = response.body?.string() ?: return emptyList()
                parseModelsResponse(body)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun parseToConfig(): String = config.toJson()

    override fun createChatModel(modelId: String, hasVisualCapability: Boolean): ChatModel {
        val httpClientBuilder = AndroidOkHttpClientBuilder()
            .connectTimeout(java.time.Duration.ofMillis(config.connectTimeoutMillis))
            .readTimeout(java.time.Duration.ofMillis(config.requestTimeoutMillis))

        return OpenAiChatModel.builder()
            .baseUrl(config.baseUrl.removeSuffix("/"))
            .apiKey(config.apiKey ?: "")
            .modelName(modelId)
            .httpClientBuilder(httpClientBuilder)
            .build()
    }

    override fun createStreamingChatModel(modelId: String, hasVisualCapability: Boolean): StreamingChatModel {
        val httpClientBuilder = AndroidOkHttpClientBuilder()
            .connectTimeout(java.time.Duration.ofMillis(config.connectTimeoutMillis))
            .readTimeout(java.time.Duration.ofMillis(config.requestTimeoutMillis))

        return OpenAiStreamingChatModel.builder()
            .baseUrl(config.baseUrl.removeSuffix("/"))
            .apiKey(config.apiKey ?: "")
            .modelName(modelId)
            .httpClientBuilder(httpClientBuilder)
            .build()
    }

    private fun parseModelsResponse(json: String): List<Model> {
        val models = mutableListOf<Model>()
        val obj = JSONObject(json)
        val dataArray: JSONArray = obj.optJSONArray("data") ?: return emptyList()

        for (i in 0 until dataArray.length()) {
            val modelObj = dataArray.getJSONObject(i)
            val modelId = modelObj.getString("id")
            models.add(Model(
                id = modelId,
                providerId = this.id,
                displayName = modelId,
                hasVisualCapability = guessVisualCapability(modelId)
            ))
        }
        return models
    }

    private fun guessVisualCapability(modelId: String): Boolean {
        val visualKeywords = listOf("vision", "gpt-4o", "gpt-4-turbo", "claude-3", "gemini")
        return visualKeywords.any { modelId.contains(it, ignoreCase = true) }
    }

    companion object {
        fun fromConfig(id: Long, name: String, configJson: String): OpenAIModelProvider {
            val config = OpenAIModelConfig.fromJson(configJson)
            return OpenAIModelProvider(id, name, config)
        }
    }
}
