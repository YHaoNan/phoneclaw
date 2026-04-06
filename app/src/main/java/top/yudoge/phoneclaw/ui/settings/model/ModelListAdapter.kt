package top.yudoge.phoneclaw.ui.settings.model

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import top.yudoge.phoneclaw.databinding.ItemModelListFooterBinding
import top.yudoge.phoneclaw.databinding.ItemModelSimpleBinding
import top.yudoge.phoneclaw.llm.provider.ModelEntity

class ModelListAdapter(
    private val onEditClick: (ModelEntity) -> Unit,
    private val onDeleteClick: (ModelEntity) -> Unit,
    private val onDoneClick: () -> Unit,
    private val onAddNewClick: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_MODEL = 0
        private const val TYPE_FOOTER = 1
    }

    private var models: List<ModelEntity> = emptyList()

    fun setData(models: List<ModelEntity>) {
        this.models = models
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == models.size) TYPE_FOOTER else TYPE_MODEL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_MODEL -> {
                val binding = ItemModelSimpleBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                ModelViewHolder(binding)
            }
            else -> {
                val binding = ItemModelListFooterBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                FooterViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ModelViewHolder -> holder.bind(models[position])
            is FooterViewHolder -> holder.bind()
        }
    }

    override fun getItemCount(): Int = models.size + 1

    inner class ModelViewHolder(
        private val binding: ItemModelSimpleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(model: ModelEntity) {
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

    inner class FooterViewHolder(
        private val binding: ItemModelListFooterBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind() {
            binding.btnDone.setOnClickListener {
                onDoneClick()
            }
            
            binding.btnAddNew.setOnClickListener {
                onAddNewClick()
            }
        }
    }
}
