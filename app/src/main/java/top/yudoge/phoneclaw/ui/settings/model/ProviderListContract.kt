package top.yudoge.phoneclaw.ui.settings.model

import top.yudoge.phoneclaw.db.PhoneClawDbHelper.ModelProviderRecord
import top.yudoge.phoneclaw.db.PhoneClawDbHelper.ModelRecord

interface ProviderListContract {
    interface View {
        fun showProviders(providers: List<ModelProviderRecord>, providerModels: Map<Long, List<ModelRecord>>)
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
