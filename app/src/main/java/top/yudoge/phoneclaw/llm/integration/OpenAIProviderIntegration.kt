package top.yudoge.phoneclaw.llm.integration

import top.yudoge.phoneclaw.llm.domain.objects.Model
import top.yudoge.phoneclaw.llm.domain.objects.ModelProvider

class OpenAIProviderIntegration {
    
    fun createChatModelConfig(provider: ModelProvider, model: Model): OpenAIModelConfig {
        return OpenAIModelConfig(
            modelId = model.id,
            baseUrl = extractBaseUrl(provider.config),
            apiKey = extractApiKey(provider.config),
            hasVisualCapability = model.hasVisualCapability
        )
    }
    
    private fun extractBaseUrl(config: String): String {
        return try {
            val json = org.json.JSONObject(config)
            json.optString("baseUrl", "https://api.openai.com/v1")
        } catch (e: Exception) {
            "https://api.openai.com/v1"
        }
    }
    
    private fun extractApiKey(config: String): String {
        return try {
            val json = org.json.JSONObject(config)
            json.getString("apiKey")
        } catch (e: Exception) {
            ""
        }
    }
}

data class OpenAIModelConfig(
    val modelId: String,
    val baseUrl: String,
    val apiKey: String,
    val hasVisualCapability: Boolean = false
)
