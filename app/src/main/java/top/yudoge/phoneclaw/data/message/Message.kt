package top.yudoge.phoneclaw.data.message

data class Message(
    val id: String,
    val sessionId: String,
    val role: Role,
    val content: String,
    val timestamp: Long,
    val toolName: String? = null,
    val toolParams: String? = null,
    val toolResult: String? = null,
    val toolState: String? = null,
    val success: Boolean = false
)

enum class Role {
    USER, AGENT, TOOL, SKILL
}
