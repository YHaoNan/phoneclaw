package top.yudoge.phoneclaw.llm.data.repository

import top.yudoge.phoneclaw.llm.domain.objects.Message

interface MessageRepository {
    fun getBySessionId(sessionId: String): List<Message>
    fun insert(message: Message): String
    fun update(message: Message)
    fun delete(id: String)
}
