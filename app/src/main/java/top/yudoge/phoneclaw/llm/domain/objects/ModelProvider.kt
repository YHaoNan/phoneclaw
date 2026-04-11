package top.yudoge.phoneclaw.llm.domain.objects

data class ModelProvider(
    val id: Long,
    val name: String,
    val providerType: String,
    val config: String
)
