package top.yudoge.phoneclaw.ui.settings.model

import top.yudoge.phoneclaw.llm.domain.objects.ModelProvider

interface ProviderListContract {
    interface View {
        fun showProviders(providers: List<ModelProvider>, providerModels: Map<Long, List<ModelAdapterItem>>)
        fun showError(message: String)
        fun onProviderDeleted()
        fun onModelDeleted()
    }

    interface Presenter {
        fun loadProviders()
        fun toggleProviderExpanded(providerId: Long)
        fun deleteProvider(providerId: Long)
        fun deleteModel(modelId: String)
        fun onDestroy()
    }
}
