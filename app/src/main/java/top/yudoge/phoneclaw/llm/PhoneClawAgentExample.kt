package top.yudoge.phoneclaw.llm

import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import android.content.Context
import org.json.JSONObject
import top.yudoge.phoneclaw.llm.agent.PhoneClawAgent
import top.yudoge.phoneclaw.llm.provider.ModelProviderRepositoryImpl
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object PhoneClawAgentExample {

    fun createAgentFromDatabase(
        context: Context,
        modelName: String,
        modelId: String = "gpt-4o"
    ): PhoneClawAgent {
        val repository = ModelProviderRepositoryImpl(context)
        val providers = repository.listProvider()
        
        val provider = providers.find { it.name == modelName }
            ?: throw IllegalStateException("Provider '$modelName' not found in database")
        
        val configJson = JSONObject(provider.modelProviderConfig)
        val baseUrl = configJson.optString("baseUrl", "https://api.openai.com")
        val apiKey = configJson.optString("apiKey", "")
        
        val client = OpenAILLMClient(
            apiKey = apiKey,
            settings = OpenAIClientSettings(baseUrl = baseUrl)
        )
        
        val model = LLModel(
            provider = LLMProvider.OpenAI,
            id = modelId,
            capabilities = listOf(
                LLMCapability.Temperature,
                LLMCapability.Tools,
                LLMCapability.ToolChoice,
                LLMCapability.Schema.JSON.Standard,
                LLMCapability.Completion
            ),
            contextLength = 128000,
            maxOutputTokens = 4096
        )
        
        return PhoneClawAgent.builder()
            .llmClient(client)
            .llmModel(model)
            .build()
    }
}
