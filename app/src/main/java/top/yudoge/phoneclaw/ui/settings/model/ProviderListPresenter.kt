package top.yudoge.phoneclaw.ui.settings.model

import android.util.Log
import top.yudoge.phoneclaw.app.AppContainer
import top.yudoge.phoneclaw.llm.domain.objects.ModelProvider

class ProviderListPresenter(
    private val view: ProviderListContract.View
) : ProviderListContract.Presenter {

    override fun loadProviders() {
        try {
            val providers = AppContainer.getInstance().modelProviderFacade.getAllProviders()
            Log.d("ProviderList", "Loaded ${providers.size} providers")
            providers.forEach { p ->
                Log.d("ProviderList", "  Provider: id=${p.id}, name=${p.name}")
            }
            
            val providerModels = mutableMapOf<Long, List<ModelAdapterItem>>()
            
            for (provider in providers) {
                val models = AppContainer.getInstance().modelProviderFacade.getModelsByProvider(provider.id)
                providerModels[provider.id] = models.map { ModelAdapterItem.fromModel(it) }
                Log.d("ProviderList", "  Provider ${provider.id} has ${models.size} models")
            }
            
            view.showProviders(providers, providerModels)
        } catch (e: Exception) {
            view.showError("加载失败: ${e.message}")
        }
    }

    override fun toggleProviderExpanded(providerId: Long) {
    }

    override fun deleteProvider(providerId: Long) {
        try {
            AppContainer.getInstance().modelProviderFacade.deleteProvider(providerId)
            view.onProviderDeleted()
        } catch (e: Exception) {
            view.showError("删除失败: ${e.message}")
        }
    }

    override fun deleteModel(modelId: String) {
        try {
            AppContainer.getInstance().modelProviderFacade.deleteModel(modelId)
            view.onModelDeleted()
        } catch (e: Exception) {
            view.showError("删除失败: ${e.message}")
        }
    }

    override fun onDestroy() {
    }
}
