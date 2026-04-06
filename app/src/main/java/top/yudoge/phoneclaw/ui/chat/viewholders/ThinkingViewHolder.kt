package top.yudoge.phoneclaw.ui.chat.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import top.yudoge.phoneclaw.databinding.ItemMessageThinkingBinding
import top.yudoge.phoneclaw.ui.chat.model.MessageItem

class ThinkingViewHolder(
    private val binding: ItemMessageThinkingBinding
) : BaseMessageViewHolder(binding.root) {

    companion object {
        fun create(parent: ViewGroup): ThinkingViewHolder {
            val binding = ItemMessageThinkingBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ThinkingViewHolder(binding)
        }
    }

    override fun bind(item: MessageItem) {
        if (item !is MessageItem.ThinkingMessage) return
        
        binding.statusText.text = item.status
    }
    
    fun updateStatus(status: String) {
        binding.statusText.text = status
    }
}
