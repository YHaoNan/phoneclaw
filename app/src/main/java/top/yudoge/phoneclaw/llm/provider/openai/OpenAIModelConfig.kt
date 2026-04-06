package top.yudoge.phoneclaw.llm.provider.openai

/**
 * OpenAI兼容模型配置
 */
data class OpenAIModelConfig(
    /**
     * 基础url
     */
    val baseUrl: String,
    /**
     * 聊天url
     */
    val chatCompletionUrl: String? = "/v1/chat/completions",
    /**
     * embeddingsUrl
     */
    val embeddingsUrl: String? = "/v1/embeddings",

    val moderationsUrl: String?,

    /**
     * 模型列表url
     */
    val modelsUrl: String? = "/v1/models",
    /**
     * apikey
     */
    val apiKey: String? = "",
    /**
     * 指定模型名称
     */
    val specificModel: String?,

    val connectTimeoutMillis: Long? = 5000,

    val requestTimeoutMillis: Long? = 60000,

    val socketTimeoutMillis: Long? = 120000
)