package top.yudoge.phoneclaw.llm.provider

import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.llm.LLMProvider

interface ModelInitializer {

    fun loadClient(provider: ModelProviderEntity): LLMClient

    fun getProvider(): LLMProvider

}