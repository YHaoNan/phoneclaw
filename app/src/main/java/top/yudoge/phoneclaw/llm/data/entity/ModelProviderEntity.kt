package top.yudoge.phoneclaw.llm.data.entity

data class ModelProviderEntity(
    val id: Long,
    val name: String,
    val apiType: String,
    val hasVisualCapability: Boolean = false,
    val modelProviderConfig: String
)
