package top.yudoge.phoneclaw.llm.domain

import top.yudoge.phoneclaw.llm.domain.objects.ModelProvider
import top.yudoge.phoneclaw.llm.domain.objects.ProviderType

interface ModelProviderFactory {
    fun create(id: Long, name: String, providerType: ProviderType, configJson: String): ModelProvider
}
