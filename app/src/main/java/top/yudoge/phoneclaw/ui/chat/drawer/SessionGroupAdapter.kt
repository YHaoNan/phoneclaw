package top.yudoge.phoneclaw.ui.chat.drawer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import top.yudoge.phoneclaw.databinding.ItemSessionGroupBinding
import top.yudoge.phoneclaw.db.PhoneClawDbHelper

class SessionGroupAdapter(
    private val onSessionClick: (PhoneClawDbHelper.SessionRecord) -> Unit,
    private val onSessionLongClick: (PhoneClawDbHelper.SessionRecord) -> Unit
) : ListAdapter<SessionGroup, SessionGroupAdapter.GroupViewHolder>(SessionGroupDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = ItemSessionGroupBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class GroupViewHolder(
        private val binding: ItemSessionGroupBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(group: SessionGroup) {
            binding.groupTitleText.text = group.title
        }
    }
}

class SessionGroupDiffCallback : DiffUtil.ItemCallback<SessionGroup>() {
    override fun areItemsTheSame(oldItem: SessionGroup, newItem: SessionGroup): Boolean {
        return oldItem.title == newItem.title
    }

    override fun areContentsTheSame(oldItem: SessionGroup, newItem: SessionGroup): Boolean {
        return oldItem == newItem
    }
}
