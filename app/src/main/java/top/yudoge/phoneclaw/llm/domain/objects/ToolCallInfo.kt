package top.yudoge.phoneclaw.llm.domain.objects

data class ToolCallInfo(
    val toolName: String,
    val arguments: String? = null
)
