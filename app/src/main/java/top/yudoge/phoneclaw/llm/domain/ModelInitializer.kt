package top.yudoge.phoneclaw.llm.domain

import top.yudoge.phoneclaw.llm.data.entity.ModelProviderEntity

interface ModelInitializer {
    fun validate(provider: ModelProviderEntity)
}
