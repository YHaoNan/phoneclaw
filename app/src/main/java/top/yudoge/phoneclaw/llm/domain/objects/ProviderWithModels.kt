package top.yudoge.phoneclaw.llm.domain.objects

data class ProviderWithModels(
    val provider: ModelProvider,
    val models: List<Model>
)
