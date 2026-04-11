package top.yudoge.phoneclaw.llm.data.entity

data class SkillEntity(
    val name: String,
    val description: String,
    val argumentHint: String? = null,
    val disableModelInvocation: Boolean = false,
    val userInvocable: Boolean = true,
    val allowedTools: String? = null,
    val context: String? = null,
    val skillDir: String? = null,
    val supportingFiles: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

data class SkillEntityWithContent(
    val entity: SkillEntity,
    val content: String
)
