package top.yudoge.phoneclaw.data.message

interface MessageRepository {
    fun saveMessage(sessionId: String, message: Message)
    fun updateMessage(sessionId: String, message: Message)
    fun getMessages(sessionId: String): List<Message>
    fun deleteMessages(sessionId: String)
}
