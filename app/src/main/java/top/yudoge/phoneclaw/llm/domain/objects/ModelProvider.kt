package top.yudoge.phoneclaw.llm.domain.objects

import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.StreamingChatModel

abstract class ModelProvider(
    val id: Long,
    val name: String,
    val providerType: ProviderType
) {
    abstract fun supportAutoFetchModelList(): Boolean
    abstract fun fetchModelList(): List<Model>
    abstract fun parseToConfig(): String
    abstract fun createChatModel(modelId: String, hasVisualCapability: Boolean = false): ChatModel
    
    open fun createStreamingChatModel(modelId: String, hasVisualCapability: Boolean = false): StreamingChatModel? {
        return null
    }
    
    fun supportsStreaming(): Boolean {
        return try {
            createStreamingChatModel("test", false) != null
        } catch (e: Exception) {
            false
        }
    }
}
