package top.yudoge.phoneclaw.ui.chat.viewholders

import android.content.Intent
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.content.ContextCompat
import top.yudoge.phoneclaw.R
import top.yudoge.phoneclaw.databinding.ItemMessageToolBinding
import top.yudoge.phoneclaw.ui.chat.ToolCallDetailActivity
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
            
            if (item.isExpanded) {
                checkContentTruncation(item)
            }
        }
    }
    
    private fun checkContentTruncation(item: MessageItem.ToolCallMessage) {
        binding.paramsText.post {
            val paramsTruncated = isContentTruncated(item.params, 10, binding.paramsText)
            val resultTruncated = item.result?.let { isContentTruncated(it, 10, binding.resultText) } ?: false
            
            if (paramsTruncated || resultTruncated) {
                binding.viewFullButton.visibility = View.VISIBLE
                binding.viewFullButton.setOnClickListener {
                    val context = binding.root.context
                    val intent = Intent(context, ToolCallDetailActivity::class.java).apply {
                        putExtra("toolName", item.toolName)
                        putExtra("params", item.params)
                        putExtra("result", item.result)
                        putExtra("state", item.state.name)
                    }
                    context.startActivity(intent)
                }
            } else {
                binding.viewFullButton.visibility = View.GONE
            }
        }
    }
    
    private fun isContentTruncated(text: String, maxLines: Int, textView: android.widget.TextView): Boolean {
        if (text.isEmpty()) return false
        
        val width = textView.width - textView.paddingLeft - textView.paddingRight
        if (width <= 0) return false
        
        val layout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, textView.paint, width)
                .setMaxLines(maxLines)
                .setEllipsize(TextUtils.TruncateAt.END)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(text, textView.paint, width, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false)
        }
        
        return layout.lineCount > maxLines || (layout.lineCount == maxLines && layout.getEllipsisCount(maxLines - 1) > 0)
    }
}
