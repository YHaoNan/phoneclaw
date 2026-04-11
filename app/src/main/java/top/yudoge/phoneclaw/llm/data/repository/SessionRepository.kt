package top.yudoge.phoneclaw.llm.data.repository

import top.yudoge.phoneclaw.llm.domain.objects.Session

interface SessionRepository {
    fun getAll(): List<Session>
    fun getById(id: String): Session?
    fun insert(session: Session): String
    fun update(session: Session)
    fun updateTitle(id: String, title: String)
    fun delete(id: String)
}
