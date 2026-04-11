package top.yudoge.phoneclaw.llm.data.repository

import android.content.ContentValues
import top.yudoge.phoneclaw.app.data.PhoneClawDatabaseHelper
import top.yudoge.phoneclaw.llm.data.entity.SessionEntity
import top.yudoge.phoneclaw.llm.domain.objects.Session

class SessionRepositoryImpl(
    private val dbHelper: PhoneClawDatabaseHelper
) : SessionRepository {
    
    override fun getAll(): List<Session> {
        val sessions = mutableListOf<Session>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            PhoneClawDatabaseHelper.TABLE_SESSIONS,
            null, null, null, null, null, "updated_at DESC"
        )
        
        while (cursor.moveToNext()) {
            val entity = SessionEntity(
                id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                title = cursor.getString(cursor.getColumnIndexOrThrow("title")) ?: "",
                createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
                updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at")),
                modelId = cursor.getString(cursor.getColumnIndexOrThrow("model_id"))
            )
            sessions.add(entity.toDomain())
        }
        cursor.close()
        return sessions
    }
    
    override fun getById(id: String): Session? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            PhoneClawDatabaseHelper.TABLE_SESSIONS,
            null, "id = ?", arrayOf(id), null, null, null
        )
        
        var session: Session? = null
        if (cursor.moveToFirst()) {
            val entity = SessionEntity(
                id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                title = cursor.getString(cursor.getColumnIndexOrThrow("title")) ?: "",
                createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
                updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at")),
                modelId = cursor.getString(cursor.getColumnIndexOrThrow("model_id"))
            )
            session = entity.toDomain()
        }
        cursor.close()
        return session
    }
    
    override fun insert(session: Session): String {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("id", session.id)
            put("title", session.title)
            put("created_at", session.createdAt)
            put("updated_at", session.updatedAt)
            put("model_id", session.modelId)
        }
        db.insertWithOnConflict(
            PhoneClawDatabaseHelper.TABLE_SESSIONS,
            null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
        return session.id
    }
    
    override fun update(session: Session) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("title", session.title)
            put("updated_at", System.currentTimeMillis())
            put("model_id", session.modelId)
        }
        db.update(PhoneClawDatabaseHelper.TABLE_SESSIONS, values, "id = ?", arrayOf(session.id))
    }
    
    override fun updateTitle(id: String, title: String) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("title", title)
            put("updated_at", System.currentTimeMillis())
        }
        db.update(PhoneClawDatabaseHelper.TABLE_SESSIONS, values, "id = ?", arrayOf(id))
    }
    
    override fun delete(id: String) {
        val db = dbHelper.writableDatabase
        db.delete(PhoneClawDatabaseHelper.TABLE_MESSAGES, "session_id = ?", arrayOf(id))
        db.delete(PhoneClawDatabaseHelper.TABLE_SESSIONS, "id = ?", arrayOf(id))
    }
}

private fun SessionEntity.toDomain() = Session(
    id = id,
    title = title,
    createdAt = createdAt,
    updatedAt = updatedAt,
    modelId = modelId
)
