package top.yudoge.phoneclaw.ui.settings.model

import top.yudoge.phoneclaw.db.PhoneClawDbHelper
import top.yudoge.phoneclaw.db.PhoneClawDbHelper.ModelProviderRecord
import top.yudoge.phoneclaw.db.PhoneClawDbHelper.ModelRecord

class ProviderListPresenter(
    private val view: ProviderListContract.View,
    private val dbHelper: PhoneClawDbHelper
) : ProviderListContract.Presenter {

    override fun loadProviders() {
        try {
            val providers = dbHelper.allModelProviders
            android.util.Log.d("ProviderList", "Loaded ${providers.size} providers")
            providers.forEach { p ->
                android.util.Log.d("ProviderList", "  Provider: id=${p.id}, name=${p.name}")
            }
            val providerModels = mutableMapOf<Long, List<ModelRecord>>()
            
            for (provider in providers) {
                val models = dbHelper.getModelsByProvider(provider.id)
                providerModels[provider.id] = models
                android.util.Log.d("ProviderList", "  Provider ${provider.id} has ${models.size} models")
            }
            
            view.showProviders(providers, providerModels)
        } catch (e: Exception) {
            view.showError("加载失败: ${e.message}")
        }
    }

    override fun toggleProviderExpanded(providerId: Long) {
        // Handled by the adapter
    }

    override fun deleteProvider(providerId: Long) {
        try {
            dbHelper.deleteModelProvider(providerId)
            view.onProviderDeleted()
        } catch (e: Exception) {
            view.showError("删除失败: ${e.message}")
        }
    }

    override fun deleteModel(modelId: String) {
        try {
            dbHelper.deleteModel(modelId)
            view.onModelDeleted()
        } catch (e: Exception) {
            view.showError("删除失败: ${e.message}")
        }
    }

    override fun onDestroy() {
        // Nothing to clean up
    }
}
