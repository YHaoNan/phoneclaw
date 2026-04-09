package top.yudoge.phoneclaw.llm.skills

data class Skill(
    val name: String,
    val description: String,
    val argumentHint: String? = null,
    val disableModelInvocation: Boolean = false,
    val userInvocable: Boolean = true,
    val allowedTools: List<String>? = null,
    val context: String? = null,
    val content: String,
    val skillDir: String? = null,
    val supportingFiles: List<String> = emptyList(),
    val isBuiltIn: Boolean = false
)
