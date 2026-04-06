package top.yudoge.phoneclaw.domain

import top.yudoge.phoneclaw.llm.provider.ModelEntity
import top.yudoge.phoneclaw.llm.provider.ModelProviderEntity

data class ModelSelection(
    val provider: ModelProviderEntity,
    val model: ModelEntity
)
