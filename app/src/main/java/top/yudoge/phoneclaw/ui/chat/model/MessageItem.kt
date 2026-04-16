package top.yudoge.phoneclaw.ui.chat.model

sealed class MessageItem {
    abstract val id: String
    abstract val timestamp: Long

    data class UserMessage(
        override val id: String,
        override val timestamp: Long,
        val content: String,
        val images: List<ImageInfo>? = null
    ) : MessageItem()

    data class AgentMessage(
        override val id: String,
        override val timestamp: Long,
        val content: String
    ) : MessageItem()

    data class ToolCallMessage(
        override val id: String,
        override val timestamp: Long,
        val toolName: String,
        val params: String,
        var result: String? = null,
        var state: CallState = CallState.RUNNING,
        var isExpanded: Boolean = false
    ) : MessageItem() {
        enum class CallState { RUNNING, SUCCESS, FAILED }
    }

    data class SkillCallMessage(
        override val id: String,
        override val timestamp: Long,
        val skillName: String,
        val arguments: String = "",
        var result: String? = null,
        var state: CallState = CallState.RUNNING
    ) : MessageItem() {
        enum class CallState { RUNNING, SUCCESS, FAILED }
    }

    data class ThinkingMessage(
        override val id: String,
        override val timestamp: Long,
        var status: String = "思考中..."
    ) : MessageItem()

    data class ImageInfo(
        val uri: String
    )
}
