package top.yudoge.phoneclaw.ui.chat

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import top.yudoge.phoneclaw.ui.chat.model.MessageItem
import top.yudoge.phoneclaw.ui.chat.viewholders.AgentMessageViewHolder
import top.yudoge.phoneclaw.ui.chat.viewholders.SkillCallViewHolder
import top.yudoge.phoneclaw.ui.chat.viewholders.ThinkingViewHolder
import top.yudoge.phoneclaw.ui.chat.viewholders.ToolCallViewHolder
import top.yudoge.phoneclaw.ui.chat.viewholders.UserMessageViewHolder

class MessageAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_USER = 0
        private const val TYPE_AGENT = 1
        private const val TYPE_TOOL = 2
        private const val TYPE_SKILL = 3
        private const val TYPE_THINKING = 4
    }

    private val items = mutableListOf<MessageItem>()
    private var thinkingPosition: Int = -1

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is MessageItem.UserMessage -> TYPE_USER
            is MessageItem.AgentMessage -> TYPE_AGENT
            is MessageItem.ToolCallMessage -> TYPE_TOOL
            is MessageItem.SkillCallMessage -> TYPE_SKILL
            is MessageItem.ThinkingMessage -> TYPE_THINKING
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_USER -> UserMessageViewHolder.create(parent)
            TYPE_AGENT -> AgentMessageViewHolder.create(parent)
            TYPE_TOOL -> ToolCallViewHolder.create(parent)
            TYPE_SKILL -> SkillCallViewHolder.create(parent)
            TYPE_THINKING -> ThinkingViewHolder.create(parent)
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is UserMessageViewHolder -> holder.bind(items[position] as MessageItem.UserMessage)
            is AgentMessageViewHolder -> holder.bind(items[position] as MessageItem.AgentMessage)
            is ToolCallViewHolder -> holder.bind(items[position] as MessageItem.ToolCallMessage)
            is SkillCallViewHolder -> holder.bind(items[position] as MessageItem.SkillCallMessage)
            is ThinkingViewHolder -> holder.bind(items[position] as MessageItem.ThinkingMessage)
        }
    }

    fun setItems(messages: List<MessageItem>) {
        items.clear()
        items.addAll(messages)
        thinkingPosition = -1
        notifyDataSetChanged()
    }

    fun addItem(message: MessageItem) {
        val insertAt = items.size
        items.add(message)
        notifyItemInserted(insertAt)
    }

    fun updateItem(position: Int, message: MessageItem) {
        if (position in items.indices) {
            items[position] = message
            notifyItemChanged(position)
        }
    }

    fun removeItem(position: Int) {
        if (position in items.indices) {
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun addThinkingItem() {
        val thinking = MessageItem.ThinkingMessage(
            id = "thinking_temp",
            timestamp = System.currentTimeMillis()
        )
        addItem(thinking)
        thinkingPosition = items.size - 1
    }

    fun removeThinkingItem() {
        val index = items.indexOfFirst { it is MessageItem.ThinkingMessage }
        if (index >= 0) {
            items.removeAt(index)
            notifyItemRemoved(index)
        }
        thinkingPosition = -1
    }

    fun updateThinkingStatus(status: String) {
        val index = items.indexOfFirst { it is MessageItem.ThinkingMessage }
        if (index >= 0) {
            val thinking = (items[index] as MessageItem.ThinkingMessage).copy(status = status)
            items[index] = thinking
            notifyItemChanged(index)
        }
    }
    
    fun getMessageItemAt(position: Int): MessageItem {
        return items[position]
    }
}
