package top.yudoge.phoneclaw.llm.domain.objects

enum class SkillSource {
    BUILT_IN,
    USER
}

data class Skill(
    val name: String,
    val description: String,
    val argumentHint: String? = null,
    val disableModelInvocation: Boolean = false,
    val userInvocable: Boolean = true,
    val allowedTools: List<String>? = null,
    val context: String? = null,
    val source: SkillSource = SkillSource.BUILT_IN,
    val skillDir: String? = null,
    val supportingFiles: List<String> = emptyList()
)

data class SkillWithContent(
    val skill: Skill,
    val content: String
)
