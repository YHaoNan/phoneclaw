package top.yudoge.phoneclaw.llm.integration

import top.yudoge.phoneclaw.llm.domain.objects.Model
import top.yudoge.phoneclaw.llm.domain.objects.ModelProvider

class AgentIntegration(
    private val openAIProviderIntegration: OpenAIProviderIntegration
) {
    
    fun createAgentConfig(provider: ModelProvider, model: Model): AgentConfig {
        val modelConfig = openAIProviderIntegration.createChatModelConfig(provider, model)
        return AgentConfig(
            modelConfig = modelConfig,
            tools = emptyList()
        )
    }
    
    fun createAgentConfigWithTools(
        provider: ModelProvider, 
        model: Model, 
        tools: List<String>
    ): AgentConfig {
        val modelConfig = openAIProviderIntegration.createChatModelConfig(provider, model)
        return AgentConfig(
            modelConfig = modelConfig,
            tools = tools
        )
    }
}

data class AgentConfig(
    val modelConfig: OpenAIModelConfig,
    val tools: List<String>
)
