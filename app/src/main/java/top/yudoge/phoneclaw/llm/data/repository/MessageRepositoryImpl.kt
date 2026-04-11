package top.yudoge.phoneclaw.llm.data.repository

import android.content.ContentValues
import top.yudoge.phoneclaw.app.data.PhoneClawDatabaseHelper
import top.yudoge.phoneclaw.llm.data.entity.MessageEntity
import top.yudoge.phoneclaw.llm.domain.objects.Message
import top.yudoge.phoneclaw.llm.domain.objects.MessageRole

class MessageRepositoryImpl(
    private val dbHelper: PhoneClawDatabaseHelper
) : MessageRepository {
    
    override fun getBySessionId(sessionId: String): List<Message> {
        val messages = mutableListOf<Message>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            PhoneClawDatabaseHelper.TABLE_MESSAGES,
            null, "session_id = ?", arrayOf(sessionId), null, null, "timestamp ASC"
        )
        
        while (cursor.moveToNext()) {
            val entity = MessageEntity(
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
            )
            messages.add(entity.toDomain())
        }
        cursor.close()
        return messages
    }
    
    override fun insert(message: Message): String {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("session_id", message.sessionId)
            put("role", message.role.name)
            put("content", message.content)
            put("timestamp", message.timestamp)
            put("tool_name", message.toolName)
            put("tool_params", message.toolParams)
            put("tool_result", message.toolResult)
            put("tool_state", message.toolState)
            put("success", if (message.success) 1 else 0)
        }
        val id = db.insert(PhoneClawDatabaseHelper.TABLE_MESSAGES, null, values)
        
        db.execSQL(
            "UPDATE ${PhoneClawDatabaseHelper.TABLE_SESSIONS} SET updated_at = ? WHERE id = ?",
            arrayOf(System.currentTimeMillis(), message.sessionId)
        )
        
        return id.toString()
    }
    
    override fun update(message: Message) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("content", message.content)
            put("tool_name", message.toolName)
            put("tool_params", message.toolParams)
            put("tool_result", message.toolResult)
            put("tool_state", message.toolState)
            put("success", if (message.success) 1 else 0)
        }
        db.update(
            PhoneClawDatabaseHelper.TABLE_MESSAGES,
            values, "session_id = ? AND timestamp = ?",
            arrayOf(message.sessionId, message.timestamp.toString())
        )
    }
    
    override fun delete(id: String) {
        val db = dbHelper.writableDatabase
        db.delete(PhoneClawDatabaseHelper.TABLE_MESSAGES, "id = ?", arrayOf(id))
    }
}

private fun MessageEntity.toDomain() = Message(
    id = id?.toString() ?: "",
    sessionId = sessionId,
    role = MessageRole.valueOf(role),
    content = content,
    timestamp = timestamp,
    toolName = toolName,
    toolParams = toolParams,
    toolResult = toolResult,
    toolState = toolState,
    success = success
)
