package top.yudoge.phoneclaw.llm.domain.objects

data class ToolCallResult(
    val toolName: String,
    val arguments: String? = null,
    val result: String?,
    val success: Boolean,
    val error: String? = null
)
