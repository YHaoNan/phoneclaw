package top.yudoge.phoneclaw.ui.settings.taskscript

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import top.yudoge.phoneclaw.databinding.ItemTaskScriptBinding
import top.yudoge.phoneclaw.llm.domain.objects.TaskScript

class TaskScriptAdapter(
    private val scripts: List<TaskScript>,
    private val onClick: (TaskScript) -> Unit,
    private val onDelete: (TaskScript) -> Unit
) : RecyclerView.Adapter<TaskScriptAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTaskScriptBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = scripts.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(scripts[position], dateFormat, onClick, onDelete)
    }

    class ViewHolder(private val binding: ItemTaskScriptBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            item: TaskScript,
            dateFormat: SimpleDateFormat,
            onClick: (TaskScript) -> Unit,
            onDelete: (TaskScript) -> Unit
        ) {
            binding.scriptNameText.text = item.name
            binding.scriptSummaryText.text = item.summary
            binding.scriptCreatedAtText.text = dateFormat.format(Date(item.createdTime))
            binding.root.setOnClickListener { onClick(item) }
            binding.deleteButton.setOnClickListener { onDelete(item) }
        }
    }
}
