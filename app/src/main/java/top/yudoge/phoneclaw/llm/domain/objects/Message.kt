package top.yudoge.phoneclaw.llm.domain.objects

enum class MessageRole {
    USER,
    AGENT,
    TOOL,
    SKILL
}

data class Message(
    val id: String,
    val sessionId: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Long,
    val toolName: String? = null,
    val toolParams: String? = null,
    val toolResult: String? = null,
    val toolState: String? = null,
    val success: Boolean = false
)
