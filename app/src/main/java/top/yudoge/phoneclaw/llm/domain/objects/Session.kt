package top.yudoge.phoneclaw.llm.domain.objects

data class Session(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val modelId: String? = null
)
