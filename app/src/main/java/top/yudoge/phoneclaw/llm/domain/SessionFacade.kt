package top.yudoge.phoneclaw.llm.domain

import top.yudoge.phoneclaw.llm.data.entity.MessageEntity
import top.yudoge.phoneclaw.llm.data.entity.SessionEntity
import top.yudoge.phoneclaw.llm.data.repository.MessageRepository
import top.yudoge.phoneclaw.llm.data.repository.SessionRepository
import top.yudoge.phoneclaw.llm.domain.objects.Message
import top.yudoge.phoneclaw.llm.domain.objects.MessageRole
import top.yudoge.phoneclaw.llm.domain.objects.Session
import java.util.UUID

class SessionFacade(
    private val sessionRepository: SessionRepository,
    private val messageRepository: MessageRepository
) {
    fun getAllSessions(): List<Session> = 
        sessionRepository.getAll().map { it.toDomain() }
    
    fun getSessionById(id: String): Session? = 
        sessionRepository.getById(id)?.toDomain()
    
    fun createSession(title: String = "New Chat", modelId: String? = null): Session {
        val now = System.currentTimeMillis()
        val session = Session(
            id = UUID.randomUUID().toString(),
            title = title,
            createdAt = now,
            updatedAt = now,
            modelId = modelId
        )
        sessionRepository.insert(session.toEntity())
        return session
    }
    
    fun updateSessionTitle(id: String, title: String) {
        sessionRepository.updateTitle(id, title)
    }
    
    fun deleteSession(id: String) {
        sessionRepository.delete(id)
    }
    
    fun getSessionWithMessages(sessionId: String): Pair<Session?, List<Message>> {
        val session = sessionRepository.getById(sessionId)?.toDomain()
        val messages = messageRepository.getBySessionId(sessionId).mapNotNull { it.toDomain() }
        return session to messages
    }
    
    fun getMessages(sessionId: String): List<Message> {
        return messageRepository.getBySessionId(sessionId).mapNotNull { it.toDomain() }
    }
    
    fun addMessage(message: Message): String {
        return messageRepository.insert(message.toEntity()).toString()
    }
    
    fun updateMessage(message: Message) {
        messageRepository.update(message.toEntity())
    }
    
    fun deleteMessage(id: String) {
        messageRepository.delete(id.toLong())
    }
    
    fun clearAllSessions() {
        sessionRepository.getAll().forEach { sessionRepository.delete(it.id) }
    }
    
    private fun SessionEntity.toDomain() = Session(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
        modelId = modelId
    )
    
    private fun Session.toEntity() = SessionEntity(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
        modelId = modelId
    )
    
    private fun MessageEntity.toDomain(): Message? {
        val parsedRole = parseRole(role) ?: return null
        return Message(
            id = id.toString(),
            sessionId = sessionId,
            role = parsedRole,
            content = content,
            timestamp = timestamp,
            toolName = toolName,
            toolParams = toolParams,
            toolResult = toolResult,
            toolState = toolState,
            success = success
        )
    }
    
    private fun Message.toEntity() = MessageEntity(
        id = id.toLongOrNull(),
        sessionId = sessionId,
        role = role.toStorageRole(),
        content = content,
        timestamp = timestamp,
        toolName = toolName,
        toolParams = toolParams,
        toolResult = toolResult,
        toolState = toolState,
        success = success
    )

    private fun parseRole(rawRole: String): MessageRole? {
        return when (rawRole.lowercase()) {
            "user" -> MessageRole.USER
            "assistant", "agent", "ai" -> MessageRole.AGENT
            "tool", "toolcall", "tool_call" -> MessageRole.TOOL
            "skill", "use_skill" -> MessageRole.SKILL
            else -> null
        }
    }

    private fun MessageRole.toStorageRole(): String {
        return when (this) {
            MessageRole.USER -> "user"
            MessageRole.AGENT -> "assistant"
            MessageRole.TOOL -> "tool"
            MessageRole.SKILL -> "skill"
        }
    }
}
