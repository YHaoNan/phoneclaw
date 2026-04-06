package top.yudoge.phoneclaw.ui.chat.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import top.yudoge.phoneclaw.R
import top.yudoge.phoneclaw.databinding.ItemMessageSkillBinding
import top.yudoge.phoneclaw.ui.chat.model.MessageItem

class SkillCallViewHolder(
    private val binding: ItemMessageSkillBinding
) : BaseMessageViewHolder(binding.root) {

    companion object {
        fun create(parent: ViewGroup): SkillCallViewHolder {
            val binding = ItemMessageSkillBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return SkillCallViewHolder(binding)
        }
    }

    override fun bind(item: MessageItem) {
        if (item !is MessageItem.SkillCallMessage) return
        
        binding.skillNameText.text = item.skillName
        val context = binding.root.context
        
        when (item.state) {
            MessageItem.SkillCallMessage.CallState.RUNNING -> {
                binding.statusText.text = context.getString(R.string.running)
                binding.statusText.setTextColor(ContextCompat.getColor(context, R.color.primary))
            }
            MessageItem.SkillCallMessage.CallState.SUCCESS -> {
                binding.statusText.text = context.getString(R.string.success)
                binding.statusText.setTextColor(ContextCompat.getColor(context, R.color.success))
            }
            MessageItem.SkillCallMessage.CallState.FAILED -> {
                binding.statusText.text = context.getString(R.string.failed)
                binding.statusText.setTextColor(ContextCompat.getColor(context, R.color.error))
            }
        }
    }
}
