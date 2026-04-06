package top.yudoge.phoneclaw.ui.chat.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import top.yudoge.phoneclaw.databinding.ItemMessageAgentBinding
import top.yudoge.phoneclaw.ui.chat.model.MessageItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AgentMessageViewHolder(
    private val binding: ItemMessageAgentBinding
) : BaseMessageViewHolder(binding.root) {

    companion object {
        fun create(parent: ViewGroup): AgentMessageViewHolder {
            val binding = ItemMessageAgentBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return AgentMessageViewHolder(binding)
        }
    }

    override fun bind(item: MessageItem) {
        if (item !is MessageItem.AgentMessage) return
        
        binding.contentText.text = item.content
        binding.timeText.text = formatTime(item.timestamp)
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
