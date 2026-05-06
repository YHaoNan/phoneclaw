package top.yudoge.phoneclaw.llm.domain.objects

data class TaskScript(
    val id: String,
    val name: String,
    val summary: String,
    val createdTime: Long,
    val codeContent: String
)
