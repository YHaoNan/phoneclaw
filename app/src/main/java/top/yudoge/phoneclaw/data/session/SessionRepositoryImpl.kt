package top.yudoge.phoneclaw.data.session

import android.content.Context
import top.yudoge.phoneclaw.db.PhoneClawDbHelper

class SessionRepositoryImpl(context: Context) : SessionRepository {
    private val dbHelper = PhoneClawDbHelper(context)

    override fun createSession(title: String, modelId: String?): String {
        val id = java.util.UUID.randomUUID().toString()
        dbHelper.saveSession(id, title, System.currentTimeMillis(), modelId)
        return id
    }

    override fun getSession(id: String): Session? {
        val record = dbHelper.getSession(id) ?: return null
        return Session(
            id = record.id,
            title = record.title,
            createdAt = record.createdAt,
            updatedAt = record.updatedAt,
            modelId = record.modelId
        )
    }

    override fun getAllSessions(): List<Session> {
        return dbHelper.allSessions.map { record ->
            Session(
                id = record.id,
                title = record.title,
                createdAt = record.createdAt,
                updatedAt = record.updatedAt,
                modelId = record.modelId
            )
        }
    }

    override fun updateSessionTitle(id: String, title: String) {
        dbHelper.updateSessionTitle(id, title)
    }

    override fun updateSessionModel(id: String, modelId: String) {
        dbHelper.updateSessionModel(id, modelId)
    }

    override fun deleteSession(id: String) {
        dbHelper.deleteSession(id)
    }
}
