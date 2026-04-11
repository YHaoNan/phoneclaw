package top.yudoge.phoneclaw.llm.integration.openai

import org.json.JSONObject
import top.yudoge.phoneclaw.llm.domain.ModelInitializeException
import top.yudoge.phoneclaw.llm.domain.ModelInitializer
import top.yudoge.phoneclaw.llm.domain.objects.ModelProvider

class OpenAIModelInitializer : ModelInitializer {
    override fun validate(provider: ModelProvider) {
        try {
            val config = JSONObject(provider.config)
            val baseUrl = config.optString(OpenAIModelConfig.KEY_BASE_URL, OpenAIModelConfig.DEFAULT_BASE_URL)
            val apiKey = config.optString(OpenAIModelConfig.KEY_API_KEY, "")
            if (baseUrl.isBlank() || apiKey.isBlank()) {
                throw IllegalArgumentException("Missing base_url or api_key")
            }
        } catch (e: Exception) {
            throw ModelInitializeException("Invalid OpenAI provider config", e)
        }
    }
}
