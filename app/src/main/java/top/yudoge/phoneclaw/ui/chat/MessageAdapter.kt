package top.yudoge.phoneclaw.ui.chat

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import top.yudoge.phoneclaw.ui.chat.model.MessageItem
import top.yudoge.phoneclaw.ui.chat.viewholders.AgentMessageViewHolder
import top.yudoge.phoneclaw.ui.chat.viewholders.BaseMessageViewHolder
import top.yudoge.phoneclaw.ui.chat.viewholders.SkillCallViewHolder
import top.yudoge.phoneclaw.ui.chat.viewholders.ThinkingViewHolder
import top.yudoge.phoneclaw.ui.chat.viewholders.ToolCallViewHolder
import top.yudoge.phoneclaw.ui.chat.viewholders.UserMessageViewHolder

class MessageAdapter : ListAdapter<MessageItem, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    companion object {
        private const val TYPE_USER = 0
        private const val TYPE_AGENT = 1
        private const val TYPE_TOOL = 2
        private const val TYPE_SKILL = 3
        private const val TYPE_THINKING = 4
    }

    private var thinkingPosition: Int = -1

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
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
            is UserMessageViewHolder -> holder.bind(getItem(position) as MessageItem.UserMessage)
            is AgentMessageViewHolder -> holder.bind(getItem(position) as MessageItem.AgentMessage)
            is ToolCallViewHolder -> holder.bind(getItem(position) as MessageItem.ToolCallMessage)
            is SkillCallViewHolder -> holder.bind(getItem(position) as MessageItem.SkillCallMessage)
            is ThinkingViewHolder -> holder.bind(getItem(position) as MessageItem.ThinkingMessage)
        }
    }

    fun addItem(message: MessageItem) {
        val currentList = currentList.toMutableList()
        currentList.add(message)
        submitList(currentList)
    }

    fun updateItem(position: Int, message: MessageItem) {
        val currentList = currentList.toMutableList()
        if (position in currentList.indices) {
            currentList[position] = message
            submitList(currentList)
        }
    }

    fun removeItem(position: Int) {
        val currentList = currentList.toMutableList()
        if (position in currentList.indices) {
            currentList.removeAt(position)
            submitList(currentList)
        }
    }

    fun addThinkingItem() {
        val thinking = MessageItem.ThinkingMessage(
            id = "thinking_temp",
            timestamp = System.currentTimeMillis()
        )
        addItem(thinking)
        thinkingPosition = currentList.size - 1
    }

    fun removeThinkingItem() {
        if (thinkingPosition >= 0 && thinkingPosition < currentList.size) {
            val currentList = currentList.toMutableList()
            currentList.removeAt(thinkingPosition)
            submitList(currentList)
            thinkingPosition = -1
        }
    }

    fun updateThinkingStatus(status: String) {
        if (thinkingPosition >= 0 && thinkingPosition < currentList.size) {
            val thinking = (getItem(thinkingPosition) as? MessageItem.ThinkingMessage)
                ?.copy(status = status)
            thinking?.let {
                val currentList = currentList.toMutableList()
                currentList[thinkingPosition] = it
                submitList(currentList)
            }
        }
    }
    
    fun getMessageItemAt(position: Int): MessageItem {
        return getItem(position)
    }
}

class MessageDiffCallback : DiffUtil.ItemCallback<MessageItem>() {
    override fun areItemsTheSame(oldItem: MessageItem, newItem: MessageItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: MessageItem, newItem: MessageItem): Boolean {
        return oldItem == newItem
    }
}
