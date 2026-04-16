package top.yudoge.phoneclaw.llm.domain.objects

enum class AskUserAnswerSource {
    OPTION,
    OTHER,
    CANCELLED,
    TIMEOUT
}

data class AskUserResponse(
    val requestId: String,
    val confirmed: Boolean,
    val answer: String? = null,
    val source: AskUserAnswerSource,
    val error: String? = null,
    val submittedAt: Long = System.currentTimeMillis()
)

