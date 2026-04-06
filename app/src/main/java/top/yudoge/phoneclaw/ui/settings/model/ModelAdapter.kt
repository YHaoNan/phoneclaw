package top.yudoge.phoneclaw.ui.settings.model

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import top.yudoge.phoneclaw.databinding.ItemModelSimpleBinding

class ModelAdapter(
    private val onEditClick: (ModelAdapterItem) -> Unit,
    private val onDeleteClick: (ModelAdapterItem) -> Unit
) : RecyclerView.Adapter<ModelAdapter.ModelViewHolder>() {

    private var models: List<ModelAdapterItem> = emptyList()

    fun setData(models: List<ModelAdapterItem>) {
        this.models = models
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModelViewHolder {
        val binding = ItemModelSimpleBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ModelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ModelViewHolder, position: Int) {
        holder.bind(models[position])
    }

    override fun getItemCount(): Int = models.size

    inner class ModelViewHolder(
        private val binding: ItemModelSimpleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(model: ModelAdapterItem) {
            binding.modelName.text = model.displayName
            binding.iconVisual.visibility = if (model.hasVisualCapability) View.VISIBLE else View.GONE

            binding.btnEdit.setOnClickListener {
                onEditClick(model)
            }

            binding.btnDelete.setOnClickListener {
                onDeleteClick(model)
            }
        }
    }
}
