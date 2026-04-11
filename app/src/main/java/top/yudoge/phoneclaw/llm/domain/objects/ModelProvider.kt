package top.yudoge.phoneclaw.llm.domain.objects

data class ModelProvider(
    val id: Long,
    val name: String,
    val apiType: String,
    val hasVisualCapability: Boolean = false,
    val config: String
)
