package top.yudoge.phoneclaw.llm.domain.objects

data class Model(
    val id: String,
    val providerId: Long,
    val displayName: String,
    val hasVisualCapability: Boolean = false
)
