package top.yudoge.phoneclaw.llm.data.entity

data class ModelProviderEntity(
    val id: Long,
    val name: String,
    val providerType: String,
    val modelProviderConfig: String
)
