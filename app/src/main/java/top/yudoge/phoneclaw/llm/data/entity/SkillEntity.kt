package top.yudoge.phoneclaw.llm.data.entity

data class SkillEntity(
    val name: String,
    val description: String,
    val argumentHint: String? = null,
    val disableModelInvocation: Boolean = false,
    val userInvocable: Boolean = true,
    val allowedTools: String? = null,
    val context: String? = null,
    val skillDir: String,
    val supportingFiles: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
