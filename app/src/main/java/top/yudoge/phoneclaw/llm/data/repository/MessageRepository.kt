package top.yudoge.phoneclaw.llm.data.repository

import top.yudoge.phoneclaw.llm.data.entity.MessageEntity

interface MessageRepository {
    fun getBySessionId(sessionId: String): List<MessageEntity>
    fun insert(entity: MessageEntity): Long
    fun update(entity: MessageEntity)
    fun delete(id: Long)
}
