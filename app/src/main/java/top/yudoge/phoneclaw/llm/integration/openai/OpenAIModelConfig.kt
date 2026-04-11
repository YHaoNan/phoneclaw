package top.yudoge.phoneclaw.llm.integration.openai

data class OpenAIModelConfig(
    val baseUrl: String,
    val apiKey: String? = "",
    val chatCompletionUrl: String = "/v1/chat/completions",
    val modelsUrl: String = "/v1/models",
    val responseApiEnabled: Boolean = false,
    val responseUrl: String = "/v1/responses",
    val connectTimeoutMillis: Long = 5000,
    val requestTimeoutMillis: Long = 60000
) {
    companion object {
        const val KEY_BASE_URL = "base_url"
        const val KEY_API_KEY = "api_key"
        const val KEY_CHAT_COMPLETION_URL = "chat_completion_url"
        const val KEY_MODELS_URL = "models_url"
        const val KEY_RESPONSE_API_ENABLED = "response_api_enabled"
        const val KEY_RESPONSE_URL = "response_url"
        const val KEY_CONNECT_TIMEOUT = "connect_timeout_millis"
        const val KEY_REQUEST_TIMEOUT = "request_timeout_millis"

        const val DEFAULT_BASE_URL = "https://api.openai.com"
        const val DEFAULT_CHAT_COMPLETION_URL = "/v1/chat/completions"
        const val DEFAULT_MODELS_URL = "/v1/models"
        const val DEFAULT_RESPONSE_URL = "/v1/responses"
        const val DEFAULT_CONNECT_TIMEOUT = 5000L
        const val DEFAULT_REQUEST_TIMEOUT = 60000L

        fun fromJson(json: String): OpenAIModelConfig {
            return try {
                val obj = org.json.JSONObject(json)
                OpenAIModelConfig(
                    baseUrl = obj.optString(KEY_BASE_URL, DEFAULT_BASE_URL),
                    apiKey = obj.optString(KEY_API_KEY, ""),
                    chatCompletionUrl = obj.optString(KEY_CHAT_COMPLETION_URL, DEFAULT_CHAT_COMPLETION_URL),
                    modelsUrl = obj.optString(KEY_MODELS_URL, DEFAULT_MODELS_URL),
                    responseApiEnabled = obj.optBoolean(KEY_RESPONSE_API_ENABLED, false),
                    responseUrl = obj.optString(KEY_RESPONSE_URL, DEFAULT_RESPONSE_URL),
                    connectTimeoutMillis = obj.optLong(KEY_CONNECT_TIMEOUT, DEFAULT_CONNECT_TIMEOUT),
                    requestTimeoutMillis = obj.optLong(KEY_REQUEST_TIMEOUT, DEFAULT_REQUEST_TIMEOUT)
                )
            } catch (e: Exception) {
                OpenAIModelConfig(baseUrl = DEFAULT_BASE_URL)
            }
        }
    }

    fun toJson(): String {
        val obj = org.json.JSONObject()
        obj.put(KEY_BASE_URL, baseUrl)
        obj.put(KEY_API_KEY, apiKey ?: "")
        obj.put(KEY_CHAT_COMPLETION_URL, chatCompletionUrl)
        obj.put(KEY_MODELS_URL, modelsUrl)
        obj.put(KEY_RESPONSE_API_ENABLED, responseApiEnabled)
        obj.put(KEY_RESPONSE_URL, responseUrl)
        obj.put(KEY_CONNECT_TIMEOUT, connectTimeoutMillis)
        obj.put(KEY_REQUEST_TIMEOUT, requestTimeoutMillis)
        return obj.toString()
    }
}
