package top.yudoge.phoneclaw.llm.data.repository

import top.yudoge.phoneclaw.llm.data.entity.SessionEntity

interface SessionRepository {
    fun getAll(): List<SessionEntity>
    fun getById(id: String): SessionEntity?
    fun insert(entity: SessionEntity)
    fun update(entity: SessionEntity)
    fun updateTitle(id: String, title: String)
    fun delete(id: String)
}
