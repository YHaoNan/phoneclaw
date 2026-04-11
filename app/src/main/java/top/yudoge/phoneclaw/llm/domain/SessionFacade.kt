package top.yudoge.phoneclaw.llm.domain

import top.yudoge.phoneclaw.llm.data.repository.MessageRepository
import top.yudoge.phoneclaw.llm.data.repository.SessionRepository
import top.yudoge.phoneclaw.llm.domain.objects.Message
import top.yudoge.phoneclaw.llm.domain.objects.Session
import java.util.UUID

class SessionFacade(
    private val sessionRepository: SessionRepository,
    private val messageRepository: MessageRepository
) {
    fun getAllSessions(): List<Session> = sessionRepository.getAll()
    
    fun getSessionById(id: String): Session? = sessionRepository.getById(id)
    
    fun createSession(title: String = "New Chat", modelId: String? = null): Session {
        val now = System.currentTimeMillis()
        val session = Session(
            id = UUID.randomUUID().toString(),
            title = title,
            createdAt = now,
            updatedAt = now,
            modelId = modelId
        )
        sessionRepository.insert(session)
        return session
    }
    
    fun updateSessionTitle(id: String, title: String) {
        sessionRepository.updateTitle(id, title)
    }
    
    fun deleteSession(id: String) {
        sessionRepository.delete(id)
    }
    
    fun getSessionWithMessages(sessionId: String): Pair<Session?, List<Message>> {
        val session = sessionRepository.getById(sessionId)
        val messages = messageRepository.getBySessionId(sessionId)
        return session to messages
    }
    
    fun getMessages(sessionId: String): List<Message> {
        return messageRepository.getBySessionId(sessionId)
    }
    
    fun addMessage(message: Message): String {
        return messageRepository.insert(message)
    }
    
    fun updateMessage(message: Message) {
        messageRepository.update(message)
    }
    
    fun deleteMessage(id: String) {
        messageRepository.delete(id)
    }
    
    fun clearAllSessions() {
        sessionRepository.getAll().forEach { sessionRepository.delete(it.id) }
    }
}
