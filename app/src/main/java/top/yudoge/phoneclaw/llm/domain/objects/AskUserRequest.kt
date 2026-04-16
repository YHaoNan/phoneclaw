package top.yudoge.phoneclaw.llm.domain.objects

data class AskUserRequest(
    val requestId: String,
    val question: String,
    val answers: List<String>,
    val allowOther: Boolean = true,
    val timeoutMs: Long = 120_000L,
    val createdAt: Long = System.currentTimeMillis()
)

