package top.yudoge.phoneclaw.ui.settings.model

import android.content.Context
import android.util.Log
import top.yudoge.phoneclaw.llm.provider.ModelProviderEntity
import top.yudoge.phoneclaw.llm.provider.ModelProviderRepositoryImpl
import top.yudoge.phoneclaw.llm.provider.ModelRepositoryImpl

class ProviderListPresenter(
    private val view: ProviderListContract.View,
    context: Context
) : ProviderListContract.Presenter {

    private val providerRepository = ModelProviderRepositoryImpl(context)
    private val modelRepository = ModelRepositoryImpl(context)

    override fun loadProviders() {
        try {
            val providers = providerRepository.listProvider()
            Log.d("ProviderList", "Loaded ${providers.size} providers")
            providers.forEach { p ->
                Log.d("ProviderList", "  Provider: id=${p.id}, name=${p.name}")
            }
            
            val providerModels = mutableMapOf<Long, List<ModelAdapterItem>>()
            
            for (provider in providers) {
                val models = modelRepository.getModelsByProvider(provider.id)
                providerModels[provider.id] = models.map { ModelAdapterItem.fromEntity(it) }
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
            providerRepository.deleteProvider(providerId)
            view.onProviderDeleted()
        } catch (e: Exception) {
            view.showError("删除失败: ${e.message}")
        }
    }

    override fun deleteModel(modelId: String) {
        try {
            modelRepository.deleteModel(modelId)
            view.onModelDeleted()
        } catch (e: Exception) {
            view.showError("删除失败: ${e.message}")
        }
    }

    override fun onDestroy() {
    }
}
