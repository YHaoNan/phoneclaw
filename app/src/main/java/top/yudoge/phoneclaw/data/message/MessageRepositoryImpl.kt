package top.yudoge.phoneclaw.data.message

import android.content.Context
import top.yudoge.phoneclaw.db.PhoneClawDbHelper

class MessageRepositoryImpl(context: Context) : MessageRepository {
    private val dbHelper = PhoneClawDbHelper(context)

    override fun saveMessage(sessionId: String, message: Message) {
        val record = PhoneClawDbHelper.MessageRecord().apply {
            role = when (message.role) {
                Role.USER -> "user"
                Role.AGENT -> "agent"
                Role.TOOL -> "tool"
                Role.SKILL -> "skill"
            }
            content = message.content
            timestamp = message.timestamp
            toolName = message.toolName
            toolParams = message.toolParams
            toolResult = message.toolResult
            toolState = message.toolState
            success = message.success
        }
        dbHelper.saveMessage(sessionId, record)
    }
    
    override fun updateMessage(sessionId: String, message: Message) {
        val record = PhoneClawDbHelper.MessageRecord().apply {
            content = message.content
            toolName = message.toolName
            toolParams = message.toolParams
            toolResult = message.toolResult
            toolState = message.toolState
            success = message.success
        }
        dbHelper.updateMessage(sessionId, message.timestamp, record)
    }

    override fun getMessages(sessionId: String): List<Message> {
        return dbHelper.getMessages(sessionId).map { record ->
            Message(
                id = record.timestamp.toString(),
                sessionId = sessionId,
                role = when (record.role) {
                    "user" -> Role.USER
                    "agent" -> Role.AGENT
                    "tool" -> Role.TOOL
                    "skill" -> Role.SKILL
                    else -> Role.USER
                },
                content = record.content,
                timestamp = record.timestamp,
                toolName = record.toolName,
                toolParams = record.toolParams,
                toolResult = record.toolResult,
                toolState = record.toolState,
                success = record.success
            )
        }
    }

    override fun deleteMessages(sessionId: String) {
        // Messages are deleted via dbHelper.deleteSession
    }
}
