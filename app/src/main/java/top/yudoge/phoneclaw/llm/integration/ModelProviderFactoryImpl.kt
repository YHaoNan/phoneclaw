package top.yudoge.phoneclaw.llm.integration

import top.yudoge.phoneclaw.llm.domain.ModelProviderFactory
import top.yudoge.phoneclaw.llm.domain.objects.ModelProvider
import top.yudoge.phoneclaw.llm.domain.objects.ProviderType
import top.yudoge.phoneclaw.llm.integration.openai.OpenAIModelProvider

class ModelProviderFactoryImpl : ModelProviderFactory {
    override fun create(id: Long, name: String, providerType: ProviderType, configJson: String): ModelProvider {
        return when (providerType) {
            ProviderType.OpenAICompatible -> OpenAIModelProvider.fromConfig(id, name, configJson)
        }
    }
}
