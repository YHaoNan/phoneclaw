package top.yudoge.phoneclaw.llm.provider.openai

data class OpenAIModelConfig(
    val baseUrl: String,
    val chatCompletionUrl: String? = "/v1/chat/completions",
    val embeddingsUrl: String? = "/v1/embeddings",
    val moderationsUrl: String?,
    val modelsUrl: String? = "/v1/models",
    val apiKey: String? = "",
    val connectTimeoutMillis: Long? = 5000,
    val requestTimeoutMillis: Long? = 60000,
    val socketTimeoutMillis: Long? = 120000
) {
    companion object {
        const val KEY_BASE_URL = "base_url"
        const val KEY_API_KEY = "api_key"
        const val KEY_CHAT_COMPLETION_URL = "chat_completion_url"
        const val KEY_EMBEDDINGS_URL = "embeddings_url"
        const val KEY_MODERATIONS_URL = "moderations_url"
        const val KEY_MODELS_URL = "models_url"
        const val KEY_CONNECT_TIMEOUT = "connect_timeout_millis"
        const val KEY_REQUEST_TIMEOUT = "request_timeout_millis"

        const val DEFAULT_BASE_URL = "https://api.openai.com"
        const val DEFAULT_CHAT_COMPLETION_URL = "/v1/chat/completions"
        const val DEFAULT_EMBEDDINGS_URL = "/v1/embeddings"
        const val DEFAULT_MODELS_URL = "/v1/models"
        const val DEFAULT_CONNECT_TIMEOUT = 5000L
        const val DEFAULT_REQUEST_TIMEOUT = 60000L
    }
}