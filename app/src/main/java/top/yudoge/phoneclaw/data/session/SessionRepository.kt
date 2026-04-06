package top.yudoge.phoneclaw.data.session

interface SessionRepository {
    fun createSession(title: String, modelId: String?): String
    fun getSession(id: String): Session?
    fun getAllSessions(): List<Session>
    fun updateSessionTitle(id: String, title: String)
    fun updateSessionModel(id: String, modelId: String)
    fun deleteSession(id: String)
}
