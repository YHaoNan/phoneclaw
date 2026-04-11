package top.yudoge.phoneclaw.llm.data.entity

data class SessionEntity(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val modelId: String? = null
)
