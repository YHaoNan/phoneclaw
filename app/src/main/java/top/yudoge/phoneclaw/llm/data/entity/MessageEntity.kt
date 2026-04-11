package top.yudoge.phoneclaw.llm.data.entity

data class MessageEntity(
    val id: Long? = null,
    val sessionId: String,
    val role: String,
    val content: String,
    val timestamp: Long,
    val toolName: String? = null,
    val toolParams: String? = null,
    val toolResult: String? = null,
    val toolState: String? = null,
    val success: Boolean = false
)
