package top.yudoge.phoneclaw.ui.chat.model

class AgentMessageBuffer {
    private var messageId: String? = null
    private val buffer = StringBuilder()
    private var hasDelta = false

    fun start(newMessageId: String) {
        messageId = newMessageId
        buffer.clear()
        hasDelta = false
    }

    fun currentMessageId(): String? = messageId

    fun appendDelta(delta: String): String {
        if (messageId == null) return ""
        val incoming = delta
        val current = buffer.toString()
        // Some providers stream full snapshot text instead of token deltas.
        // If incoming already contains current content as prefix, replace buffer safely.
        if (current.isNotEmpty() && incoming.startsWith(current) && incoming.length >= current.length) {
            buffer.clear()
            buffer.append(incoming)
        } else {
            buffer.append(incoming)
        }
        hasDelta = true
        return buffer.toString()
    }

    fun complete(finalText: String): String {
        if (messageId == null) return ""
        if (!hasDelta && finalText.isNotEmpty()) {
            buffer.clear()
            buffer.append(finalText)
        }
        return buffer.toString()
    }

    fun clearAndGetFinalContent(): String {
        val final = buffer.toString()
        messageId = null
        buffer.clear()
        hasDelta = false
        return final
    }
}
