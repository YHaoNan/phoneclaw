package top.yudoge.phoneclaw.ui.chat

import android.net.Uri
import top.yudoge.phoneclaw.db.PhoneClawDbHelper
import top.yudoge.phoneclaw.ui.chat.model.MessageItem

interface ChatContract {

    interface View {
        fun showMessages(messages: List<MessageItem>)
        fun appendMessage(message: MessageItem)
        fun updateMessage(position: Int, message: MessageItem)
        fun removeMessage(position: Int)
        fun scrollToBottom()
        fun showThinking()
        fun hideThinking()
        fun updateThinkingStatus(status: String)
        fun setSendButtonEnabled(enabled: Boolean)
        fun showStopButton()
        fun hideStopButton()
        fun showError(message: String)
        fun showSessionTitle(title: String)
        fun showModelSelector(models: List<String>, selectedIndex: Int)
        fun closeDrawer()
        fun openDrawer()
    }

    interface Presenter {
        fun attachView(view: View)
        fun detachView()
        fun loadSession(sessionId: String?)
        fun createNewSession()
        fun sendMessage(content: String, images: List<Uri>? = null)
        fun stopAgent()
        fun deleteSession(sessionId: String)
        fun renameSession(sessionId: String, newTitle: String)
        fun selectModel(modelIndex: Int)
        fun toggleInputMode()
    }
}
