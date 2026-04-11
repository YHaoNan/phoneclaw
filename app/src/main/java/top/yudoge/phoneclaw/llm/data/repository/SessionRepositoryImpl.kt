package top.yudoge.phoneclaw.llm.data.repository

import android.content.ContentValues
import top.yudoge.phoneclaw.app.data.PhoneClawDatabaseHelper
import top.yudoge.phoneclaw.llm.data.entity.SessionEntity

class SessionRepositoryImpl(
    private val dbHelper: PhoneClawDatabaseHelper
) : SessionRepository {
    
    override fun getAll(): List<SessionEntity> {
        val sessions = mutableListOf<SessionEntity>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            PhoneClawDatabaseHelper.TABLE_SESSIONS,
            null, null, null, null, null, "updated_at DESC"
        )
        
        while (cursor.moveToNext()) {
            sessions.add(SessionEntity(
                id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                title = cursor.getString(cursor.getColumnIndexOrThrow("title")) ?: "",
                createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
                updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at")),
                modelId = cursor.getString(cursor.getColumnIndexOrThrow("model_id"))
            ))
        }
        cursor.close()
        return sessions
    }
    
    override fun getById(id: String): SessionEntity? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            PhoneClawDatabaseHelper.TABLE_SESSIONS,
            null, "id = ?", arrayOf(id), null, null, null
        )
        
        var session: SessionEntity? = null
        if (cursor.moveToFirst()) {
            session = SessionEntity(
                id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                title = cursor.getString(cursor.getColumnIndexOrThrow("title")) ?: "",
                createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
                updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at")),
                modelId = cursor.getString(cursor.getColumnIndexOrThrow("model_id"))
            )
        }
        cursor.close()
        return session
    }
    
    override fun insert(entity: SessionEntity) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("id", entity.id)
            put("title", entity.title)
            put("created_at", entity.createdAt)
            put("updated_at", entity.updatedAt)
            put("model_id", entity.modelId)
        }
        db.insertWithOnConflict(
            PhoneClawDatabaseHelper.TABLE_SESSIONS,
            null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
    }
    
    override fun update(entity: SessionEntity) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("title", entity.title)
            put("updated_at", System.currentTimeMillis())
            put("model_id", entity.modelId)
        }
        db.update(PhoneClawDatabaseHelper.TABLE_SESSIONS, values, "id = ?", arrayOf(entity.id))
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
