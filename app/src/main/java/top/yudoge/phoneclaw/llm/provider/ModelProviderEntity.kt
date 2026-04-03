package top.yudoge.phoneclaw.llm.provider

/**
 * 代表一个用户定义的模型
 */
data class ModelProviderEntity(
    val id: Long,
    val name: String,
    val apiType: APIType,
    val hasVisualCapability: Boolean,
    val modelProviderConfig: String
)
