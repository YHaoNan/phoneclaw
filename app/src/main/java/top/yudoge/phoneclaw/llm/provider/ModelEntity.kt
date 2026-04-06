package top.yudoge.phoneclaw.llm.provider

data class ModelEntity(
    val id: String,
    val providerId: Long,
    val displayName: String,
    val hasVisualCapability: Boolean
)