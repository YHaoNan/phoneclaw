package top.yudoge.phoneclaw.ui.chat.drawer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import top.yudoge.phoneclaw.databinding.ItemSessionBinding
import top.yudoge.phoneclaw.llm.domain.objects.Session
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SessionAdapter(
    private val onSessionClick: (Session) -> Unit,
    private val onSessionLongClick: (Session) -> Unit
) : ListAdapter<Session, SessionAdapter.ViewHolder>(SessionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSessionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemSessionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onSessionClick(getItem(position))
                }
            }
            binding.root.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onSessionLongClick(getItem(position))
                }
                true
            }
        }

        fun bind(session: Session) {
            binding.titleText.text = session.title.ifEmpty { "新对话" }
            binding.timeText.text = formatTime(session.updatedAt)
        }

        private fun formatTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < 60000 -> "刚刚"
                diff < 3600000 -> "${diff / 60000}分钟前"
                diff < 86400000 -> "${diff / 3600000}小时前"
                diff < 604800000 -> "${diff / 86400000}天前"
                else -> {
                    val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
            }
        }
    }
}

class SessionDiffCallback : DiffUtil.ItemCallback<Session>() {
    override fun areItemsTheSame(
        oldItem: Session,
        newItem: Session
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: Session,
        newItem: Session
    ): Boolean {
        return oldItem == newItem
    }
}
