package top.yudoge.phoneclaw.llm.data.entity

data class ModelEntity(
    val id: String,
    val providerId: Long,
    val displayName: String,
    val hasVisualCapability: Boolean = false
)
