package top.yudoge.phoneclaw.ui.chat.viewholders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import top.yudoge.phoneclaw.R
import top.yudoge.phoneclaw.databinding.ItemMessageToolBinding
import top.yudoge.phoneclaw.ui.chat.model.MessageItem

class ToolCallViewHolder(
    private val binding: ItemMessageToolBinding
) : BaseMessageViewHolder(binding.root) {

    companion object {
        fun create(parent: ViewGroup): ToolCallViewHolder {
            val binding = ItemMessageToolBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ToolCallViewHolder(binding)
        }
    }

    override fun bind(item: MessageItem) {
        if (item !is MessageItem.ToolCallMessage) return
        
        binding.toolNameText.text = item.toolName
        val context = binding.root.context
        
        when (item.state) {
            MessageItem.ToolCallMessage.CallState.RUNNING -> {
                binding.statusText.text = context.getString(R.string.running)
                binding.statusText.setTextColor(ContextCompat.getColor(context, R.color.primary))
            }
            MessageItem.ToolCallMessage.CallState.SUCCESS -> {
                binding.statusText.text = context.getString(R.string.success)
                binding.statusText.setTextColor(ContextCompat.getColor(context, R.color.success))
            }
            MessageItem.ToolCallMessage.CallState.FAILED -> {
                binding.statusText.text = context.getString(R.string.failed)
                binding.statusText.setTextColor(ContextCompat.getColor(context, R.color.error))
            }
        }
        
        binding.paramsText.text = item.params
        binding.resultText.text = item.result ?: ""
        
        if (item.result != null) {
            binding.resultLabel.visibility = View.VISIBLE
            binding.resultText.visibility = View.VISIBLE
        } else {
            binding.resultLabel.visibility = View.GONE
            binding.resultText.visibility = View.GONE
        }
        
        binding.detailsContainer.visibility = if (item.isExpanded) View.VISIBLE else View.GONE
        binding.expandIcon.rotation = if (item.isExpanded) 180f else 0f
        
        binding.expandIcon.setOnClickListener {
            item.isExpanded = !item.isExpanded
            binding.detailsContainer.visibility = if (item.isExpanded) View.VISIBLE else View.GONE
            binding.expandIcon.rotation = if (item.isExpanded) 180f else 0f
        }
    }
}
