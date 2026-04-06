package top.yudoge.phoneclaw.llm.provider.openai

import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.OpenAILLMProvider
import kotlinx.serialization.json.Json
import top.yudoge.phoneclaw.llm.provider.ModelInitializeException
import top.yudoge.phoneclaw.llm.provider.ModelInitializer
import top.yudoge.phoneclaw.llm.provider.ModelProviderEntity
import kotlin.time.ExperimentalTime

class OpenAIModelInitializer : ModelInitializer {

    @ExperimentalTime
    override fun loadClient(provider: ModelProviderEntity): LLMClient {

        try {

            val config =
                Json.decodeFromString<OpenAIModelConfig>(provider.modelProviderConfig)

            val client = OpenAILLMClient(
                apiKey = config.apiKey ?: "",
                settings = OpenAIClientSettings(
                    baseUrl = config.baseUrl,
                    chatCompletionsPath = config.chatCompletionUrl ?: "",
                    embeddingsPath = config.embeddingsUrl ?: "",
                    moderationsPath = config.moderationsUrl ?: "",
                    modelsPath = config.modelsUrl ?: ""
                )
            )

            return client
        } catch (e: Exception) {
            throw ModelInitializeException("Cannot initialize OpenAILLMClient", e)
        }

    }

    override fun getProvider(): LLMProvider {
        return LLMProvider.OpenAI
    }


}