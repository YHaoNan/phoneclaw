package top.yudoge.phoneclaw.llm.data.repository

import android.content.ContentValues
import top.yudoge.phoneclaw.app.data.PhoneClawDatabaseHelper
import top.yudoge.phoneclaw.llm.data.entity.MessageEntity

class MessageRepositoryImpl(
    private val dbHelper: PhoneClawDatabaseHelper
) : MessageRepository {
    
    override fun getBySessionId(sessionId: String): List<MessageEntity> {
        val messages = mutableListOf<MessageEntity>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            PhoneClawDatabaseHelper.TABLE_MESSAGES,
            null, "session_id = ?", arrayOf(sessionId), null, null, "timestamp ASC"
        )
        
        while (cursor.moveToNext()) {
            messages.add(MessageEntity(
                id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                sessionId = cursor.getString(cursor.getColumnIndexOrThrow("session_id")),
                role = cursor.getString(cursor.getColumnIndexOrThrow("role")),
                content = cursor.getString(cursor.getColumnIndexOrThrow("content")),
                timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")),
                toolName = cursor.getString(cursor.getColumnIndexOrThrow("tool_name")),
                toolParams = cursor.getString(cursor.getColumnIndexOrThrow("tool_params")),
                toolResult = cursor.getString(cursor.getColumnIndexOrThrow("tool_result")),
                toolState = cursor.getString(cursor.getColumnIndexOrThrow("tool_state")),
                success = cursor.getInt(cursor.getColumnIndexOrThrow("success")) == 1
            ))
        }
        cursor.close()
        return messages
    }
    
    override fun insert(entity: MessageEntity): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("session_id", entity.sessionId)
            put("role", entity.role)
            put("content", entity.content)
            put("timestamp", entity.timestamp)
            put("tool_name", entity.toolName)
            put("tool_params", entity.toolParams)
            put("tool_result", entity.toolResult)
            put("tool_state", entity.toolState)
            put("success", if (entity.success) 1 else 0)
        }
        val id = db.insert(PhoneClawDatabaseHelper.TABLE_MESSAGES, null, values)
        
        db.execSQL(
            "UPDATE ${PhoneClawDatabaseHelper.TABLE_SESSIONS} SET updated_at = ? WHERE id = ?",
            arrayOf(System.currentTimeMillis(), entity.sessionId)
        )
        
        return id
    }
    
    override fun update(entity: MessageEntity) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("content", entity.content)
            put("tool_name", entity.toolName)
            put("tool_params", entity.toolParams)
            put("tool_result", entity.toolResult)
            put("tool_state", entity.toolState)
            put("success", if (entity.success) 1 else 0)
        }
        db.update(
            PhoneClawDatabaseHelper.TABLE_MESSAGES,
            values, "id = ?",
            arrayOf(entity.id.toString())
        )
    }
    
    override fun delete(id: Long) {
        val db = dbHelper.writableDatabase
        db.delete(PhoneClawDatabaseHelper.TABLE_MESSAGES, "id = ?", arrayOf(id.toString()))
    }
}
