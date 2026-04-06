package top.yudoge.phoneclaw.domain

import top.yudoge.phoneclaw.data.session.Session
import top.yudoge.phoneclaw.data.session.SessionRepository

class SessionManager(
    private val sessionRepo: SessionRepository
) {
    fun createSession(): Session {
        val id = sessionRepo.createSession("新对话", null)
        return sessionRepo.getSession(id)!!
    }

    fun getSession(id: String): Session? = sessionRepo.getSession(id)

    fun getAllSessions(): List<Session> = sessionRepo.getAllSessions()

    fun updateTitle(id: String, title: String) {
        sessionRepo.updateSessionTitle(id, title)
    }

    fun deleteSession(id: String) {
        sessionRepo.deleteSession(id)
    }
}
