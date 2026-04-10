package top.yudoge.phoneclaw.llm.provider.openai

import org.json.JSONObject
import top.yudoge.phoneclaw.llm.provider.ModelInitializeException
import top.yudoge.phoneclaw.llm.provider.ModelInitializer
import top.yudoge.phoneclaw.llm.provider.ModelProviderEntity

class OpenAIModelInitializer : ModelInitializer {
    override fun validate(provider: ModelProviderEntity) {
        try {
            val config = JSONObject(provider.modelProviderConfig)
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
