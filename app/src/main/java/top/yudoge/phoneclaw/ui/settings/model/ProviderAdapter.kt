package top.yudoge.phoneclaw.ui.settings.model

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import top.yudoge.phoneclaw.databinding.ItemProviderExpandBinding
import top.yudoge.phoneclaw.db.PhoneClawDbHelper.ModelProviderRecord
import top.yudoge.phoneclaw.db.PhoneClawDbHelper.ModelRecord

class ProviderAdapter(
    private val onProviderClick: (ModelProviderRecord) -> Unit,
    private val onProviderLongClick: (ModelProviderRecord) -> Unit,
    private val onAddModelClick: (ModelProviderRecord) -> Unit,
    private val onEditModelClick: (ModelRecord) -> Unit,
    private val onDeleteModelClick: (ModelRecord) -> Unit,
    private val onDeleteProviderClick: (ModelProviderRecord) -> Unit
) : RecyclerView.Adapter<ProviderAdapter.ProviderViewHolder>() {

    private var providers: List<ModelProviderRecord> = emptyList()
    private var providerModels: Map<Long, List<ModelRecord>> = emptyMap()
    private val expandedProviders = mutableSetOf<Long>()

    fun setData(providers: List<ModelProviderRecord>, providerModels: Map<Long, List<ModelRecord>>) {
        this.providers = providers
        this.providerModels = providerModels
        expandedProviders.retainAll(providers.map { it.id }.toSet())
        notifyDataSetChanged()
    }

    fun toggleExpanded(providerId: Long) {
        if (expandedProviders.contains(providerId)) {
            expandedProviders.remove(providerId)
        } else {
            expandedProviders.add(providerId)
        }
        val index = providers.indexOfFirst { it.id == providerId }
        if (index >= 0) notifyItemChanged(index)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProviderViewHolder {
        val binding = ItemProviderExpandBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ProviderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProviderViewHolder, position: Int) {
        holder.bind(providers[position])
    }

    override fun getItemCount(): Int = providers.size

    inner class ProviderViewHolder(
        private val binding: ItemProviderExpandBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var modelAdapter: ModelAdapter? = null

        fun bind(provider: ModelProviderRecord) {
            binding.providerName.text = provider.name
            binding.providerApiType.text = provider.apiType

            val models = providerModels[provider.id] ?: emptyList()
            val isExpanded = expandedProviders.contains(provider.id)

            binding.modelsContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE
            binding.expandIcon.rotation = if (isExpanded) 180f else 0f

            binding.headerLayout.setOnClickListener {
                toggleExpanded(provider.id)
            }

            binding.headerLayout.setOnLongClickListener {
                onProviderLongClick(provider)
                true
            }

            modelAdapter = ModelAdapter(
                onEditClick = onEditModelClick,
                onDeleteClick = onDeleteModelClick
            )
            binding.modelsRecyclerView.layoutManager = LinearLayoutManager(binding.root.context)
            binding.modelsRecyclerView.adapter = modelAdapter
            modelAdapter?.setData(models)

            binding.btnAddModel.setOnClickListener {
                android.util.Log.d("ProviderAdapter", "Add model clicked for provider: id=${provider.id}, name=${provider.name}")
                onAddModelClick(provider)
            }
        }
    }
}
