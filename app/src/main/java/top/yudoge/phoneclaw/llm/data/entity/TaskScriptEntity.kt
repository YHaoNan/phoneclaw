package top.yudoge.phoneclaw.llm.data.entity

data class TaskScriptEntity(
    val id: String,
    val name: String,
    val summary: String,
    val createdAt: Long,
    val content: String
)
