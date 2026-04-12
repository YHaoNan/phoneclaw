package top.yudoge.phoneclaw.ui.floating

import android.content.Context
import android.content.Intent

object FloatingWindowStatusNotifier {
    const val ACTION_STATUS_CHANGED = "top.yudoge.phoneclaw.ui.floating.ACTION_STATUS_CHANGED"
    const val EXTRA_STATE = "extra_state"
    const val EXTRA_TITLE = "extra_title"

    const val STATE_IDLE = "idle"
    const val STATE_REASONING = "reasoning"
    const val STATE_TOOL_RUNNING = "tool_running"
    const val STATE_COMPLETED = "completed"
    const val STATE_ERROR = "error"

    fun notify(context: Context, state: String, title: String? = null) {
        val intent = Intent(ACTION_STATUS_CHANGED).apply {
            `package` = context.packageName
            putExtra(EXTRA_STATE, state)
            putExtra(EXTRA_TITLE, title)
        }
        context.sendBroadcast(intent)
    }
}
